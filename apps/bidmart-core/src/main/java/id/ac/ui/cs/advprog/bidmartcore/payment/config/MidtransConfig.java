package id.ac.ui.cs.advprog.bidmartcore.payment.config;

import com.midtrans.Config;
import com.midtrans.service.MidtransCoreApi;
import com.midtrans.service.impl.MidtransCoreApiImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MidtransConfig {

    @Value("${midtrans.server-key}")
    private String serverKey;

    @Value("${midtrans.client-key}")
    private String clientKey;

    @Value("${midtrans.is-production:false}")
    private boolean isProduction;

    @Bean
    public MidtransCoreApi midtransCoreApi() {
        Config config = new Config(serverKey, clientKey, isProduction);
        return new MidtransCoreApiImpl(config);
    }
}
