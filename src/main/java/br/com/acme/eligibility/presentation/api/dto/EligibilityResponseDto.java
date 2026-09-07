package br.com.acme.eligibility.presentation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resposta da consulta de elegibilidade. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityResponseDto {

    private String cnpj;
    private String regiao;
    private boolean eligible;
    private String reason;
}
