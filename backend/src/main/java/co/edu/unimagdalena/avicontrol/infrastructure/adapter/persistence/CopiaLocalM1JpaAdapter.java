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
    var galpon = galpones.findById(idGalpon).orElseGet(GalponEntity::new);
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
    var lote = lotes.findById(idLote).orElseGet(LoteEntity::new);
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
