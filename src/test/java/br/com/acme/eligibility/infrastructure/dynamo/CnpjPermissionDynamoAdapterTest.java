package br.com.acme.eligibility.infrastructure.dynamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import br.com.acme.eligibility.domain.exception.DomainValidationException;
import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.CnpjPermission;
import br.com.acme.eligibility.domain.model.Regiao;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@ExtendWith(MockitoExtension.class)
class CnpjPermissionDynamoAdapterTest {

    private static final String CNPJ = "12345678000195";
    private static final String REGIAO = "SUDESTE";

    @Mock
    private DynamoDbEnhancedClient enhancedClient;

    @Mock
    private DynamoDbTable<CnpjPermissionItem> table;

    private CnpjPermissionDynamoAdapter adapter;

    @BeforeEach
    void setUp() {
        DynamoProperties properties = new DynamoProperties();
        properties.setTableName("cnpj-product-control");
        doReturn(table).when(enhancedClient).table(eq("cnpj-product-control"), any());
        adapter = new CnpjPermissionDynamoAdapter(enhancedClient, properties, new CnpjPermissionMapperImpl());
    }

    @Test
    void shouldReturnAllowedWhenItemIsPresentAndAllowed() {
        when(table.getItem(any(Key.class))).thenReturn(item(true));

        Optional<CnpjPermission> permission = adapter.findBy(Cnpj.of(CNPJ), Regiao.of(REGIAO));

        assertThat(permission).isPresent();
        assertThat(permission.get().isAllowed()).isTrue();
    }

    @Test
    void shouldReturnBlockedWhenItemIsPresentAndNotAllowed() {
        when(table.getItem(any(Key.class))).thenReturn(item(false));

        Optional<CnpjPermission> permission = adapter.findBy(Cnpj.of(CNPJ), Regiao.of(REGIAO));

        assertThat(permission).isPresent();
        assertThat(permission.get().isAllowed()).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenItemIsAbsent() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        Optional<CnpjPermission> permission = adapter.findBy(Cnpj.of(CNPJ), Regiao.of(REGIAO));

        assertThat(permission).isEmpty();
    }

    @Test
    void shouldFailWhenSdkThrows() {
        when(table.getItem(any(Key.class))).thenThrow(SdkClientException.create("unreachable"));

        assertThatThrownBy(() -> adapter.findBy(Cnpj.of(CNPJ), Regiao.of(REGIAO)))
                .isInstanceOf(CnpjPermissionUnavailableException.class)
                .hasCauseInstanceOf(SdkClientException.class);
    }

    @Test
    void shouldRejectWhenStoredCnpjIsInvalid() {
        CnpjPermissionItem corrupted = new CnpjPermissionItem();
        corrupted.setCnpj("123");
        corrupted.setRegiao(REGIAO);
        corrupted.setAllowed(true);
        when(table.getItem(any(Key.class))).thenReturn(corrupted);

        assertThatThrownBy(() -> adapter.findBy(Cnpj.of(CNPJ), Regiao.of(REGIAO)))
                .isInstanceOf(DomainValidationException.class);
    }

    private static CnpjPermissionItem item(boolean allowed) {
        CnpjPermissionItem item = new CnpjPermissionItem();
        item.setCnpj(CNPJ);
        item.setRegiao(REGIAO);
        item.setAllowed(allowed);
        return item;
    }
}
