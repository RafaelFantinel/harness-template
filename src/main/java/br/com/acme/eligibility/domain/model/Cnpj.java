package br.com.acme.eligibility.domain.model;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import java.util.regex.Pattern;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Objeto de valor que garante um CNPJ sempre normalizado em 14 digitos. */
@Getter
@ToString
@EqualsAndHashCode
public final class Cnpj {

    private static final Pattern NON_DIGIT = Pattern.compile("\\D");
    private static final int LENGTH = 14;

    private final String value;

    private Cnpj(String value) {
        this.value = value;
    }

    public static Cnpj of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainValidationException("cnpj is required");
        }
        String digits = NON_DIGIT.matcher(raw).replaceAll("");
        if (digits.length() != LENGTH) {
            throw new DomainValidationException("cnpj must have " + LENGTH + " digits");
        }
        return new Cnpj(digits);
    }
}
