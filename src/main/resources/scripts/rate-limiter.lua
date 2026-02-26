local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local currentTime = tonumber(ARGV[3])
local windowMillis = tonumber(ARGV[4])

-- Get the current state or initialize it
local tokenInfo = redis.call('HMGET', key, 'tokens', 'lastRefill')
local tokens = tonumber(tokenInfo[1])
local lastRefill = tonumber(tokenInfo[2])

if tokens == nil then
    -- First request, initialize bucket
    tokens = capacity
    lastRefill = currentTime
else
    -- Calculate token refill based on time elapsed
    local elapsedTime = currentTime - lastRefill
    local tokensToAdd = math.floor(elapsedTime * (refillRate / windowMillis))

    if tokensToAdd > 0 then
        tokens = math.min(capacity, tokens + tokensToAdd)
        lastRefill = currentTime
    end
end

-- Process the current request
local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

-- Store updated state
redis.call('HMSET', key, 'tokens', tokens, 'lastRefill', lastRefill)
redis.call('EXPIRE', key, math.ceil(windowMillis/1000) * 2)

return tokens
