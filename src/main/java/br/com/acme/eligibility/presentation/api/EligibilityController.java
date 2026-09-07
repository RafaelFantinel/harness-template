package br.com.acme.eligibility.presentation.api;

import br.com.acme.eligibility.application.usecase.CheckEligibilityUseCase;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.presentation.api.dto.EligibilityRequestDto;
import br.com.acme.eligibility.presentation.api.dto.EligibilityResponseDto;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Rota de consulta de elegibilidade por CNPJ, DICOM e regiao. */
@RestController
@RequestMapping("/v1/elegibilidade")
@RequiredArgsConstructor
public class EligibilityController {

    private final CheckEligibilityUseCase checkEligibilityUseCase;
    private final EligibilityApiMapper mapper;

    @PostMapping
    public ResponseEntity<EligibilityResponseDto> check(@Valid @RequestBody EligibilityRequestDto requestDto) {
        EligibilityDecision decision = checkEligibilityUseCase.execute(mapper.toDomain(requestDto));
        return ResponseEntity.ok(mapper.toResponse(decision));
    }
}
