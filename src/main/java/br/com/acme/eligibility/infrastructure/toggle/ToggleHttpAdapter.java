package br.com.acme.eligibility.infrastructure.toggle;

import br.com.acme.eligibility.application.port.out.TogglePort;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Response;

/** Adaptador Retrofit da porta de toggles. */
@Slf4j
@Component
@Profile("!fake")
@RequiredArgsConstructor
public class ToggleHttpAdapter implements TogglePort {

    private final ToggleApi toggleApi;
    private final ToggleMapper toggleMapper;
    private final ToggleProperties properties;

    @Override
    public ProductToggle fetchToggle(EligibilityRequest request) {
        try {
            Response<ToggleResponse> response = toggleApi.getToggle(
                    properties.getProduct(),
                    request.getCnpj().getValue(),
                    request.getRegiao().getValue(),
                    request.getDicom().getValue()).execute();

            if (!response.isSuccessful() || response.body() == null) {
                throw new ToggleUnavailableException("toggle service returned status " + response.code());
            }
            return toggleMapper.toDomain(response.body());
        } catch (IOException exception) {
            throw new ToggleUnavailableException("toggle service call failed", exception);
        }
    }
}
