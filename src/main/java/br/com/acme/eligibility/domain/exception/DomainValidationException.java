package br.com.acme.eligibility.domain.exception;

/** Sinaliza dado de entrada que viola uma invariante de dominio. */
public class DomainValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DomainValidationException(String message) {
        super(message);
    }
}
