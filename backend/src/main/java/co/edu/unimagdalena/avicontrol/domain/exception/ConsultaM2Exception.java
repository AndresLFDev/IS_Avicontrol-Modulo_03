package co.edu.unimagdalena.avicontrol.domain.exception;

/**
 * Módulo 2 no respondió, agotó el tiempo o respondió con un error (consulta o confirmación de
 * utilización). La sincronización queda {@code FALLIDA} y el aviso sigue pendiente; el siguiente
 * ciclo reintenta.
 */
public class ConsultaM2Exception extends RuntimeException {

  public ConsultaM2Exception(String mensaje) {
    super(mensaje);
  }

  public ConsultaM2Exception(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
