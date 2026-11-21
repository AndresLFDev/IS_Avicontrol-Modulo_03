package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarFinanzasM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Una ejecución de la sincronización con M2 y su bitácora. */
class SyncModulo2ServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-21T10:00:00Z");
  private static final LoteConsultableM2 UNO =
      new LoteConsultableM2(UUID.randomUUID(), UUID.randomUUID());
  private static final LoteConsultableM2 DOS =
      new LoteConsultableM2(UUID.randomUUID(), UUID.randomUUID());

  private ConsultarFinanzasM2 m2;
  private CopiaLocalM2Repository copia;
  private AplicarLoteM2Service aplicador;
  private RegistroSincronizacionRepository bitacora;
  private SyncModulo2Service servicio;

  @BeforeEach
  void preparar() {
    m2 = mock(ConsultarFinanzasM2.class);
    copia = mock(CopiaLocalM2Repository.class);
    aplicador = mock(AplicarLoteM2Service.class);
    bitacora = mock(RegistroSincronizacionRepository.class);
    when(bitacora.iniciar(eq(FuenteSincronizacion.MODULO_2), any(), any())).thenReturn(8L);
    when(m2.resultado(any())).thenReturn(Optional.empty());
    when(m2.alimentos(any())).thenReturn(List.of());
    when(m2.medicamentos(any())).thenReturn(List.of());
    servicio =
        new SyncModulo2Service(m2, copia, aplicador, bitacora, Clock.fixed(AHORA, ZoneOffset.UTC));
  }

  @Test
  void consultaCadaLoteYCierraLaBitacoraComoExitosa() {
    when(copia.lotesAConsultar()).thenReturn(List.of(UNO, DOS));
    when(aplicador.aplicar(any(), any(), any(), any(), any()))
        .thenReturn(new AplicarLoteM2Service.Aplicacion(3, List.of()));

    var resumen = servicio.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.registrosActualizados()).isEqualTo(6);
    verify(bitacora)
        .iniciar(
            FuenteSincronizacion.MODULO_2, "GET /api/integracion/modulo3/lotes/{loteId}", AHORA);
    verify(bitacora).completar(8L, 6, null, AHORA);
    verify(m2).alimentos(UNO.idLote());
    verify(m2).medicamentos(DOS.idLote());
  }

  @Test
  void sinLotesPorConsultarCompletaSinLlamarAM2() {
    when(copia.lotesAConsultar()).thenReturn(List.of());

    var resumen = servicio.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.registrosActualizados()).isZero();
    verify(bitacora).completar(8L, 0, null, AHORA);
    verify(m2, never()).alimentos(any());
  }

  @Test
  void losDatosOmitidosQuedanEnLaNotaSinFallarLaEjecucion() {
    when(copia.lotesAConsultar()).thenReturn(List.of(UNO));
    when(aplicador.aplicar(any(), any(), any(), any(), any()))
        .thenReturn(new AplicarLoteM2Service.Aplicacion(1, List.of("a omitido", "b omitido")));

    var resumen = servicio.sincronizar();

    assertThat(resumen.exitosa()).isTrue();
    assertThat(resumen.incidencias()).hasSize(2);
    verify(bitacora).completar(8L, 1, "a omitido; b omitido", AHORA);
  }

  @Test
  void siM2FallaLaEjecucionQuedaFallidaYNoSeSigueConsultando() {
    when(copia.lotesAConsultar()).thenReturn(List.of(UNO, DOS));
    when(m2.alimentos(UNO.idLote())).thenThrow(new ConsultaM2Exception("M2 sin respuesta"));

    var resumen = servicio.sincronizar();

    assertThat(resumen.exitosa()).isFalse();
    assertThat(resumen.error()).isEqualTo("M2 sin respuesta");
    verify(bitacora).fallar(8L, "M2 sin respuesta", AHORA);
    verify(bitacora, never())
        .completar(anyLong(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    verify(m2, never()).alimentos(DOS.idLote());
  }

  @Test
  void unErrorInesperadoTambienQuedaRegistradoComoFallido() {
    when(copia.lotesAConsultar()).thenReturn(List.of(UNO));
    when(aplicador.aplicar(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalStateException("base caída"));

    var resumen = servicio.sincronizar();

    assertThat(resumen.exitosa()).isFalse();
    assertThat(resumen.error()).contains("IllegalStateException").contains("base caída");
    verify(bitacora).fallar(eq(8L), any(), eq(AHORA));
  }
}
