package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarEstadoSincronizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ListarLotesUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LotePageDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LoteResumenDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** M3-CU01 — Lista de lotes sin Liquidación {@code ACTIVA} (plan 002). */
@Service
public class ListarLotesService implements ListarLotesUseCase {

  static final int TAMANO_MAXIMO = 6; // FR-011
  static final String ACCION = "GENERAR_LIQUIDACION"; // FR-005: acción única
  static final String SIN_LOTES = "No hay lotes disponibles para consultar"; // FR-006
  static final String SIN_RESULTADOS = "No se encontraron resultados"; // FR-010

  private final LoteRepository loteRepository;
  private final ConsultarEstadoSincronizacionUseCase estadoSincronizacion;

  public ListarLotesService(
      LoteRepository loteRepository, ConsultarEstadoSincronizacionUseCase estadoSincronizacion) {
    this.loteRepository = loteRepository;
    this.estadoSincronizacion = estadoSincronizacion;
  }

  @Override
  @Transactional(readOnly = true)
  public LotePageDto listarLotes(EtapaLote etapa, String search, int page, int size) {
    if (etapa == EtapaLote.LIQUIDADO) {
      throw new ParametroInvalidoException(
          "etapa", "La etapa LIQUIDADO no se lista; consulte el historial de liquidaciones.");
    }
    String termino = search == null || search.isBlank() ? null : search.strip();
    int pageSize = Math.min(size, TAMANO_MAXIMO);

    var resultado = loteRepository.buscarListables(etapa, termino, page, pageSize);
    var filas = resultado.contenido().stream().map(ListarLotesService::aFila).toList();

    String mensaje;
    if (!filas.isEmpty()) {
      mensaje = filas.size() + " de " + resultado.totalElements() + " lotes"; // FR-012
    } else if (etapa == null && termino == null && !loteRepository.existeAlgunoListable()) {
      mensaje = SIN_LOTES;
    } else {
      mensaje = SIN_RESULTADOS; // filtro sin coincidencias o página fuera de rango
    }
    return new LotePageDto(
        filas,
        page,
        pageSize,
        resultado.totalElements(),
        resultado.totalPages(),
        mensaje,
        estadoSincronizacion.consultar());
  }

  private static LoteResumenDto aFila(LoteListado lote) {
    boolean siniestro = lote.poblacionActual() == 0;
    boolean habilitada =
        lote.etapa() == EtapaLote.POR_LIQUIDAR && (lote.tieneResultadoSacrificio() || siniestro);
    return new LoteResumenDto(
        lote.idLote(),
        lote.nombreLote(),
        lote.fechaIngreso(),
        lote.idGalpon(),
        lote.nombreGalpon(),
        lote.etapa().name(),
        siniestro,
        ACCION,
        habilitada);
  }
}
