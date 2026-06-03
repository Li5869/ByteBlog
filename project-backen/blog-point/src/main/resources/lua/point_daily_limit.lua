-- 每日积分上限检查 Lua 脚本
-- KEYS[1]: 每日积分 key
-- ARGV[1]: 本次积分值
-- ARGV[2]: 每日上限
-- ARGV[3]: 过期时间（秒）
-- 返回值: 实际可发放积分数

local key = KEYS[1]
local points = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
local expireSeconds = tonumber(ARGV[3])

-- 获取当前值
local current = tonumber(redis.call('GET', key) or '0')

-- 已达上限，直接返回 0
if current >= limit then
    return 0
end

-- 计算实际可发放积分数
local effective = points
if current + points > limit then
    effective = limit - current
end

-- 原子递增
local newVal = redis.call('INCRBY', key, effective)

-- 首次写入，设置过期时间
if newVal == effective then
    redis.call('EXPIRE', key, expireSeconds)
end

return effective
