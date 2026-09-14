package org.openathar.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "org.openathar.api")
class ArchitectureTest {

    @ArchTest
    static final ArchRule domain_has_no_spring_deps =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "..adapter..");

    @ArchTest
    static final ArchRule application_depends_only_on_ports =
        classes().that().resideInAPackage("..application..").and().haveSimpleNameNotEndingWith("Test")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..port..", "..application..", "java..", "org.springframework..", "org.openathar.core..");

    @ArchTest
    static final ArchRule web_adapter_depends_on_ports_and_application =
        classes().that().resideInAPackage("..adapter.web..").and().haveSimpleNameNotEndingWith("Test")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..domain..", "..port..", "..application..", "..adapter.web..", "java..", "org.springframework..", "org.mapstruct..");
}