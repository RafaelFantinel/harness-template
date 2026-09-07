package br.com.acme.eligibility.infrastructure.toggle;

import br.com.acme.eligibility.application.port.out.TogglePort;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Servico de toggles falso usado em desenvolvimento e testes locais.
 * Ativo apenas no profile {@code fake}, evitando dependencia de rede.
 */
@Component
@Profile("fake")
@RequiredArgsConstructor
public class FakeToggleAdapter implements TogglePort {

    private static final Set<String> DISABLED_REGIONS = Set.of("NORTE");

    private final ToggleProperties properties;

    @Override
    public ProductToggle fetchToggle(EligibilityRequest request) {
        boolean enabled = !DISABLED_REGIONS.contains(request.getRegiao().getValue());
        return new ProductToggle(properties.getProduct(), enabled);
    }
}
