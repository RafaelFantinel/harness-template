package br.com.acme.eligibility.infrastructure.dynamo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuracao de acesso ao DynamoDB. */
@Getter
@Setter
@ConfigurationProperties(prefix = "eligibility.dynamo")
public class DynamoProperties {

    private String tableName = "cnpj-product-control";
    private String region = "us-east-1";

    /** Endpoint alternativo para LocalStack; vazio usa o endpoint padrao da AWS. */
    private String endpoint = "";
}
