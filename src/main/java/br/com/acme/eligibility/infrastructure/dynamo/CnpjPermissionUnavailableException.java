package br.com.acme.eligibility.infrastructure.dynamo;

/** Sinaliza indisponibilidade da tabela de controle de CNPJ. */
public class CnpjPermissionUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CnpjPermissionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
