package br.com.acme.eligibility.domain.model;

import lombok.Value;

/** Resultado da avaliacao de elegibilidade. */
@Value
public class EligibilityDecision {

    private final Cnpj cnpj;
    private final Regiao regiao;
    private final boolean eligible;
    private final DenialReason reason;

    public static EligibilityDecision allow(Cnpj cnpj, Regiao regiao) {
        return new EligibilityDecision(cnpj, regiao, true, null);
    }

    public static EligibilityDecision deny(Cnpj cnpj, Regiao regiao, DenialReason reason) {
        return new EligibilityDecision(cnpj, regiao, false, reason);
    }
}
