package co.edu.unimagdalena.avicontrol;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Reglas de dependencias de la arquitectura limpia (docs/arquitectura/general.md): el dominio es
 * Java puro y la aplicación no conoce los detalles de infraestructura. Un PR que las rompa no pasa
 * la CI.
 */
@AnalyzeClasses(
    packages = "co.edu.unimagdalena.avicontrol",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String DOMINIO = "..avicontrol.domain..";
  private static final String APLICACION = "..avicontrol.application..";
  private static final String INFRAESTRUCTURA = "..avicontrol.infrastructure..";

  @ArchTest
  static final ArchRule dominioNoDependeDeOtrasCapas =
      noClasses()
          .that()
          .resideInAPackage(DOMINIO)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(APLICACION, INFRAESTRUCTURA)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule dominioSinFrameworks =
      noClasses()
          .that()
          .resideInAPackage(DOMINIO)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..",
              "jakarta..",
              "tools.jackson..",
              "com.fasterxml.jackson..",
              "org.apache.kafka..",
              "org.apache.poi..",
              "lombok..")
          .because("el dominio es Java puro")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule aplicacionNoDependeDeInfraestructura =
      noClasses()
          .that()
          .resideInAPackage(APLICACION)
          .should()
          .dependOnClassesThat()
          .resideInAPackage(INFRAESTRUCTURA)
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule aplicacionSinDetallesTecnicos =
      noClasses()
          .that()
          .resideInAPackage(APLICACION)
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "jakarta.persistence..",
              "org.springframework.data.jpa..",
              "org.springframework.web..",
              "org.springframework.kafka..",
              "org.apache.kafka..",
              "org.apache.poi..")
          .because("la aplicación solo orquesta el dominio a través de sus puertos")
          .allowEmptyShould(true);
}
