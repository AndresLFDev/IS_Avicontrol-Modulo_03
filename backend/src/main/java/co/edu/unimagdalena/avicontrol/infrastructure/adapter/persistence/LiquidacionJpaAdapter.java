package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.model.CalculoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.DatosLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.EstadoLiquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.model.Liquidacion;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.ResultadoSacrificio;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import co.edu.unimagdalena.avicontrol.domain.port.out.LiquidacionRepository;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Persistencia de la Liquidación con JPA; los nombres de lote y galpón vienen de la copia local.
 */
@Repository
public class LiquidacionJpaAdapter implements LiquidacionRepository {

  private static final String UQ_LOTE_ACTIVA = "uq_lote_activa";

  private final LiquidacionJpaRepository repositorio;
  private final JdbcClient jdbc;

  public LiquidacionJpaAdapter(LiquidacionJpaRepository repositorio, JdbcClient jdbc) {
    this.repositorio = repositorio;
    this.jdbc = jdbc;
  }

  @Override
  public Optional<Long> buscarIdActivaPorLote(UUID idLote) {
    return repositorio
        .findByIdLoteAndEstado(idLote, EstadoLiquidacion.ACTIVA)
        .map(entidad -> entidad.id);
  }

  @Override
  public long guardarActiva(
      DatosLiquidacion datos, CalculoLiquidacion calculo, String usuario, Instant fechaHora) {
    var lote = datos.lote();
    var entidad = new LiquidacionEntity();
    entidad.idLote = lote.idLote();
    entidad.idGalpon = lote.idGalpon();
    entidad.idResultadoSacrificio =
        datos.resultadoSacrificio().map(ResultadoSacrificio::idResultado).orElse(null);
    entidad.pollosVendidos = calculo.pollosVendidos();
    entidad.pesoTotalKg = calculo.pesoTotalKg();
    entidad.pesoPromedioKg = calculo.pesoPromedioKg();
    entidad.precioKgCop = calculo.precioKgCop();
    entidad.ventaBrutaCop = calculo.ventaBrutaCop();
    entidad.mortalidadAves = calculo.mortalidadAves();
    entidad.porcentajeMortalidad = calculo.porcentajeMortalidad();
    entidad.costosOperativosCop = calculo.costosOperativosCop();
    entidad.utilidadNetaCop = calculo.utilidadNetaCop();
    entidad.estado = EstadoLiquidacion.ACTIVA;
    entidad.fechaHoraGeneracion = fechaHora;
    entidad.usuarioResponsable = usuario;

    for (var partida : calculo.partidas()) {
      var fila = new PartidaCostoEntity();
      fila.liquidacion = entidad;
      fila.idLote = lote.idLote();
      fila.categoria = partida.categoria();
      fila.concepto = partida.concepto();
      fila.cantidad = partida.cantidad();
      fila.unidadMedida = partida.unidadMedida();
      fila.precioUnitarioCop = partida.precioUnitarioCop();
      fila.subtotalCop = partida.subtotalCop();
      fila.fuenteOrigen = partida.fuenteOrigen();
      fila.referenciaOrigen = partida.referenciaOrigen();
      entidad.partidas.add(fila);
    }

    var m1 = snapshot(entidad, FuenteSincronizacion.MODULO_1, datos.fechaSincronizacionM1());
    m1.poblacionInicial = lote.poblacionInicial();
    m1.poblacionActual = lote.poblacionActual();
    m1.costoTotalLoteCop = lote.costoTotalCop();
    snapshot(entidad, FuenteSincronizacion.MODULO_2, datos.fechaSincronizacionM2());

    try {
      return repositorio.saveAndFlush(entidad).id;
    } catch (DataIntegrityViolationException e) {
      // Otra confirmación del mismo lote se adelantó (arquitectura general, "Concurrencia")
      if (String.valueOf(e.getMostSpecificCause().getMessage()).contains(UQ_LOTE_ACTIVA)) {
        throw new LiquidacionYaExistenteException(lote.idLote(), null);
      }
      throw e;
    }
  }

  @Override
  public Optional<Liquidacion> buscarPorId(long idLiquidacion) {
    return repositorio.findById(idLiquidacion).map(this::aDominio);
  }

  private static SnapshotDatosOrigenEntity snapshot(
      LiquidacionEntity liquidacion, FuenteSincronizacion fuente, Instant fechaHoraSync) {
    var fila = new SnapshotDatosOrigenEntity();
    fila.liquidacion = liquidacion;
    fila.idLote = liquidacion.idLote;
    fila.fuente = fuente;
    fila.fechaHoraSync = fechaHoraSync;
    liquidacion.snapshots.add(fila);
    return fila;
  }

  private Liquidacion aDominio(LiquidacionEntity e) {
    var nombres =
        jdbc.sql(
                """
                SELECT l.nombre AS nombre_lote, g.nombre AS nombre_galpon
                  FROM lote l, galpon g
                 WHERE l.id_lote = :idLote AND g.id_galpon = :idGalpon
                """)
            .param("idLote", e.idLote)
            .param("idGalpon", e.idGalpon)
            .query((rs, i) -> new String[] {rs.getString(1), rs.getString(2)})
            .single();
    var anulacion = e.registroAnulacion;
    return new Liquidacion(
        e.id,
        e.estado,
        e.idLote,
        e.idGalpon,
        nombres[0],
        nombres[1],
        e.idResultadoSacrificio,
        e.pollosVendidos,
        e.pesoTotalKg,
        // La columna guarda 4 decimales; se presenta con 2 como en la vista previa
        e.pesoPromedioKg == null ? null : e.pesoPromedioKg.setScale(2, RoundingMode.HALF_UP),
        e.precioKgCop,
        e.ventaBrutaCop,
        e.mortalidadAves,
        e.porcentajeMortalidad,
        new SubtotalesCategoria(
            subtotal(e, CategoriaCosto.ALIMENTO),
            subtotal(e, CategoriaCosto.MEDICINA),
            subtotal(e, CategoriaCosto.POBLACION)),
        e.costosOperativosCop,
        e.utilidadNetaCop,
        e.fechaHoraGeneracion,
        e.usuarioResponsable,
        fechaSync(e, FuenteSincronizacion.MODULO_1),
        fechaSync(e, FuenteSincronizacion.MODULO_2),
        anulacion == null
            ? null
            : new RegistroAnulacion(
                anulacion.id,
                anulacion.motivo,
                anulacion.fechaHoraAnulacion,
                anulacion.usuarioResponsable));
  }

  private static long subtotal(LiquidacionEntity e, CategoriaCosto categoria) {
    return e.partidas.stream()
        .filter(p -> p.categoria == categoria)
        .mapToLong(p -> p.subtotalCop)
        .sum();
  }

  private static Instant fechaSync(LiquidacionEntity e, FuenteSincronizacion fuente) {
    return e.snapshots.stream()
        .filter(s -> s.fuente == fuente)
        .map(s -> s.fechaHoraSync)
        .findFirst()
        .orElse(null);
  }
}
