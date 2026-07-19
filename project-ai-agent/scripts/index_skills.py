"""
Skills 向量化入库脚本

使用方式：
    python scripts/index_skills.py                    # 全量入库（先清空再写入）
    python scripts/index_skills.py --skill writing-assistant  # 仅索引指定 Skill
    python scripts/index_skills.py --force                   # 强制重建（清空后重写）
    python scripts/index_skills.py --dry-run                 # 仅预览切片统计，不写入 DB
    python scripts/index_skills.py --preview                 # 预览切片详情
"""

import argparse
import asyncio
import sys
from pathlib import Path

from loguru import logger

from services.skill.chunker import get_skill_chunker
from vectorstore.skill_vector_store import get_skill_vector_store


async def index_all_skills(force: bool = False, dry_run: bool = False) -> dict:
    """
    索引所有 Skill 文档到向量库

    Args:
        force: 是否先清空旧数据再写入
        dry_run: 只切片不写入

    Returns:
        {"skill_count": N, "chunk_count": N, "skills": [...], "dry_run": bool}
    """
    chunker = get_skill_chunker()
    store = await get_skill_vector_store()

    # 1. 语义切片
    chunks = chunker.chunk_all_skills()
    if not chunks:
        logger.warning("[IndexSkills] 未找到任何 SKILL.md 文件")
        return {"skill_count": 0, "chunk_count": 0, "skills": [], "dry_run": dry_run}

    skill_names = sorted(set(c.skill_name for c in chunks))
    logger.info(f"[IndexSkills] 切片完成: {len(skill_names)} 个 Skill, {len(chunks)} 个切片")

    if dry_run:
        logger.info("[IndexSkills] 预览模式，不写入数据库")
        return {"skill_count": len(skill_names), "chunk_count": len(chunks), "skills": skill_names, "dry_run": True}

    # 2. 清空旧数据 + 全量入库
    if force:
        await store.clear_all()
        logger.info("[IndexSkills] 已清空旧数据")

    ids = await store.add_skill_chunks(chunks)

    logger.info(f"[IndexSkills] 入库完成: {len(skill_names)} 个 Skill, {len(ids)} 个切片")
    return {"skill_count": len(skill_names), "chunk_count": len(ids), "skills": skill_names, "dry_run": False}


async def index_single_skill(skill_name: str, dry_run: bool = False) -> dict:
    """
    索引单个指定 Skill

    Args:
        skill_name: Skill 目录名（如 writing-assistant）
        dry_run: 只切片不写入

    Returns:
        {"skill_count": 1, "chunk_count": N, "skills": [skill_name], "dry_run": bool}
    """
    chunker = get_skill_chunker()
    store = await get_skill_vector_store()

    skill_dir = chunker.skills_dir / skill_name
    skill_file = skill_dir / "SKILL.md"

    if not skill_file.exists():
        logger.error(f"[IndexSkills] Skill 文件不存在: {skill_file}")
        return {"skill_count": 0, "chunk_count": 0, "skills": [], "dry_run": dry_run, "error": f"Skill 文件不存在: {skill_name}"}

    content = skill_file.read_text(encoding="utf-8")
    chunks = chunker.chunk_skill(content, skill_name, str(skill_file))

    if not chunks:
        logger.warning(f"[IndexSkills] {skill_name} 切片为空")
        return {"skill_count": 0, "chunk_count": 0, "skills": [], "dry_run": dry_run}

    logger.info(f"[IndexSkills] {skill_name}: {len(chunks)} 个切片")

    if dry_run:
        logger.info("[IndexSkills] 预览模式，不写入数据库")
        return {"skill_count": 1, "chunk_count": len(chunks), "skills": [skill_name], "dry_run": True}

    # 按 skill_name 精确删除旧数据，再写入新切片
    await store.delete_by_skill_name(skill_name)
    ids = await store.add_skill_chunks(chunks)

    logger.info(f"[IndexSkills] {skill_name} 入库完成: {len(ids)} 个切片")
    return {"skill_count": 1, "chunk_count": len(ids), "skills": [skill_name], "dry_run": False}


def _preview_chunks(chunks: list) -> None:
    """打印切片详情预览"""
    for c in chunks:
        preview = c.content[:80].replace("\n", " ")
        suffix = "..." if len(c.content) > 80 else ""
        print(f"  {c.section_title:40s} | {len(c.content):4d}字 | {preview}{suffix}")


async def _run_preview() -> None:
    """预览模式：打印所有 Skill 切片详情"""
    chunker = get_skill_chunker()
    chunks = chunker.chunk_all_skills()
    if not chunks:
        print("未找到任何 SKILL.md 文件")
        return

    # 按 skill_name 分组展示
    skill_group: dict[str, list] = {}
    for c in chunks:
        skill_group.setdefault(c.skill_name, []).append(c)

    for skill_name in sorted(skill_group):
        group_chunks = skill_group[skill_name]
        print(f"\n{'='*60}")
        print(f"📁 {skill_name}  ({len(group_chunks)} 个切片)")
        print(f"{'='*60}")
        _preview_chunks(group_chunks)

    total = len(chunks)
    skill_count = len(skill_group)
    print(f"\n{'='*60}")
    print(f"总计: {skill_count} 个 Skill, {total} 个切片")
    print(f"{'='*60}")


def main():
    """CLI 入口"""
    parser = argparse.ArgumentParser(description="Skills 向量化入库工具")
    parser.add_argument("--skill", type=str, default=None, help="仅索引指定 Skill（目录名）")
    parser.add_argument("--force", action="store_true", help="清空旧数据后重建")
    parser.add_argument("--dry-run", action="store_true", help="仅预览切片统计，不写入")
    parser.add_argument("--preview", action="store_true", help="预览切片详情")
    args = parser.parse_args()

    if args.preview:
        asyncio.run(_run_preview())
        return

    if args.skill:
        result = asyncio.run(index_single_skill(args.skill, dry_run=args.dry_run))
    else:
        result = asyncio.run(index_all_skills(force=args.force, dry_run=args.dry_run))

    if result.get("error"):
        print(f"\n❌ {result['error']}")
        sys.exit(1)

    print(f"\n✅ {'预览' if result['dry_run'] else '入库'}完成: {result['skill_count']} 个 Skill, {result['chunk_count']} 个切片")
    if result["skills"]:
        print(f"   Skills: {', '.join(result['skills'])}")


if __name__ == "__main__":
    main()
