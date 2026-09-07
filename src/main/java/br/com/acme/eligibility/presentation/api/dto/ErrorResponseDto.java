package br.com.acme.eligibility.presentation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** Erro devolvido ao cliente, sem detalhes internos. */
@Data
@AllArgsConstructor
public class ErrorResponseDto {

    private String code;
    private String message;
}
