package co.edu.unimagdalena.avicontrol.application.service;

import co.edu.unimagdalena.avicontrol.domain.exception.RangoFechasInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarHistorialUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.HistorialPageDto;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** M3-CU06: consulta sobre las Liquidaciones almacenadas en M3, sin consultar a M1 ni a M2. */
@Service
public class ConsultarHistorialService implements ConsultarHistorialUseCase {

  static final ZoneId ZONA_FILTROS = ZoneId.of("America/Bogota"); // arquitectura general
  static final String SIN_REGISTROS = "No hay liquidaciones registradas aún"; // FR-007
  static final String SIN_COINCIDENCIAS =
      "No se encontraron liquidaciones para los criterios de búsqueda aplicados"; // FR-007

  private final LiquidacionRepository liquidaciones;

  public ConsultarHistorialService(LiquidacionRepository liquidaciones) {
    this.liquidaciones = liquidaciones;
  }

  @Override
  @Transactional(readOnly = true)
  public HistorialPageDto consultarHistorial(
      String search, LocalDate desde, LocalDate hasta, int page, int size) {
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      throw new RangoFechasInvalidoException(desde, hasta);
    }
    String termino = search == null || search.isBlank() ? null : search.strip();
    Instant inicio = desde == null ? null : inicioDelDia(desde);
    Instant finExclusivo = hasta == null ? null : inicioDelDia(hasta.plusDays(1));

    var resultado = liquidaciones.buscarHistorial(termino, inicio, finExclusivo, page, size);

    String mensaje = null;
    if (resultado.contenido().isEmpty()) {
      boolean sinFiltros = termino == null && desde == null && hasta == null;
      mensaje = sinFiltros && resultado.totalElements() == 0 ? SIN_REGISTROS : SIN_COINCIDENCIAS;
    }
    return new HistorialPageDto(
        resultado.contenido(),
        resultado.page(),
        resultado.size(),
        resultado.totalElements(),
        resultado.totalPages(),
        mensaje);
  }

  private static Instant inicioDelDia(LocalDate fecha) {
    return fecha.atStartOfDay(ZONA_FILTROS).toInstant();
  }
}
