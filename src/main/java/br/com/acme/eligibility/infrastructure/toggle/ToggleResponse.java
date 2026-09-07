package br.com.acme.eligibility.infrastructure.toggle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload devolvido pelo servico de toggles. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ToggleResponse {

    private String name;
    private boolean enabled;
}
