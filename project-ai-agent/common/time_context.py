"""
时间上下文工具

提供格式化的当前时间信息，供 _memory_recall_node 注入系统上下文，
LLM 无需调用工具即可获知时间。
"""

from datetime import datetime


def get_formatted_time() -> str:
    """
    获取格式化的当前时间信息

    Returns:
        包含日期时间、星期、工作日/周末、时间段描述的字符串
    """
    now = datetime.now()

    date_str = now.strftime("%Y-%m-%d %H:%M:%S")
    weekday_names = ["星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"]
    weekday = weekday_names[now.weekday()]
    is_weekday = "工作日" if now.weekday() < 5 else "周末"

    hour = now.hour
    if 0 <= hour < 6:
        period = "凌晨"
    elif 6 <= hour < 9:
        period = "早上"
    elif 9 <= hour < 11:
        period = "上午"
    elif 11 <= hour < 13:
        period = "中午"
    elif 13 <= hour < 17:
        period = "下午"
    elif 17 <= hour < 19:
        period = "傍晚"
    elif 19 <= hour < 22:
        period = "晚上"
    else:
        period = "深夜"

    return f"当前时间：{date_str}\n{weekday}（{is_weekday}）\n时间段：{period}"
