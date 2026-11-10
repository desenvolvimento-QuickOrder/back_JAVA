package com.quickorder.restaurant.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CamadasArquiteturaTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importarClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.quickorder.restaurant.core");
  }

  @Test
  void dominioNaoDependeDeFrameworks() {
    noClasses()
        .that()
        .resideInAnyPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "jakarta.persistence..",
            "tools.jackson..",
            "com.fasterxml.jackson..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  void aplicacaoNaoDependeDeInfraestrutura() {
    noClasses()
        .that()
        .resideInAnyPackage("..application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..")
        .allowEmptyShould(true)
        .check(classes);
  }

  @Test
  void webNaoDependeDeInfraestrutura() {
    noClasses()
        .that()
        .resideInAnyPackage("..web..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infrastructure..")
        .allowEmptyShould(true)
        .check(classes);
  }
}
