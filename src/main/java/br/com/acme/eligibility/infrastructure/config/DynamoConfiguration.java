package br.com.acme.eligibility.infrastructure.config;

import br.com.acme.eligibility.infrastructure.dynamo.DynamoProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

/** Fabrica dos clientes DynamoDB. */
@Configuration
public class DynamoConfiguration {

    @Bean
    public DynamoDbClient dynamoDbClient(DynamoProperties properties) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder()
                .region(Region.of(properties.getRegion()));

        if (!properties.getEndpoint().isEmpty()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }
}
