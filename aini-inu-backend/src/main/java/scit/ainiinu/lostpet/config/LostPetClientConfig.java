package scit.ainiinu.lostpet.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LostPetClientConfig {

    @Bean(name = "chatRestTemplate")
    public RestTemplate chatRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
