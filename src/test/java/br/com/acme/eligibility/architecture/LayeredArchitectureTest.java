package br.com.acme.eligibility.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

/** Garante que as dependencias entre camadas apontem sempre para dentro. */
@AnalyzeClasses(packages = "br.com.acme.eligibility", importOptions = ImportOption.DoNotIncludeTests.class)
class LayeredArchitectureTest {

    @ArchTest
    static final ArchRule LAYERS_ARE_RESPECTED = Architectures.layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("br.com.acme.eligibility.domain..")
            .layer("Application").definedBy("br.com.acme.eligibility.application..")
            .layer("Infrastructure").definedBy("br.com.acme.eligibility.infrastructure..")
            .layer("Presentation").definedBy("br.com.acme.eligibility.presentation..")
            .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
            .whereLayer("Infrastructure").mayOnlyBeAccessedByLayers("Presentation")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation");
}
