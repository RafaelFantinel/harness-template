package br.com.acme.eligibility.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.acme.eligibility.application.usecase.CheckEligibilityUseCase;
import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.DenialReason;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.domain.model.Regiao;
import br.com.acme.eligibility.presentation.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EligibilityController.class)
@Import({EligibilityApiMapperImpl.class, ApiExceptionHandler.class})
class EligibilityControllerTest {

    private static final String PAYLOAD =
            "{\"cnpj\":\"12.345.678/0001-95\",\"dicom\":\"DCM-1\",\"regiao\":\"sudeste\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CheckEligibilityUseCase checkEligibilityUseCase;

    @Test
    void shouldReturnEligibleWhenUseCaseAllows() throws Exception {
        when(checkEligibilityUseCase.execute(any()))
                .thenReturn(EligibilityDecision.allow(Cnpj.of("12345678000195"), Regiao.of("SUDESTE")));

        mockMvc.perform(post("/v1/elegibilidade").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.cnpj").value("12345678000195"));
    }

    @Test
    void shouldReturnReasonWhenUseCaseDenies() throws Exception {
        when(checkEligibilityUseCase.execute(any()))
                .thenReturn(EligibilityDecision.deny(Cnpj.of("12345678000195"), Regiao.of("SUDESTE"),
                        DenialReason.CNPJ_BLOCKED));

        mockMvc.perform(post("/v1/elegibilidade").contentType(MediaType.APPLICATION_JSON).content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reason").value("CNPJ_BLOCKED"));
    }

    @Test
    void shouldReturnBadRequestWhenCnpjIsInvalid() throws Exception {
        String invalid = "{\"cnpj\":\"123\",\"dicom\":\"DCM-1\",\"regiao\":\"sudeste\"}";

        mockMvc.perform(post("/v1/elegibilidade").contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
