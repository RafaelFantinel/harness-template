package br.com.acme.eligibility.domain.model;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import java.util.Locale;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Objeto de valor da regiao de atendimento, normalizado em maiusculo. */
@Getter
@ToString
@EqualsAndHashCode
public final class Regiao {

    private final String value;

    private Regiao(String value) {
        this.value = value;
    }

    public static Regiao of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainValidationException("regiao is required");
        }
        return new Regiao(raw.trim().toUpperCase(Locale.ROOT));
    }
}
