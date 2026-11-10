package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.model.EtapaLote;
import co.edu.unimagdalena.avicontrol.domain.model.LoteListado;
import co.edu.unimagdalena.avicontrol.domain.port.out.LoteRepository;
import co.edu.unimagdalena.avicontrol.domain.shared.Pagina;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

/**
 * Lista de lotes con la etapa derivada en la consulta (plan 002): un {@code CASE} sobre la alerta
 * de vaciado sanitario, la Liquidación {@code ACTIVA} y el estado del galpón, en el orden de
 * M3-CU01.FR-013. Así el filtro por etapa, la exclusión de {@code LIQUIDADO} y el total de la
 * paginación se resuelven en la base de datos.
 */
@Repository
public class LoteJpaAdapter implements LoteRepository {

  private final LoteJpaRepository repositorio;

  public LoteJpaAdapter(LoteJpaRepository repositorio) {
    this.repositorio = repositorio;
  }

  @Override
  public Pagina<LoteListado> buscarListables(EtapaLote etapa, String search, int page, int size) {
    String codigoEtapa = etapa == null ? null : etapa.name();
    String patron = search == null ? null : "%" + escaparLike(search) + "%";

    var resultado =
        repositorio.buscarListables(codigoEtapa, search, patron, PageRequest.of(page, size));

    return Pagina.de(
        resultado.getContent().stream().map(LoteJpaAdapter::aLote).toList(),
        page,
        size,
        resultado.getTotalElements());
  }

  @Override
  public boolean existeAlgunoListable() {
    return repositorio.existeAlgunoListable();
  }

  private static LoteListado aLote(LoteFilaListado f) {
    return new LoteListado(
        f.idLote(),
        f.nombreLote(),
        f.fechaIngreso(),
        f.idGalpon(),
        f.nombreGalpon(),
        EtapaLote.valueOf(f.etapa()),
        f.poblacionActual(),
        f.tieneResultado());
  }

  /** Escapa los comodines de LIKE (con {@code !} como carácter de escape) para buscar literal. */
  static String escaparLike(String texto) {
    return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
  }
}
