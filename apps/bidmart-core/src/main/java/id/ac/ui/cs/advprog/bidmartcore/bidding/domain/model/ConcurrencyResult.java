package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model;

public record ConcurrencyResult(
    BidAcceptance status,
        long visiblePrice,
        String winnerId,
        long endTime,
        long bidderCommittedMax,
        Long proxyVisiblePrice,
        String proxyWinnerId
) {
    public enum BidAcceptance {
        LEADING, // accepted and user now leads
        OUTBID, // accepted but instantly auto-lost
        REJECTED,
        CACHE_MISS,
        NOT_ACTIVE,
        ENDED
    }

    public static ConcurrencyResult leading(long visiblePrice, String newWinner, long newEndTime,
            long bidderCommittedMax) {
        return new ConcurrencyResult(
                BidAcceptance.LEADING,
                visiblePrice,
                newWinner,
                newEndTime,
                bidderCommittedMax,
                null,
                null);
    }

    public static ConcurrencyResult outbid(long visiblePrice, String winnerId, long endTime,
            long bidderCommittedMax, long proxyVisiblePrice, String proxyWinnerId) {
        return new ConcurrencyResult(BidAcceptance.OUTBID, visiblePrice, winnerId, endTime,
                bidderCommittedMax, proxyVisiblePrice, proxyWinnerId);
    }

    public static ConcurrencyResult rejected() {
        return new ConcurrencyResult(BidAcceptance.REJECTED, 0, null, 0, 0, null, null);
    }

    public static ConcurrencyResult cacheMiss() {
        return new ConcurrencyResult(BidAcceptance.CACHE_MISS, 0, null, 0, 0, null, null);
    }

    public static ConcurrencyResult notActive() {
        return new ConcurrencyResult(BidAcceptance.NOT_ACTIVE, 0, null, 0, 0, null, null);
    }

    public static ConcurrencyResult ended() {
        return new ConcurrencyResult(BidAcceptance.ENDED, 0, null, 0, 0, null, null);
    }
}
