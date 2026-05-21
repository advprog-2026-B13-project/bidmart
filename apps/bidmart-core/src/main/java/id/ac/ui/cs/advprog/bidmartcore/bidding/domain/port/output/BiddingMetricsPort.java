package id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output;

public interface BiddingMetricsPort {

    record Sample(long startNanos) {}

    default Sample start() {
        return new Sample(System.nanoTime());
    }

    void record(Sample sample, String timerName);
    void record(Sample sample, String timerName, String tagKey, String tagValue);
}
