package br.com.acme.eligibility.infrastructure.dynamo;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/** Item da tabela DynamoDB que controla o uso do produto por CNPJ e regiao. */
@Getter
@Setter
@DynamoDbBean
public class CnpjPermissionItem {

    private String cnpj;
    private String regiao;
    private boolean allowed;

    @DynamoDbPartitionKey
    public String getCnpj() {
        return cnpj;
    }

    @DynamoDbSortKey
    public String getRegiao() {
        return regiao;
    }
}
