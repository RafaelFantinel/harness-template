package br.com.acme.eligibility.application.port.out;

import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.Regiao;
import java.util.Optional;

/** Porta de saida para a tabela de controle de CNPJ. */
public interface CnpjPermissionPort {

    Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao);
}
