package scit.ainiinu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class AiniInuApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiniInuApplication.class, args);
    }

}
