package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaCosto;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarDesgloseUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.CategoriaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.DesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.IngresosDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.PartidaDesgloseDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** M3-CU05: arma el desglose solo con lo almacenado en M3, sin consultar a M1 ni a M2. */
@Service
public class ConsultarDesgloseService implements ConsultarDesgloseUseCase {

  private final LiquidacionRepository liquidaciones;

  public ConsultarDesgloseService(LiquidacionRepository liquidaciones) {
    this.liquidaciones = liquidaciones;
  }

  @Override
  @Transactional(readOnly = true)
  public DesgloseDto consultarDesglose(long idLiquidacion) {
    var liquidacion =
        liquidaciones
            .buscarPorId(idLiquidacion)
            .orElseThrow(() -> new LiquidacionNoEncontradaException(idLiquidacion));
    var partidas = liquidaciones.buscarPartidas(idLiquidacion);

    return new DesgloseDto(
        liquidacion.idLiquidacion(),
        liquidacion.estado().name(),
        liquidacion.idLote(),
        liquidacion.nombreLote(),
        liquidacion.nombreGalpon(),
        liquidacion.siniestroTotal(),
        new IngresosDto(
            liquidacion.pollosVendidos(),
            liquidacion.pesoTotalKg(),
            liquidacion.pesoPromedioKg(),
            liquidacion.precioKgCop(),
            liquidacion.ventaBrutaCop()),
        Arrays.stream(CategoriaCosto.values())
            .map(categoria -> agrupar(categoria, partidas, liquidacion))
            .toList(),
        liquidacion.mortalidadAves(),
        liquidacion.porcentajeMortalidad(),
        liquidacion.costosOperativosCop(),
        liquidacion.utilidadNetaCop(),
        liquidacion.registroAnulacion(),
        liquidacion.estado() == EstadoLiquidacion.ACTIVA);
  }

  private static CategoriaDesgloseDto agrupar(
      CategoriaCosto categoria, List<PartidaCosto> partidas, Liquidacion liquidacion) {
    var filas =
        partidas.stream()
            .filter(partida -> partida.categoria() == categoria)
            .map(partida -> aFila(partida, liquidacion))
            .toList();
    long subtotal = filas.stream().mapToLong(PartidaDesgloseDto::subtotalCop).sum();
    return new CategoriaDesgloseDto(categoria.name(), etiqueta(categoria), subtotal, filas);
  }

  private static PartidaDesgloseDto aFila(PartidaCosto partida, Liquidacion liquidacion) {
    return new PartidaDesgloseDto(
        partida.concepto(),
        partida.cantidad(),
        partida.unidadMedida(),
        partida.precioUnitarioCop(),
        partida.subtotalCop(),
        partida.fuenteOrigen().name(),
        // El costo de población no tiene documento propio: su fuente es el lote de Módulo 1
        partida.referenciaOrigen() != null
            ? partida.referenciaOrigen()
            : liquidacion.idLote().toString());
  }

  private static String etiqueta(CategoriaCosto categoria) {
    return switch (categoria) {
      case ALIMENTO -> "Alimento";
      case MEDICINA -> "Insumos Médicos";
      case POBLACION -> "Costo de Población";
    };
  }
}
