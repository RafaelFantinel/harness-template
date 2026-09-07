package br.com.acme.eligibility.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** O nucleo (dominio e aplicacao) nao pode depender de framework nem de I/O. */
@AnalyzeClasses(packages = "br.com.acme.eligibility", importOptions = ImportOption.DoNotIncludeTests.class)
class CleanDomainTest {

    @ArchTest
    static final ArchRule DOMAIN_HAS_NO_FRAMEWORK = noClasses()
            .that().resideInAPackage("br.com.acme.eligibility.domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "software.amazon..", "retrofit2..", "okhttp3..",
                    "javax.persistence..", "com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule APPLICATION_HAS_NO_FRAMEWORK = noClasses()
            .that().resideInAPackage("br.com.acme.eligibility.application..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..", "software.amazon..", "retrofit2..", "okhttp3..");
}
