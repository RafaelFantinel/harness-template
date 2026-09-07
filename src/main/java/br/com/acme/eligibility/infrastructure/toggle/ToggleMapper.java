package br.com.acme.eligibility.infrastructure.toggle;

import br.com.acme.eligibility.domain.model.ProductToggle;
import org.mapstruct.Mapper;

/** Converte o payload do servico de toggles para o modelo de dominio. */
@Mapper
public interface ToggleMapper {

    ProductToggle toDomain(ToggleResponse response);
}
