package br.com.acme.eligibility.application.port.out;

import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;

/** Porta de saida para o servico externo de toggles. */
public interface TogglePort {

    ProductToggle fetchToggle(EligibilityRequest request);
}
