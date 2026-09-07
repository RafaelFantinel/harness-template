package br.com.acme.eligibility.infrastructure.config;

import br.com.acme.eligibility.infrastructure.dynamo.DynamoProperties;
import br.com.acme.eligibility.infrastructure.toggle.ToggleProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Habilita as propriedades de configuracao da camada de infraestrutura. */
@Configuration
@EnableConfigurationProperties({ToggleProperties.class, DynamoProperties.class})
public class InfrastructurePropertiesConfiguration {
}
