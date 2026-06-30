-- 优惠券领取 Lua 脚本（原子性）
-- 一次完成：检查库存 + 检查是否已领 + 扣减库存 + 标记已领
--
-- KEYS[1]: coupon:stock:{templateId}  - 库存计数器（String，数字）
-- KEYS[2]: coupon:users:{templateId}  - 已领取用户集合（Set）
-- ARGV[1]: userId                     - 当前用户ID
--
-- 返回值:
--   0  - 领取成功
--   1  - 已领取过（重复领取）
--   2  - 库存不足（已被领完）

local stockKey = KEYS[1]
local usersKey = KEYS[2]
local userId = ARGV[1]

-- 1. 检查用户是否已领取过
local alreadyClaimed = redis.call('SISMEMBER', usersKey, userId)
if alreadyClaimed == 1 then
    return 1
end

-- 2. 检查库存是否充足
local stock = tonumber(redis.call('GET', stockKey) or '0')
if stock <= 0 then
    return 2
end

-- 3. 扣减库存
redis.call('DECR', stockKey)

-- 4. 标记用户已领取
redis.call('SADD', usersKey, userId)

return 0
