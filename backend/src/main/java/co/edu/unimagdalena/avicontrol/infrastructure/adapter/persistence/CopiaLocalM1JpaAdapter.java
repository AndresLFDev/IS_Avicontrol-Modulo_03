package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM1Repository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Escritura de la copia local de M1: {@code galpon}, {@code lote} y {@code alerta_vaciado}. */
@Repository
public class CopiaLocalM1JpaAdapter implements CopiaLocalM1Repository {

  private final GalponJpaRepository galpones;
  private final LoteJpaRepository lotes;
  private final AlertaVaciadoSanitarioJpaRepository alertas;

  public CopiaLocalM1JpaAdapter(
      GalponJpaRepository galpones,
      LoteJpaRepository lotes,
      AlertaVaciadoSanitarioJpaRepository alertas) {
    this.galpones = galpones;
    this.lotes = lotes;
    this.alertas = alertas;
  }

  @Override
  @Transactional
  public void guardarGalpon(
      UUID idGalpon, String nombre, int aforoMaximo, String estado, Instant ahora) {
    var existente = galpones.findById(idGalpon);
    var galpon = existente.orElseGet(GalponEntity::new);
    if (existente.isPresent()
        && galpon.nombre.equals(nombre)
        && galpon.aforoMaximo == aforoMaximo
        && galpon.estado.equals(estado)) {
      return; // sin cambios: fecha_hora_sync solo avanza cuando los datos cambian
    }
    galpon.idGalpon = idGalpon;
    galpon.nombre = nombre;
    galpon.aforoMaximo = aforoMaximo;
    galpon.estado = estado;
    galpon.fechaHoraSync = ahora;
    galpones.save(galpon);
  }

  @Override
  @Transactional
  public void guardarLote(
      UUID idLote,
      UUID idGalpon,
      String nombre,
      LocalDate fechaIngreso,
      int poblacionInicial,
      int poblacionActual,
      long costoTotalCop,
      String estado,
      Instant ahora) {
    // Con el galpón cargado: un proxy perezoso no deja leer sus campos de paquete
    var existente = lotes.findConGalpon(idLote);
    var lote = existente.orElseGet(LoteEntity::new);
    if (existente.isPresent()
        && lote.galpon.idGalpon.equals(idGalpon)
        && lote.nombre.equals(nombre)
        && lote.fechaIngreso.equals(fechaIngreso)
        && lote.poblacionInicial == poblacionInicial
        && lote.poblacionActual == poblacionActual
        && lote.costoTotalCop == costoTotalCop
        && lote.estado.equals(estado)) {
      return; // sin cambios: fecha_hora_sync solo avanza cuando los datos cambian
    }
    lote.idLote = idLote;
    lote.galpon = galpones.getReferenceById(idGalpon);
    lote.nombre = nombre;
    lote.fechaIngreso = fechaIngreso;
    lote.poblacionInicial = poblacionInicial;
    lote.poblacionActual = poblacionActual;
    lote.costoTotalCop = costoTotalCop;
    lote.estado = estado;
    lote.fechaHoraSync = ahora;
    lotes.save(lote);
  }

  @Override
  @Transactional
  public boolean crearAlertaVaciadoSiNoExiste(
      UUID idAlerta, UUID idGalpon, UUID idLote, Instant fechaHoraEvento, long idRegistro) {
    if (alertas.existsByIdLote(idLote)) {
      return false; // las alertas son inmutables (CU07 esc. 6)
    }
    var alerta = new AlertaVaciadoSanitarioEntity();
    alerta.idAlerta = idAlerta;
    alerta.idGalpon = idGalpon;
    alerta.idLote = idLote;
    alerta.fechaHoraEvento = fechaHoraEvento;
    alerta.idRegistroSincronizacion = idRegistro;
    alertas.saveAndFlush(alerta);
    return true;
  }
}
