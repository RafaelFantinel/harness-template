package br.com.acme.eligibility.presentation.api;

import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.Dicom;
import br.com.acme.eligibility.domain.model.EligibilityDecision;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.Regiao;
import br.com.acme.eligibility.presentation.api.dto.EligibilityRequestDto;
import br.com.acme.eligibility.presentation.api.dto.EligibilityResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Converte DTOs da API para o modelo de dominio e vice-versa. */
@Mapper
public interface EligibilityApiMapper {

    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "toCnpj")
    @Mapping(target = "dicom", source = "dicom", qualifiedByName = "toDicom")
    @Mapping(target = "regiao", source = "regiao", qualifiedByName = "toRegiao")
    EligibilityRequest toDomain(EligibilityRequestDto dto);

    @Mapping(target = "cnpj", source = "cnpj.value")
    @Mapping(target = "regiao", source = "regiao.value")
    @Mapping(target = "reason", source = "reason")
    EligibilityResponseDto toResponse(EligibilityDecision decision);

    @Named("toCnpj")
    default Cnpj toCnpj(String value) {
        return Cnpj.of(value);
    }

    @Named("toDicom")
    default Dicom toDicom(String value) {
        return Dicom.of(value);
    }

    @Named("toRegiao")
    default Regiao toRegiao(String value) {
        return Regiao.of(value);
    }
}
