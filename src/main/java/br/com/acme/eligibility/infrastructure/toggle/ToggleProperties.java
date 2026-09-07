package br.com.acme.eligibility.infrastructure.toggle;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao do cliente do servico de toggles. */
@Getter
@Setter
@ConfigurationProperties(prefix = "eligibility.toggle")
public class ToggleProperties {

    private String baseUrl = "http://localhost:8081";
    private String product = "eligibility-product";
    private long timeoutSeconds = 3L;
}
