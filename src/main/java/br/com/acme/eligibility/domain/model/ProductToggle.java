package br.com.acme.eligibility.domain.model;

import lombok.Value;

/** Resposta do servico externo de toggles para o produto na regiao consultada. */
@Value
public class ProductToggle {

    private final String name;
    private final boolean enabled;
}
