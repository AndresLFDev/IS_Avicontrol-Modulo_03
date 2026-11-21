package co.edu.unimagdalena.avicontrol.application.service.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.AvisoPendiente;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConfirmarUtilizacionM2;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** El envío a M2 de los avisos de utilización pendientes. */
class PublicarAvisosM2ServiceTest {

  private static final UUID RESULTADO_1 = UUID.randomUUID();
  private static final UUID RESULTADO_2 = UUID.randomUUID();
  private static final UUID LOTE = UUID.randomUUID();

  private AvisoUtilizacionRepository avisos;
  private ConfirmarUtilizacionM2 m2;
  private PublicarAvisosM2Service servicio;

  @BeforeEach
  void preparar() {
    avisos = mock(AvisoUtilizacionRepository.class);
    m2 = mock(ConfirmarUtilizacionM2.class);
    servicio = new PublicarAvisosM2Service(avisos, m2);
  }

  @Test
  void elIdDelEventoEsElMismoParaElMismoResultadoYDistintoParaOtro() {
    assertThat(PublicarAvisosM2Service.idEvento(RESULTADO_1))
        .isEqualTo(PublicarAvisosM2Service.idEvento(RESULTADO_1))
        .isNotEqualTo(PublicarAvisosM2Service.idEvento(RESULTADO_2));
  }

  @Test
  void entregaCadaAvisoYLoMarcaSoloDespuesDeLaConfirmacionDeM2() {
    when(avisos.pendientes())
        .thenReturn(
            List.of(
                new AvisoPendiente(1, RESULTADO_1, LOTE, 0),
                new AvisoPendiente(2, RESULTADO_2, LOTE, 3)));

    var resumen = servicio.publicar();

    assertThat(resumen.entregados()).isEqualTo(2);
    assertThat(resumen.fallidos()).isZero();
    verify(m2).confirmar(PublicarAvisosM2Service.idEvento(RESULTADO_1), RESULTADO_1, LOTE);
    verify(avisos).marcarEntregado(1);
    verify(avisos).marcarEntregado(2);
  }

  @Test
  void siM2FallaElAvisoSigueSinEntregarYSeCuentaElIntento() {
    when(avisos.pendientes()).thenReturn(List.of(new AvisoPendiente(1, RESULTADO_1, LOTE, 0)));
    doThrow(new ConsultaM2Exception("M2 sin respuesta")).when(m2).confirmar(any(), any(), any());

    var resumen = servicio.publicar();

    assertThat(resumen.entregados()).isZero();
    assertThat(resumen.fallidos()).isEqualTo(1);
    assertThat(resumen.errores().get(0)).contains(RESULTADO_1.toString());
    verify(avisos, never()).marcarEntregado(1);
    verify(avisos).registrarIntentoFallido(1);
  }

  @Test
  void unFalloNoImpideEntregarLosDemasAvisos() {
    when(avisos.pendientes())
        .thenReturn(
            List.of(
                new AvisoPendiente(1, RESULTADO_1, LOTE, 0),
                new AvisoPendiente(2, RESULTADO_2, LOTE, 0)));
    doThrow(new ConsultaM2Exception("409"))
        .when(m2)
        .confirmar(PublicarAvisosM2Service.idEvento(RESULTADO_1), RESULTADO_1, LOTE);

    var resumen = servicio.publicar();

    assertThat(resumen.entregados()).isEqualTo(1);
    assertThat(resumen.fallidos()).isEqualTo(1);
    verify(avisos).marcarEntregado(2);
  }

  @Test
  void siFallaGuardarLaEntregaElAvisoQuedaPendienteYSeReintentaConElMismoEvento() {
    when(avisos.pendientes()).thenReturn(List.of(new AvisoPendiente(1, RESULTADO_1, LOTE, 0)));
    doThrow(new IllegalStateException("base caída")).when(avisos).marcarEntregado(1);

    var resumen = servicio.publicar();

    // M2 ya confirmó, pero M3 no pudo anotarlo: se reintenta y M2 responde 200 otra vez
    assertThat(resumen.fallidos()).isEqualTo(1);
    verify(avisos).registrarIntentoFallido(1);
  }

  @Test
  void sinAvisosPendientesNoLlamaAM2() {
    when(avisos.pendientes()).thenReturn(List.of());

    var resumen = servicio.publicar();

    assertThat(resumen.entregados()).isZero();
    verify(m2, never()).confirmar(any(), any(), any());
  }
}
