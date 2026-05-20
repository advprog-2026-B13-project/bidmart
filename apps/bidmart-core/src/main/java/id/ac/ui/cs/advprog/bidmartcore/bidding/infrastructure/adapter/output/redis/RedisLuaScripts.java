package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.redis;

public final class RedisLuaScripts {

    private RedisLuaScripts() {}

    // KEYS[1] = auction:{listingId}:state
    // ARGV[1] = bidAmount (long)
    // ARGV[2] = minIncrement (long)
    // ARGV[3] = bidderId (string)
    // ARGV[4] = bidType (MANUAL|PROXY)
    // ARGV[5] = currentTimeMillis (long)
    // ARGV[6] = antiSnipeThresholdMillis (long)
    // ARGV[7] = antiSnipeExtensionMillis (long)
    //
    // Returns:
    // {status, visiblePrice, winnerId, endTime, bidderCommittedMax,
    // proxyVisiblePrice, proxyWinnerId}
    // status: 1=LEADING, 0=REJECTED, -1=CACHE_MISS, -2=NOT_ACTIVE, -3=ENDED,
    // -4=OUTBID
    public static final String PLACE_BID_LUA = """
                local state = redis.call('HMGET', KEYS[1], 'price', 'endTime', 'status', 'winner', 'maxAmount')
        local currentPrice = tonumber(state[1])
        local endTime = tonumber(state[2])
        local status = state[3]
        local oldWinner = state[4] or ''
                local oldMaxAmount = tonumber(state[5])

        if not currentPrice then
                    return {-1, 0, '', 0, 0, 0, ''}
        end
        if status ~= 'ACTIVE' then
                    return {-2, 0, '', 0, 0, 0, ''}
        end
                if tonumber(ARGV[5]) > endTime then
                    return {-3, 0, '', 0, 0, 0, ''}
        end

        local bidAmount = tonumber(ARGV[1])
                local minIncrement = tonumber(ARGV[2])
                local bidderId = ARGV[3]
                local bidType = ARGV[4]
                local nowMillis = tonumber(ARGV[5])
                local antiSnipeThresholdMillis = tonumber(ARGV[6])
                local antiSnipeExtensionMillis = tonumber(ARGV[7])

                if not oldMaxAmount then
                    oldMaxAmount = currentPrice
                end

                local minRequired = currentPrice + minIncrement
                if oldWinner == '' then
                    minRequired = currentPrice
                end

                if bidAmount < minRequired then
                    return {0, 0, '', endTime, 0, 0, ''}
                end

                local newEndTime = endTime
                local timeLeft = endTime - nowMillis
                if timeLeft < antiSnipeThresholdMillis then
                    newEndTime = nowMillis + antiSnipeExtensionMillis
                end

                -- First valid bid.
                if oldWinner == '' then
                    local openingVisiblePrice = bidAmount
                    if bidType == 'PROXY' then
                        openingVisiblePrice = currentPrice
                    end

                    redis.call('HMSET', KEYS[1],
                        'price', openingVisiblePrice,
                        'winner', bidderId,
                        'maxAmount', bidAmount,
                        'endTime', newEndTime)

                    return {1, openingVisiblePrice, bidderId, newEndTime, bidAmount, 0, ''}
                end

                -- Current winner increases own bid.
                if bidderId == oldWinner then
                    if bidType == 'PROXY' then
                        if bidAmount <= oldMaxAmount then
                            return {0, 0, '', endTime, 0, 0, ''}
                        end

                        redis.call('HMSET', KEYS[1],
                            'maxAmount', bidAmount,
                            'endTime', newEndTime)
                        return {1, currentPrice, bidderId, newEndTime, bidAmount, 0, ''}
                    end

                    -- MANUAL by current winner: raise visible price, preserve proxy max if higher.
                    local keepMax = bidAmount
                    if oldMaxAmount and oldMaxAmount > keepMax then
                        keepMax = oldMaxAmount
                    end
                    redis.call('HMSET', KEYS[1],
                        'price', bidAmount,
                        'winner', bidderId,
                        'maxAmount', keepMax,
                        'endTime', newEndTime)
                    return {1, bidAmount, bidderId, newEndTime, keepMax, 0, ''}
                end

                -- Challenger takes lead.
                if bidAmount > oldMaxAmount then
                    local newVisiblePrice = bidAmount
                    if bidType == 'PROXY' then
                        local candidate = oldMaxAmount + minIncrement
                        if candidate > bidAmount then
                            newVisiblePrice = bidAmount
                        else
                            newVisiblePrice = candidate
                        end
                    end

                    redis.call('HMSET', KEYS[1],
                        'price', newVisiblePrice,
                        'winner', bidderId,
                        'maxAmount', bidAmount,
                        'endTime', newEndTime)

                    return {1, newVisiblePrice, bidderId, newEndTime, bidAmount, 0, ''}
                end

                -- Challenger is instantly outbid by current winner.
                local proxyVisiblePrice = oldMaxAmount
                local counterCandidate = bidAmount + minIncrement
                if counterCandidate < oldMaxAmount then
                    proxyVisiblePrice = counterCandidate
        end

                redis.call('HMSET', KEYS[1],
                    'price', proxyVisiblePrice,
                    'winner', oldWinner,
                    'maxAmount', oldMaxAmount,
                    'endTime', newEndTime)

                return {-4, bidAmount, oldWinner, newEndTime, bidAmount, proxyVisiblePrice, oldWinner}
            """;

    // KEYS[1] = auction:{listingId}:state
    // ARGV[1] = priceToRestore (long)
    // ARGV[2] = winnerToRestore (string)
    // ARGV[3] = endTimeToRestore (long)
    // ARGV[4] = expectedCurrentPrice (long)
    // ARGV[5] = expectedCurrentWinner (string)
    public static final String ROLLBACK_LUA = """
        local state = redis.call('HMGET', KEYS[1], 'price', 'winner')
        local currentPrice = tonumber(state[1])
        local currentWinner = state[2] or ''

        if not currentPrice then
            return 0
        end

        if currentPrice ~= tonumber(ARGV[4]) or currentWinner ~= ARGV[5] then
            return 0
        end

            local priceToRestore = tonumber(ARGV[1])
            redis.call('HMSET', KEYS[1],
                'price', ARGV[1],
                'winner', ARGV[2],
                'maxAmount', ARGV[1],
                'endTime', ARGV[3])
        return 1
        """;
}
