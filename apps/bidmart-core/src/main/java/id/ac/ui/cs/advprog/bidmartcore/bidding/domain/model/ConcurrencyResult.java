package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.model;

public record ConcurrencyResult(
    BidAcceptance status,
    long oldPrice,
    String oldWinner,
    long oldEndTime
) {
    public enum BidAcceptance {
        ACCEPTED,
        REJECTED,
        CACHE_MISS,
        NOT_ACTIVE,
        ENDED
    }

    public static ConcurrencyResult accepted(long price, String winner, long endTime) {
        return new ConcurrencyResult(BidAcceptance.ACCEPTED, price, winner, endTime);
    }

    public static ConcurrencyResult rejected(long price, String winner, long endTime) {
        return new ConcurrencyResult(BidAcceptance.REJECTED, price, winner, endTime);
    }

    public static ConcurrencyResult cacheMiss() {
        return new ConcurrencyResult(BidAcceptance.CACHE_MISS, 0, null, 0);
    }

    public static ConcurrencyResult notActive() {
        return new ConcurrencyResult(BidAcceptance.NOT_ACTIVE, 0, null, 0);
    }

    public static ConcurrencyResult ended() {
        return new ConcurrencyResult(BidAcceptance.ENDED, 0, null, 0);
    }
}
