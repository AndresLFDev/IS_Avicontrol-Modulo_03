package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoEncontradoException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoLiquidableException;
import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.exception.VistaPreviaDesactualizadaException;
import co.edu.unimagdalena.avicontrol.domain.model.CalculoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.CausaNoLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.DatosLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.GenerarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PreviewLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionPreviaDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalLiquidacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * M3-CU03 en dos pasos (FR-015): la vista previa calcula sin persistir y la confirmación vuelve a
 * validar, compara la huella y persiste exactamente lo mostrado.
 */
@Service
public class GenerarLiquidacionService
    implements PreviewLiquidacionUseCase, GenerarLiquidacionUseCase {

  static final String AVISO_VISTA_PREVIA = "Vista previa · sin generar";

  private final CopiaLocalLiquidacionRepository copiaLocal;
  private final LiquidacionRepository liquidaciones;
  private final AvisoUtilizacionRepository avisos;
  private final ConsultarLiquidacionUseCase consultar;
  private final Clock clock;

  public GenerarLiquidacionService(
      CopiaLocalLiquidacionRepository copiaLocal,
      LiquidacionRepository liquidaciones,
      AvisoUtilizacionRepository avisos,
      ConsultarLiquidacionUseCase consultar,
      Clock clock) {
    this.copiaLocal = copiaLocal;
    this.liquidaciones = liquidaciones;
    this.avisos = avisos;
    this.consultar = consultar;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public LiquidacionPreviaDto calcularVistaPrevia(GenerarLiquidacionCommand comando) {
    var datos = cargarYValidar(comando);
    var calculo = CalculoLiquidacion.de(datos, comando.precioKgCop());
    var lote = datos.lote();
    return new LiquidacionPreviaDto(
        true,
        AVISO_VISTA_PREVIA,
        lote.idLote(),
        lote.idGalpon(),
        lote.nombreLote(),
        lote.nombreGalpon(),
        datos.siniestroTotal(),
        calculo.precioKgCop(),
        calculo.pollosVendidos(),
        calculo.pesoTotalKg(),
        calculo.pesoPromedioKg(),
        calculo.ventaBrutaCop(),
        calculo.mortalidadAves(),
        calculo.porcentajeMortalidad(),
        calculo.subtotales(),
        calculo.costosOperativosCop(),
        calculo.utilidadNetaCop(),
        datos.fechaSincronizacionM1(),
        datos.fechaSincronizacionM2(),
        datos.huella(comando.precioKgCop()));
  }

  @Override
  @Transactional
  public LiquidacionDto generarLiquidacion(GenerarLiquidacionCommand comando) {
    var datos = cargarYValidar(comando); // FR-015: las validaciones se repiten al confirmar
    if (!datos.huella(comando.precioKgCop()).equals(comando.huellaDatos())) {
      throw new VistaPreviaDesactualizadaException();
    }
    var calculo = CalculoLiquidacion.de(datos, comando.precioKgCop());
    var ahora = clock.instant();
    long id = liquidaciones.guardarActiva(datos, calculo, comando.usuario(), ahora);
    // FR-010: el resultado consumido queda UTILIZADO y su aviso a M2 sale en la misma transacción
    datos
        .resultadoSacrificio()
        .ifPresent(r -> avisos.registrarUtilizacion(r.idResultado(), id, ahora));
    return consultar.consultarLiquidacion(id);
  }

  /** Precondiciones 2 a 8 del plan 003, en su orden. La 1 la cubre Bean Validation. */
  private DatosLiquidacion cargarYValidar(GenerarLiquidacionCommand comando) {
    var idLote = comando.idLote();
    var lote =
        copiaLocal.buscarLote(idLote).orElseThrow(() -> new LoteNoEncontradoException(idLote));
    validarPrecio(lote.siniestroTotal(), comando);
    liquidaciones
        .buscarIdActivaPorLote(idLote)
        .ifPresent(
            activa -> {
              throw new LiquidacionYaExistenteException(idLote, activa);
            });
    if (!copiaLocal.tieneAlertaVaciado(idLote)) {
      throw new LoteNoLiquidableException(CausaNoLiquidable.SIN_ALERTA_VACIADO);
    }
    var resultado = copiaLocal.buscarResultadoSacrificio(idLote);
    if (resultado.isEmpty() && !lote.siniestroTotal()) {
      throw new LoteNoLiquidableException(CausaNoLiquidable.SIN_RESULTADO_SACRIFICIO);
    }
    var alimento = copiaLocal.buscarPartidasAlimento(idLote);
    if (alimento.isEmpty()) {
      throw new LoteNoLiquidableException(CausaNoLiquidable.SIN_PARTIDAS_ALIMENTO);
    }
    var medicamentos = copiaLocal.buscarTramosMedicamento(idLote);
    var pendientes = DatosLiquidacion.partidasPendientes(alimento, medicamentos);
    if (!pendientes.isEmpty()) {
      throw new LoteNoLiquidableException(CausaNoLiquidable.PARTIDAS_SIN_PRECIO, pendientes);
    }
    // En siniestro total no se usa el resultado aunque exista: no hubo venta
    return new DatosLiquidacion(
        lote, lote.siniestroTotal() ? null : resultado.get(), alimento, medicamentos);
  }

  /** Precondición 3 (FR-011): el precio depende de si el lote es siniestro total. */
  private static void validarPrecio(boolean siniestroTotal, GenerarLiquidacionCommand comando) {
    if (siniestroTotal && comando.precioKgCop() != null) {
      throw new ParametroInvalidoException(
          "precioKgCop", "El lote es un siniestro total: no se ingresa precio por kg.");
    }
    if (!siniestroTotal && comando.precioKgCop() == null) {
      throw new ParametroInvalidoException("precioKgCop", "El precio por kg es obligatorio.");
    }
  }
}
