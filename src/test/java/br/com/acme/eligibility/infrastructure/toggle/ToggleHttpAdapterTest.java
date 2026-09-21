package br.com.acme.eligibility.infrastructure.toggle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.acme.eligibility.domain.model.Cnpj;
import br.com.acme.eligibility.domain.model.Dicom;
import br.com.acme.eligibility.domain.model.EligibilityRequest;
import br.com.acme.eligibility.domain.model.ProductToggle;
import br.com.acme.eligibility.domain.model.Regiao;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

class ToggleHttpAdapterTest {

    private static final int SERVER_ERROR = 500;

    private MockWebServer server;
    private ToggleHttpAdapter adapter;
    private EligibilityRequest request;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        ToggleProperties properties = new ToggleProperties();
        properties.setBaseUrl(server.url("/").toString());

        ToggleApi api = new Retrofit.Builder()
                .baseUrl(properties.getBaseUrl())
                .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper()))
                .build()
                .create(ToggleApi.class);

        adapter = new ToggleHttpAdapter(api, new ToggleMapperImpl(), properties);
        request = new EligibilityRequest(Cnpj.of("12345678000195"), Dicom.of("DCM-1"), Regiao.of("sudeste"));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void shouldMapToggleWhenServiceRespondsSuccessfully() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"name\":\"eligibility-product\",\"enabled\":true}"));

        ProductToggle toggle = adapter.fetchToggle(request);

        assertThat(toggle.isEnabled()).isTrue();
        assertThat(toggle.getName()).isEqualTo("eligibility-product");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("X-Cnpj")).isEqualTo("12345678000195");
        assertThat(recorded.getPath()).doesNotContain("cnpj=");
    }

    @Test
    void shouldFailWhenToggleServiceReturnsError() {
        server.enqueue(new MockResponse().setResponseCode(SERVER_ERROR));

        assertThatThrownBy(() -> adapter.fetchToggle(request))
                .isInstanceOf(ToggleUnavailableException.class);
    }

    @Test
    void shouldFailWhenToggleServiceReturnsInvalidJson() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{not-json"));

        assertThatThrownBy(() -> adapter.fetchToggle(request))
                .isInstanceOf(ToggleUnavailableException.class);
    }
}
