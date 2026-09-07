package br.com.acme.eligibility.domain.policy;

import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.DenialReason;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import java.util.Optional;

/**
 * Regras de elegibilidade em duas etapas. O toggle e avaliado primeiro porque
 * desliga o produto inteiro na regiao, dispensando a consulta ao controle de CNPJ.
 */
public class EligibilityPolicy {

    /** Devolve a negativa quando o produto esta desligado para a regiao. */
    public Optional<EligibilityDecision> denyWhenProductDisabled(EligibilityRequest request, ProductToggle toggle) {
        if (toggle.isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(EligibilityDecision.deny(request.getCnpj(), request.getRegiao(),
                DenialReason.PRODUCT_DISABLED_FOR_REGION));
    }

    /** Decide com base no registro de controle do CNPJ. */
    public EligibilityDecision decideByPermission(EligibilityRequest request, Optional<CnpjPermission> permission) {
        if (!permission.isPresent()) {
            return EligibilityDecision.deny(request.getCnpj(), request.getRegiao(),
                    DenialReason.CNPJ_NOT_REGISTERED);
        }
        if (!permission.get().isAllowed()) {
            return EligibilityDecision.deny(request.getCnpj(), request.getRegiao(),
                    DenialReason.CNPJ_BLOCKED);
        }
        return EligibilityDecision.allow(request.getCnpj(), request.getRegiao());
    }
}
