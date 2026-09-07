package br.com.acme.eligibility.presentation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada do servico. */
@SpringBootApplication(scanBasePackages = "br.com.acme.eligibility")
public class EligibilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(EligibilityApplication.class, args);
    }
}
