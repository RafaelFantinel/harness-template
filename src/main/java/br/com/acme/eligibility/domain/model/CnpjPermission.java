package br.com.acme.eligibility.domain.model;

import lombok.Value;

/** Registro de controle que autoriza (ou nao) o CNPJ a usar o produto na regiao. */
@Value
public class CnpjPermission {

    private final Cnpj cnpj;
    private final Regiao regiao;
    private final boolean allowed;
}
