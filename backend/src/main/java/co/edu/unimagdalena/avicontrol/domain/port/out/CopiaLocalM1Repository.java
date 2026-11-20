package co.edu.unimagdalena.avicontrol.domain.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Escritura de la copia local de Módulo 1 (galpones, lotes y alertas de vaciado derivadas). */
public interface CopiaLocalM1Repository {

  /** Inserta o actualiza el galpón por su id. */
  void guardarGalpon(UUID idGalpon, String nombre, int aforoMaximo, String estado, Instant ahora);

  /** Inserta o actualiza el lote por su id; el galpón debe existir. */
  void guardarLote(
      UUID idLote,
      UUID idGalpon,
      String nombre,
      LocalDate fechaIngreso,
      int poblacionInicial,
      int poblacionActual,
      long costoTotalCop,
      String estado,
      Instant ahora);

  /**
   * Crea la alerta de vaciado del lote si no existe (son inmutables).
   *
   * @return verdadero si la creó, falso si ya existía
   */
  boolean crearAlertaVaciadoSiNoExiste(
      UUID idAlerta, UUID idGalpon, UUID idLote, Instant fechaHoraEvento, long idRegistro);
}
