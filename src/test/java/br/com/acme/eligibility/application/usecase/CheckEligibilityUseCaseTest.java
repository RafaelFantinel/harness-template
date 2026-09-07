package br.com.acme.eligibility.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.acme.eligibility.application.port.out.CnpjPermissionPort;
import br.com.acme.eligibility.application.port.out.TogglePort;
import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.DenialReason;
import br.com.acme.eligibility.domain.model.Dicom;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import br.com.acme.eligibility.domain.model.Regiao;
import br.com.acme.eligibility.domain.policy.EligibilityPolicy;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckEligibilityUseCaseTest {

    private static final String CNPJ = "12.345.678/0001-95";
    private static final String REGIAO = "sudeste";

    @Mock
    private TogglePort togglePort;

    @Mock
    private CnpjPermissionPort cnpjPermissionPort;

    private CheckEligibilityUseCase useCase;
    private EligibilityRequest request;

    @BeforeEach
    void setUp() {
        useCase = new CheckEligibilityUseCase(togglePort, cnpjPermissionPort, new EligibilityPolicy());
        request = new EligibilityRequest(Cnpj.of(CNPJ), Dicom.of("DCM-1"), Regiao.of(REGIAO));
    }

    @Test
    void shouldAllowWhenToggleEnabledAndCnpjAllowed() {
        when(togglePort.fetchToggle(any())).thenReturn(new ProductToggle("produto", true));
        when(cnpjPermissionPort.findBy(any(), any()))
                .thenReturn(Optional.of(new CnpjPermission(Cnpj.of(CNPJ), Regiao.of(REGIAO), true)));

        EligibilityDecision decision = useCase.execute(request);

        assertThat(decision.isEligible()).isTrue();
    }

    @Test
    void shouldDenyWithoutReadingControlTableWhenToggleDisabled() {
        when(togglePort.fetchToggle(any())).thenReturn(new ProductToggle("produto", false));

        EligibilityDecision decision = useCase.execute(request);

        assertThat(decision.getReason()).isEqualTo(DenialReason.PRODUCT_DISABLED_FOR_REGION);
        verifyNoInteractions(cnpjPermissionPort);
    }

    @Test
    void shouldDenyWhenCnpjNotFoundInControlTable() {
        when(togglePort.fetchToggle(any())).thenReturn(new ProductToggle("produto", true));
        when(cnpjPermissionPort.findBy(any(), any())).thenReturn(Optional.empty());

        EligibilityDecision decision = useCase.execute(request);

        assertThat(decision.getReason()).isEqualTo(DenialReason.CNPJ_NOT_REGISTERED);
    }

    @Test
    void shouldDenyWhenCnpjBlocked() {
        when(togglePort.fetchToggle(any())).thenReturn(new ProductToggle("produto", true));
        when(cnpjPermissionPort.findBy(any(), any()))
                .thenReturn(Optional.of(new CnpjPermission(Cnpj.of(CNPJ), Regiao.of(REGIAO), false)));

        EligibilityDecision decision = useCase.execute(request);

        assertThat(decision.getReason()).isEqualTo(DenialReason.CNPJ_BLOCKED);
    }
}
