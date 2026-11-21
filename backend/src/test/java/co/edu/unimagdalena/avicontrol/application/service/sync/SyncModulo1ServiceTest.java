package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM1Exception;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarGalponesM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CU07 (plan 001, T007): una ejecución de la sincronización y su bitácora. */
class SyncModulo1ServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-20T15:00:00Z");
  private static final UUID UNO = UUID.fromString("a402a368-cd4d-4e27-8d48-9e0d7eb8d374");
  private static final UUID DOS = UUID.fromString("ec8e8c65-812c-405f-8c13-58fad384111d");

  private ConsultarGalponesM1 m1;
  private AplicarGalponM1Service aplicador;
  private RegistroSincronizacionRepository bitacora;
  private SyncModulo1Service servicio;

  @BeforeEach
  void preparar() {
    m1 = mock(ConsultarGalponesM1.class);
    aplicador = mock(AplicarGalponM1Service.class);
    bitacora = mock(RegistroSincronizacionRepository.class);
    when(bitacora.iniciar(eq(FuenteSincronizacion.MODULO_1), any(), any())).thenReturn(5L);
    servicio = new SyncModulo1Service(m1, aplicador, bitacora, Clock.fixed(AHORA, ZoneOffset.UTC));
  }

  @Test
  void sincronizaCadaGalponYCierraLaBitacoraComoExitosa() {
    when(m1.listarGalpones()).thenReturn(List.of(resumen(UNO), resumen(DOS)));
    when(m1.detalle(any())).thenAnswer(i -> Optional.of(resumen(i.getArgument(0))));
    when(m1.historial(any()))
        .thenAnswer(i -> Optional.of(HistorialLotesM1.vacio(i.getArgument(0))));
    when(aplicador.aplicar(any(), any(), eq(5L), any()))
        .thenReturn(new AplicarGalponM1Service.Aplicacion(2, List.of()));

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isTrue();
    assertThat(resumenFinal.registrosActualizados()).isEqualTo(4);
    verify(bitacora).iniciar(FuenteSincronizacion.MODULO_1, "GET /api/v1/galpones", AHORA);
    verify(bitacora).completar(5L, 4, null, AHORA);
    verify(bitacora, never()).fallar(anyLong(), any(), any());
  }

  @Test
  void losDatosOmitidosQuedanEnLaNotaDeLaBitacoraSinFallarLaEjecucion() {
    when(m1.listarGalpones()).thenReturn(List.of(resumen(UNO)));
    when(m1.detalle(UNO)).thenReturn(Optional.of(resumen(UNO)));
    when(m1.historial(UNO)).thenReturn(Optional.empty());
    when(aplicador.aplicar(any(), any(), eq(5L), any()))
        .thenReturn(new AplicarGalponM1Service.Aplicacion(1, List.of("Lote x omitido: sin costo")));

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isTrue();
    assertThat(resumenFinal.incidencias()).containsExactly("Lote x omitido: sin costo");
    verify(bitacora).completar(5L, 1, "Lote x omitido: sin costo", AHORA);
  }

  @Test
  void unGalponQueM1YaNoConoceSeSaltaSinTocarLaCopiaLocal() {
    when(m1.listarGalpones()).thenReturn(List.of(resumen(UNO)));
    when(m1.detalle(UNO)).thenReturn(Optional.empty());

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isTrue();
    verify(aplicador, never()).aplicar(any(), any(), anyLong(), any());
    verify(bitacora).completar(5L, 0, null, AHORA);
  }

  @Test
  void siM1NoRespondeLaEjecucionQuedaFallidaYNoLanza() {
    when(m1.listarGalpones()).thenThrow(new ConsultaM1Exception("M1 sin respuesta"));

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isFalse();
    assertThat(resumenFinal.error()).isEqualTo("M1 sin respuesta");
    verify(bitacora).fallar(5L, "M1 sin respuesta", AHORA);
    verify(aplicador, never()).aplicar(any(), any(), anyLong(), any());
  }

  @Test
  void siM1FallaALaMitadLoYaEscritoSeConservaYLaEjecucionQuedaFallida() {
    when(m1.listarGalpones()).thenReturn(List.of(resumen(UNO), resumen(DOS)));
    when(m1.detalle(UNO)).thenReturn(Optional.of(resumen(UNO)));
    when(m1.historial(UNO)).thenReturn(Optional.of(HistorialLotesM1.vacio(UNO)));
    when(aplicador.aplicar(any(), any(), eq(5L), any()))
        .thenReturn(new AplicarGalponM1Service.Aplicacion(2, List.of()));
    when(m1.detalle(DOS)).thenThrow(new ConsultaM1Exception("tiempo agotado"));

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isFalse();
    assertThat(resumenFinal.registrosActualizados()).isEqualTo(2);
    verify(bitacora).fallar(5L, "tiempo agotado", AHORA);
  }

  @Test
  void unErrorInesperadoTambienQuedaEnLaBitacora() {
    when(m1.listarGalpones()).thenThrow(new IllegalStateException("base caída"));

    var resumenFinal = servicio.sincronizar();

    assertThat(resumenFinal.exitosa()).isFalse();
    verify(bitacora).fallar(eq(5L), eq("IllegalStateException: base caída"), eq(AHORA));
  }

  private static GalponM1 resumen(UUID id) {
    return new GalponM1(id, "galpon", 5000L, "PRODUCTIVO", null);
  }
}
