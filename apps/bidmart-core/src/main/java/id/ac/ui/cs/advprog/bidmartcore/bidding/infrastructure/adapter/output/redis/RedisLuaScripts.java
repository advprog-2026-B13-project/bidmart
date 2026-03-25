package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

public final class RedisLuaScripts {

    private RedisLuaScripts() {}

    // KEYS[1] = auction:{listingId}:state
    // ARGV[1] = bidAmount (long)
    // ARGV[2] = minIncrement (long)
    // ARGV[3] = bidderId (string)
    // ARGV[4] = currentTimeMillis (long)
    // ARGV[5] = antiSnipeThresholdMillis (long)
    // ARGV[6] = antiSnipeExtensionMillis (long)
    //
    // Returns: {status, oldPrice, oldWinner, oldEndTime}
    //   status: 1=ACCEPTED, 0=REJECTED, -1=CACHE_MISS, -2=NOT_ACTIVE, -3=ENDED
    public static final String PLACE_BID_LUA = """
        local state = redis.call('HMGET', KEYS[1], 'price', 'endTime', 'status', 'winner')
        local currentPrice = tonumber(state[1])
        local endTime = tonumber(state[2])
        local status = state[3]
        local oldWinner = state[4] or ''

        if not currentPrice then
            return {-1, 0, '', 0}
        end
        if status ~= 'ACTIVE' then
            return {-2, 0, '', 0}
        end
        if tonumber(ARGV[4]) > endTime then
            return {-3, 0, '', 0}
        end

        local bidAmount = tonumber(ARGV[1])
        if bidAmount >= (currentPrice + tonumber(ARGV[2])) then
            local newEndTime = endTime
            local timeLeft = endTime - tonumber(ARGV[4])
            local threshold = tonumber(ARGV[5])
            if timeLeft < threshold then
                newEndTime = tonumber(ARGV[4]) + tonumber(ARGV[6])
            end
            redis.call('HMSET', KEYS[1], 'price', bidAmount, 'winner', ARGV[3], 'endTime', newEndTime)
            return {1, currentPrice, oldWinner, endTime}
        else
            return {0, currentPrice, oldWinner, endTime}
        end
        """;

    // KEYS[1] = auction:{listingId}:state
    // ARGV[1] = priceToRestore (long)
    // ARGV[2] = winnerToRestore (string)
    // ARGV[3] = endTimeToRestore (long)
    public static final String ROLLBACK_LUA = """
        redis.call('HMSET', KEYS[1], 'price', ARGV[1], 'winner', ARGV[2], 'endTime', ARGV[3])
        return 1
        """;
}
