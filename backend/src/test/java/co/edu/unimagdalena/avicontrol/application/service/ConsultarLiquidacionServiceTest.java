package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plan 003, T011: la Liquidación sale congelada y sus acciones dependen del estado. */
class ConsultarLiquidacionServiceTest {

  private final LiquidacionRepository repositorio = mock(LiquidacionRepository.class);
  private final ConsultarLiquidacionService servicio = new ConsultarLiquidacionService(repositorio);

  @Test
  void unaActivaOfreceDesgloseYAnulacion() {
    when(repositorio.buscarPorId(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null)));

    var dto = servicio.consultarLiquidacion(42L);

    assertThat(dto.estado()).isEqualTo("ACTIVA");
    assertThat(dto.accionesDisponibles()).containsExactly("VER_DESGLOSE", "ANULAR");
    assertThat(dto.registroAnulacion()).isNull();
    assertThat(dto.ventaBrutaCop()).isEqualTo(107_100_000);
    assertThat(dto.siniestroTotal()).isFalse();
  }

  @Test
  void unaAnuladaMuestraSuRegistroYSoloElDesglose() {
    var registro =
        new RegistroAnulacion(
            7L, "Error en el precio", Instant.parse("2026-10-30T12:00:00Z"), "Ana");
    when(repositorio.buscarPorId(40L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ANULADA, registro)));

    var dto = servicio.consultarLiquidacion(40L);

    assertThat(dto.accionesDisponibles()).containsExactly("VER_DESGLOSE");
    assertThat(dto.registroAnulacion()).isEqualTo(registro);
  }

  @Test
  void unaInexistenteResponde404() {
    when(repositorio.buscarPorId(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.consultarLiquidacion(999L))
        .isInstanceOf(LiquidacionNoEncontradaException.class)
        .hasMessage("No se encontró la liquidación con ID 999.");
  }

  private static Liquidacion liquidacion(EstadoLiquidacion estado, RegistroAnulacion anulacion) {
    return new Liquidacion(
        42L,
        estado,
        UUID.randomUUID(),
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        UUID.randomUUID(),
        8500,
        new BigDecimal("23800.00"),
        new BigDecimal("2.80"),
        new BigDecimal("4500.00"),
        107_100_000,
        500,
        new BigDecimal("5.56"),
        new SubtotalesCategoria(64_800_000, 2_200_000, 18_000_000),
        85_000_000,
        22_100_000,
        Instant.parse("2026-10-29T15:00:00Z"),
        "Jorge Meléndez",
        Instant.parse("2026-10-20T13:00:01Z"),
        Instant.parse("2026-10-20T14:20:01Z"),
        anulacion);
  }
}
