package com.underart;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.underart", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainIsFrameworkFree = noClasses()
            .that().resideInAPackage("com.underart.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "lombok..", "com.fasterxml..")
            .because("the domain is pure Java and does not depend on frameworks");

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage("com.underart.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.underart.application..", "com.underart.infrastructure..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage("com.underart.application..")
            .should().dependOnClassesThat().resideInAPackage("com.underart.infrastructure..")
            .because("use cases depend on domain ports");

    @ArchTest
    static final ArchRule portsAreInterfaces = classes()
            .that().resideInAnyPackage("com.underart.domain.port.in", "com.underart.domain.port.out")
            .and().areTopLevelClasses()
            .should().beInterfaces();

    @ArchTest
    static final ArchRule controllersLiveInTheWebAdapter = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("com.underart.infrastructure.web.controller");

    @ArchTest
    static final ArchRule jpaEntitiesStayInPersistence = classes()
            .that().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().resideInAPackage("com.underart.infrastructure.persistence.jpa.entity");
}
