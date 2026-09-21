package br.com.acme.eligibility.infrastructure.dynamo;

import br.com.acme.eligibility.application.port.out.CnpjPermissionPort;
import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.Regiao;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

/** Adaptador DynamoDB da porta de controle de CNPJ. */
@Component
public class CnpjPermissionDynamoAdapter implements CnpjPermissionPort {

    private final DynamoDbTable<CnpjPermissionItem> table;
    private final CnpjPermissionMapper mapper;

    public CnpjPermissionDynamoAdapter(DynamoDbEnhancedClient enhancedClient,
                                       DynamoProperties properties,
                                       CnpjPermissionMapper mapper) {
        this.table = enhancedClient.table(properties.getTableName(),
                TableSchema.fromBean(CnpjPermissionItem.class));
        this.mapper = mapper;
    }

    @Override
    public Optional<CnpjPermission> findBy(Cnpj cnpj, Regiao regiao) {
        Key key = Key.builder()
                .partitionValue(cnpj.getValue())
                .sortValue(regiao.getValue())
                .build();

        try {
            return Optional.ofNullable(table.getItem(key)).map(mapper::toDomain);
        } catch (SdkException exception) {
            throw new CnpjPermissionUnavailableException("cnpj permission store call failed", exception);
        }
    }
}
