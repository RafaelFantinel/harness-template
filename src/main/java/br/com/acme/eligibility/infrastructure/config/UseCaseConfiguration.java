package br.com.acme.eligibility.infrastructure.config;

import br.com.acme.eligibility.application.port.out.CnpjPermissionPort;
import br.com.acme.eligibility.application.port.out.TogglePort;
import br.com.acme.eligibility.application.usecase.CheckEligibilityUseCase;
import br.com.acme.eligibility.domain.policy.EligibilityPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Expoe as classes de dominio e aplicacao como beans, mantendo-as livres de framework. */
@Configuration
public class UseCaseConfiguration {

    @Bean
    public EligibilityPolicy eligibilityPolicy() {
        return new EligibilityPolicy();
    }

    @Bean
    public CheckEligibilityUseCase checkEligibilityUseCase(TogglePort togglePort,
                                                           CnpjPermissionPort cnpjPermissionPort,
                                                           EligibilityPolicy eligibilityPolicy) {
        return new CheckEligibilityUseCase(togglePort, cnpjPermissionPort, eligibilityPolicy);
    }
}
