package br.com.acme.eligibility.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import br.com.acme.eligibility.domain.model.Cnpj;
import org.junit.jupiter.api.Test;

class CnpjTest {

    @Test
    void shouldNormalizeWhenCnpjIsFormatted() {
        assertThat(Cnpj.of("12.345.678/0001-95").getValue()).isEqualTo("12345678000195");
    }

    @Test
    void shouldRejectWhenDigitCountIsWrong() {
        assertThatThrownBy(() -> Cnpj.of("123"))
                .isInstanceOf(DomainValidationException.class)
                .hasMessageContaining("14 digits");
    }
}
