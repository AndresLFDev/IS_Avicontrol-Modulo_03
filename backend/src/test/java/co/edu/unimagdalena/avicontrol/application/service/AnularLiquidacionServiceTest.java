package co.edu.unimagdalena.avicontrol.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaAnuladaException;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Plan 003, T014: la anulación cambia el estado, deja auditoría y no se repite. */
class AnularLiquidacionServiceTest {

  private static final Instant AHORA = Instant.parse("2026-11-06T15:00:00Z");
  private static final String MOTIVO = "Error en el precio por kg";

  private final LiquidacionRepository repositorio = mock(LiquidacionRepository.class);
  private final AnularLiquidacionService servicio =
      new AnularLiquidacionService(repositorio, Clock.fixed(AHORA, ZoneOffset.UTC));

  @Test
  void anulaUnaActivaConMotivoUsuarioYFecha() {
    var registro = new RegistroAnulacion(7L, MOTIVO, AHORA, "Ana");
    when(repositorio.bloquearParaAnular(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ACTIVA, null)));
    when(repositorio.anular(42L, MOTIVO, "Ana", AHORA)).thenReturn(registro);

    var dto = servicio.anularLiquidacion(42L, MOTIVO, "Ana");

    assertThat(dto.idLiquidacion()).isEqualTo(42L);
    assertThat(dto.estado()).isEqualTo("ANULADA");
    assertThat(dto.registroAnulacion()).isEqualTo(registro);
  }

  @Test
  void rechazaUnaAnuladaConSuRegistroExistente() {
    var existente = new RegistroAnulacion(7L, MOTIVO, AHORA, "Ana");
    when(repositorio.bloquearParaAnular(42L))
        .thenReturn(Optional.of(liquidacion(EstadoLiquidacion.ANULADA, existente)));

    assertThatThrownBy(() -> servicio.anularLiquidacion(42L, "Otro motivo cualquiera", "Luis"))
        .isInstanceOfSatisfying(
            LiquidacionYaAnuladaException.class,
            e -> {
              assertThat(e).hasMessage("La Liquidación 42 ya fue anulada.");
              assertThat(e.getRegistroAnulacion()).isEqualTo(existente);
            });
    verify(repositorio, never()).anular(anyLong(), any(), any(), any());
  }

  @Test
  void unaInexistenteResponde404() {
    when(repositorio.bloquearParaAnular(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> servicio.anularLiquidacion(999L, MOTIVO, "Ana"))
        .isInstanceOf(LiquidacionNoEncontradaException.class);
    verify(repositorio, never()).anular(anyLong(), any(), any(), any());
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
        AHORA,
        "Jorge",
        AHORA,
        AHORA,
        anulacion);
  }
}
