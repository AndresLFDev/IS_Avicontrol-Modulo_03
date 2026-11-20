package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM1Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CU07 (plan 001, T007): qué se escribe en la copia local por cada galpón de M1. */
class AplicarGalponM1ServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-20T15:00:00Z");
  private static final UUID GALPON = UUID.fromString("a402a368-cd4d-4e27-8d48-9e0d7eb8d374");
  private static final UUID LOTE_ACTIVO = UUID.fromString("2f545f14-128e-4e02-8b74-f00925789dc5");
  private static final UUID LOTE_ANTERIOR = UUID.fromString("7331eec8-e447-4868-bdef-8eca3fff8d44");

  private CopiaLocalM1Repository copia;
  private AplicarGalponM1Service servicio;

  @BeforeEach
  void preparar() {
    copia = mock(CopiaLocalM1Repository.class);
    servicio = new AplicarGalponM1Service(copia);
  }

  @Test
  void guardaElGalponYSuLoteActivo() {
    var detalle = galpon("PRODUCTIVO", lote(LOTE_ACTIVO, 1000L, 975L, 4_000_000L));

    var resultado = servicio.aplicar(detalle, HistorialLotesM1.vacio(GALPON), 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(2);
    assertThat(resultado.incidencias()).isEmpty();
    verify(copia).guardarGalpon(GALPON, "galpon demo", 5000, "PRODUCTIVO", AHORA);
    verify(copia)
        .guardarLote(
            LOTE_ACTIVO,
            GALPON,
            "lote demo",
            LocalDate.parse("2026-11-18"),
            1000,
            975,
            4_000_000L,
            "ACTIVO",
            AHORA);
    verify(copia, never()).crearAlertaVaciadoSiNoExiste(any(), any(), any(), any(), anyLong());
  }

  @Test
  void unGalponSinLoteActivoSoloGuardaElGalpon() {
    var resultado =
        servicio.aplicar(galpon("DISPONIBLE", null), HistorialLotesM1.vacio(GALPON), 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(1);
    verify(copia).guardarGalpon(GALPON, "galpon demo", 5000, "DISPONIBLE", AHORA);
    verify(copia, never())
        .guardarLote(any(), any(), anyString(), any(), anyInt(), anyInt(), anyLong(), any(), any());
  }

  @Test
  void unLoteAnteriorSeGuardaDesvinculadoYCreaSuAlertaDeVaciado() {
    var historial =
        new HistorialLotesM1(GALPON, null, List.of(lote(LOTE_ANTERIOR, 2000L, 1974L, 5_000_000L)));
    when(copia.crearAlertaVaciadoSiNoExiste(
            any(), eq(GALPON), eq(LOTE_ANTERIOR), eq(AHORA), eq(7L)))
        .thenReturn(true);

    var resultado = servicio.aplicar(galpon("VACIADO_SANITARIO", null), historial, 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(3); // galpón, lote y alerta
    verify(copia)
        .guardarLote(
            LOTE_ANTERIOR,
            GALPON,
            "lote demo",
            LocalDate.parse("2026-11-18"),
            2000,
            1974,
            5_000_000L,
            "DESVINCULADO",
            AHORA);
    verify(copia)
        .crearAlertaVaciadoSiNoExiste(
            AplicarGalponM1Service.idAlertaDerivada(LOTE_ANTERIOR),
            GALPON,
            LOTE_ANTERIOR,
            AHORA,
            7L);
  }

  @Test
  void laAlertaYaExistenteNoSeCuentaNiSeModifica() {
    var historial =
        new HistorialLotesM1(GALPON, null, List.of(lote(LOTE_ANTERIOR, 2000L, 1974L, 5_000_000L)));
    when(copia.crearAlertaVaciadoSiNoExiste(any(), any(), any(), any(), anyLong()))
        .thenReturn(false);

    var resultado = servicio.aplicar(galpon("VACIADO_SANITARIO", null), historial, 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(2); // galpón y lote, sin alerta nueva
  }

  @Test
  void elIdDeLaAlertaDerivadaEsElMismoParaElMismoLote() {
    assertThat(AplicarGalponM1Service.idAlertaDerivada(LOTE_ANTERIOR))
        .isEqualTo(AplicarGalponM1Service.idAlertaDerivada(LOTE_ANTERIOR))
        .isNotEqualTo(AplicarGalponM1Service.idAlertaDerivada(LOTE_ACTIVO));
  }

  @Test
  void unGalponConEstadoFueraDelCatalogoSeOmiteSinEscribirNada() {
    var resultado =
        servicio.aplicar(galpon("INVENTADO", null), HistorialLotesM1.vacio(GALPON), 7L, AHORA);

    assertThat(resultado.registros()).isZero();
    assertThat(resultado.incidencias()).singleElement().asString().contains("INVENTADO");
    verify(copia, never()).guardarGalpon(any(), anyString(), anyInt(), anyString(), any());
  }

  @Test
  void unLoteInvalidoSeOmiteYElGalponSiSeGuarda() {
    var sinCosto = lote(LOTE_ACTIVO, 1000L, 975L, 0L);

    var resultado =
        servicio.aplicar(galpon("PRODUCTIVO", sinCosto), HistorialLotesM1.vacio(GALPON), 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(1); // solo el galpón
    assertThat(resultado.incidencias()).singleElement().asString().contains("costo");
    verify(copia).guardarGalpon(eq(GALPON), anyString(), anyInt(), eq("PRODUCTIVO"), eq(AHORA));
    verify(copia, never())
        .guardarLote(any(), any(), anyString(), any(), anyInt(), anyInt(), anyLong(), any(), any());
  }

  @Test
  void rechazaPoblacionActualMayorQueLaInicialYPoblacionInicialAusente() {
    var historial =
        new HistorialLotesM1(
            GALPON,
            null,
            List.of(
                lote(LOTE_ANTERIOR, 1000L, 1500L, 5_000_000L),
                lote(UUID.randomUUID(), null, 10L, 5_000_000L)));

    var resultado = servicio.aplicar(galpon("VACIADO_SANITARIO", null), historial, 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(1);
    assertThat(resultado.incidencias()).hasSize(2);
    verify(copia, never()).crearAlertaVaciadoSiNoExiste(any(), any(), any(), any(), anyLong());
  }

  @Test
  void aceptaPoblacionActualCeroPorSiniestroTotal() {
    var detalle = galpon("PRODUCTIVO", lote(LOTE_ACTIVO, 1000L, 0L, 4_000_000L));

    var resultado = servicio.aplicar(detalle, HistorialLotesM1.vacio(GALPON), 7L, AHORA);

    assertThat(resultado.registros()).isEqualTo(2);
  }

  private static GalponM1 galpon(String estado, LoteM1 activo) {
    return new GalponM1(GALPON, "galpon demo", 5000L, estado, activo);
  }

  private static LoteM1 lote(UUID id, Long inicial, Long actual, Long costo) {
    return new LoteM1(id, "lote demo", LocalDate.parse("2026-11-18"), inicial, actual, costo);
  }
}
