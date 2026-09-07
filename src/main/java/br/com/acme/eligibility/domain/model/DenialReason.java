package br.com.acme.eligibility.domain.model;

/** Motivos possiveis para a negativa de uso do produto. */
public enum DenialReason {

    PRODUCT_DISABLED_FOR_REGION,
    CNPJ_NOT_REGISTERED,
    CNPJ_BLOCKED
}
