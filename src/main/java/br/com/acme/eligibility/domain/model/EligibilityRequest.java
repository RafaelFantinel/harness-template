package br.com.acme.eligibility.domain.model;

import lombok.Value;

/** Dados de entrada de uma consulta de elegibilidade. */
@Value
public class EligibilityRequest {

    private final Cnpj cnpj;
    private final Dicom dicom;
    private final Regiao regiao;
}
