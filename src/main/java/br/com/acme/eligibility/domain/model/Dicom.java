package br.com.acme.eligibility.domain.model;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/** Objeto de valor do codigo DICOM informado pelo solicitante. */
@Getter
@ToString
@EqualsAndHashCode
public final class Dicom {

    private static final int MAX_LENGTH = 32;

    private final String value;

    private Dicom(String value) {
        this.value = value;
    }

    public static Dicom of(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new DomainValidationException("dicom is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new DomainValidationException("dicom must have at most " + MAX_LENGTH + " characters");
        }
        return new Dicom(trimmed);
    }
}
