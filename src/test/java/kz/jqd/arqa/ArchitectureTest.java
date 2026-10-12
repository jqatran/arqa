package kz.jqd.arqa;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Keeps the layering from eroding: lower layers must not know about the layers above them. */
@AnalyzeClasses(packages = "kz.jqd.arqa", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnWeb = noClasses()
            .that().resideInAnyPackage("..entity..", "..model..", "..repository..", "..exception..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..dto..", "..service..", "..seed..")
            .as("entity, model, repository and exception must not depend on controller, dto, service or seed");

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
            .that().resideInAnyPackage("..service..", "..validation..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..", "..seed..")
            .as("services and validation must not depend on controllers or the seed loader");

    @ArchTest
    static final ArchRule controllersDoNotTouchRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..validation..")
            .as("controllers talk to services only");

    @ArchTest
    static final ArchRule entryPointsDependOnServiceInterfacesOnly = noClasses()
            .that().resideInAnyPackage("..controller..", "..seed..")
            .should().dependOnClassesThat().haveSimpleNameStartingWith("Default")
            .as("controllers and the seed loader use service interfaces, never the Default* implementations");

    @ArchTest
    static final ArchRule domainDoesNotUseJackson = noClasses()
            .that().resideInAnyPackage("..entity..", "..model..")
            .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson..")
            .as("the domain must not know the JSON library");
}
