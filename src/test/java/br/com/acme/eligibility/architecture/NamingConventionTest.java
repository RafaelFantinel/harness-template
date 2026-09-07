package br.com.acme.eligibility.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.web.bind.annotation.RestController;

/** Convencoes de nomenclatura e de acesso entre componentes. */
@AnalyzeClasses(packages = "br.com.acme.eligibility", importOptions = ImportOption.DoNotIncludeTests.class)
class NamingConventionTest {

    @ArchTest
    static final ArchRule CONTROLLERS_ARE_SUFFIXED = classes()
            .that().areAnnotatedWith(RestController.class)
            .should().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule PORTS_ARE_INTERFACES = classes()
            .that().resideInAPackage("br.com.acme.eligibility.application.port..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule USE_CASES_ARE_SUFFIXED = classes()
            .that().resideInAPackage("br.com.acme.eligibility.application.usecase..")
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_TOUCH_ADAPTERS = noClasses()
            .that().resideInAPackage("br.com.acme.eligibility.presentation.api..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("br.com.acme.eligibility.infrastructure.dynamo..",
                    "br.com.acme.eligibility.infrastructure.toggle..");
}
