package br.com.acme.eligibility.infrastructure.dynamo;

import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.Regiao;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/** Converte o item do DynamoDB para o modelo de dominio. */
@Mapper
public interface CnpjPermissionMapper {

    @Mapping(target = "cnpj", source = "cnpj", qualifiedByName = "toCnpj")
    @Mapping(target = "regiao", source = "regiao", qualifiedByName = "toRegiao")
    CnpjPermission toDomain(CnpjPermissionItem item);

    @Named("toCnpj")
    default Cnpj toCnpj(String value) {
        return Cnpj.of(value);
    }

    @Named("toRegiao")
    default Regiao toRegiao(String value) {
        return Regiao.of(value);
    }
}
