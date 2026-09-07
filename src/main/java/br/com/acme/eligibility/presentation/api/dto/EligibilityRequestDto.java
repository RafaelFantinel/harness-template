package br.com.acme.eligibility.presentation.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Corpo da requisicao de consulta de elegibilidade. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequestDto {

    @NotBlank
    @Pattern(regexp = "\\d{2}\\.?\\d{3}\\.?\\d{3}/?\\d{4}-?\\d{2}", message = "cnpj format is invalid")
    private String cnpj;

    @NotBlank
    private String dicom;

    @NotBlank
    private String regiao;
}
