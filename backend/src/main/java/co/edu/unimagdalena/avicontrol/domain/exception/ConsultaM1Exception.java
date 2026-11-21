package co.edu.unimagdalena.avicontrol.domain.exception;

/**
 * Módulo 1 no respondió, agotó el tiempo o respondió con un error. La sincronización queda {@code
 * FALLIDA} y la copia local no se modifica; el siguiente ciclo reintenta.
 */
public class ConsultaM1Exception extends RuntimeException {

  public ConsultaM1Exception(String mensaje) {
    super(mensaje);
  }

  public ConsultaM1Exception(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
