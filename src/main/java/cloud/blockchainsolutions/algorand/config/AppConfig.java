package cloud.blockchainsolutions.algorand.config;

import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.hubspot.jinjava.Jinjava;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Value("${algorand.host}")
    private String host;

    @Value("${algorand.port}")
    private int port;

    @Value("${algorand.token}")
    private String token;

    @Bean
    public AlgodClient algodClient() {
        return new AlgodClient(host, port, token);
    }

    @Bean
    public Jinjava jinJava() {
        return new Jinjava();
    }
}

