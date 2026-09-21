package br.com.acme.eligibility.infrastructure.toggle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.Dicom;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import br.com.acme.eligibility.domain.model.Regiao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FakeToggleAdapterTest {

    private FakeToggleAdapter adapter;

    @BeforeEach
    void setUp() {
        ToggleProperties properties = new ToggleProperties();
        properties.setProduct("eligibility-product");
        adapter = new FakeToggleAdapter(properties);
    }

    @Test
    void shouldFailWhenRegionIsUnavailable() {
        EligibilityRequest request = request("UNAVAILABLE");

        assertThatThrownBy(() -> adapter.fetchToggle(request))
                .isInstanceOf(ToggleUnavailableException.class);
    }

    @Test
    void shouldDisableWhenRegionIsNorte() {
        ProductToggle toggle = adapter.fetchToggle(request("NORTE"));

        assertThat(toggle.isEnabled()).isFalse();
    }

    @Test
    void shouldEnableWhenRegionIsSudeste() {
        ProductToggle toggle = adapter.fetchToggle(request("SUDESTE"));

        assertThat(toggle.isEnabled()).isTrue();
    }

    private static EligibilityRequest request(String regiao) {
        return new EligibilityRequest(Cnpj.of("12345678000195"), Dicom.of("DCM-1"), Regiao.of(regiao));
    }
}
