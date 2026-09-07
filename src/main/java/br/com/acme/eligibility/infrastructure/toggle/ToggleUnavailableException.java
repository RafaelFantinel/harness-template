package br.com.acme.eligibility.infrastructure.toggle;

/** Sinaliza indisponibilidade ou resposta invalida do servico de toggles. */
public class ToggleUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ToggleUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ToggleUnavailableException(String message) {
        super(message);
    }
}
