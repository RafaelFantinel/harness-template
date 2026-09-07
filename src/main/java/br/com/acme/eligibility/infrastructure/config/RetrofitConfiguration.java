package br.com.acme.eligibility.infrastructure.config;

import br.com.acme.eligibility.infrastructure.toggle.ToggleApi;
import br.com.acme.eligibility.infrastructure.toggle.ToggleProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/** Fabrica do cliente Retrofit do servico de toggles. */
@Configuration
public class RetrofitConfiguration {

    @Bean
    public OkHttpClient toggleHttpClient(ToggleProperties properties) {
        Duration timeout = Duration.ofSeconds(properties.getTimeoutSeconds());
        return new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(timeout)
                .callTimeout(timeout)
                .build();
    }

    @Bean
    public ToggleApi toggleApi(OkHttpClient toggleHttpClient, ToggleProperties properties, ObjectMapper objectMapper) {
        return new Retrofit.Builder()
                .baseUrl(properties.getBaseUrl())
                .client(toggleHttpClient)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(ToggleApi.class);
    }
}
