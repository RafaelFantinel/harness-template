package br.com.acme.eligibility.application.usecase;

import br.com.acme.eligibility.application.port.out.CnpjPermissionPort;
import br.com.acme.eligibility.application.port.out.TogglePort;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import br.com.acme.eligibility.domain.policy.EligibilityPolicy;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Orquestra as consultas externas e delega a decisao para a politica de dominio. */
@Slf4j
@RequiredArgsConstructor
public class CheckEligibilityUseCase {

    private final TogglePort togglePort;
    private final CnpjPermissionPort cnpjPermissionPort;
    private final EligibilityPolicy policy;

    public EligibilityDecision execute(EligibilityRequest request) {
        ProductToggle toggle = togglePort.fetchToggle(request);

        // Produto desligado na regiao dispensa a leitura no DynamoDB.
        Optional<EligibilityDecision> toggleDenial = policy.denyWhenProductDisabled(request, toggle);
        if (toggleDenial.isPresent()) {
            return log(request, toggleDenial.get());
        }

        Optional<CnpjPermission> permission = cnpjPermissionPort.findBy(request.getCnpj(), request.getRegiao());
        return log(request, policy.decideByPermission(request, permission));
    }

    private EligibilityDecision log(EligibilityRequest request, EligibilityDecision decision) {
        log.info("eligibility evaluated regiao={} eligible={} reason={}",
                request.getRegiao().getValue(), decision.isEligible(), decision.getReason());
        return decision;
    }
}
