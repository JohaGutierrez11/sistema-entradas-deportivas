package ec.edu.uce.tickets.reservation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class MatchServiceClientConfig {

    @Bean
    public RestClient matchServiceRestClient(@Value("${app.match-service.url}") String baseUrl,
                                             @Value("${app.match-service.internal-key}") String internalKey) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));   // máximo para conectarse
        requestFactory.setReadTimeout(Duration.ofSeconds(3));      // máximo para recibir la respuesta

        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Key", internalKey)
                .requestFactory(requestFactory)
                .build();
    }
}