package id.ac.ui.cs.advprog.bidmartcore.bidding.infrastructure.adapter.output.metrics;

import java.time.Duration;

import org.springframework.stereotype.Component;

import id.ac.ui.cs.advprog.bidmartcore.bidding.domain.port.output.BiddingMetricsPort;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MicrometerBiddingMetrics implements BiddingMetricsPort {

    private final MeterRegistry registry;

    @Override
    public void record(Sample sample, String timerName) {
        registry.timer(timerName)
                .record(Duration.ofNanos(System.nanoTime() - sample.startNanos()));
    }

    @Override
    public void record(Sample sample, String timerName, String tagKey, String tagValue) {
        registry.timer(timerName, tagKey, tagValue)
                .record(Duration.ofNanos(System.nanoTime() - sample.startNanos()));
    }
}
