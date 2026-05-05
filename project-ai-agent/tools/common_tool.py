from datetime import datetime

from langchain_core.tools import tool


@tool
async def get_the_time() -> str:
    """
    获取当前日期和时间信息。
    
    当需要知道当前时间、日期、星期几，或需要根据时间进行决策时调用此工具。
    
    使用场景：
    1. 用户询问"现在几点了"、"今天星期几"、"今天是几号"等时间相关问题
    2. 需要根据时间段选择合适的问候语（早上好/下午好/晚上好）
    3. 需要判断是否是工作时间（工作日/周末、上班时间/下班时间）
    4. 需要计算时间差（如"距离周末还有几天"、"文章发布多久了"）
    5. 需要根据日期做推荐（如"周末适合看什么文章"、"今天是什么节日"）
    6. 需要记录操作时间戳或生成时间相关的标识
    
    返回信息包括：
    - 当前日期时间（YYYY-MM-DD HH:MM:SS）
    - 星期几
    - 是否是工作日
    - 时间段描述（凌晨/早上/上午/中午/下午/傍晚/晚上/深夜）
    
    Returns:
        包含完整时间信息的字符串
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