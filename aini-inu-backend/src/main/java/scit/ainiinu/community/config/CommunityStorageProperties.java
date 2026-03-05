package scit.ainiinu.community.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "community.storage")
public class CommunityStorageProperties {

    private String publicBaseUrl = "http://localhost:8080";
    private final Local local = new Local();
    private final Presigned presigned = new Presigned();

    @Getter
    @Setter
    public static class Local {
        private String baseDir = "./var/uploads";
    }

    @Getter
    @Setter
    public static class Presigned {
        private long expiresSeconds = 300L;
    }
}
