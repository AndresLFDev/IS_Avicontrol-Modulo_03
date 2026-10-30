package co.edu.unimagdalena.avicontrol.domain.exception;

/**
 * Parámetro de un caso de uso que viola una regla de negocio; el API lo responde como {@code 400
 * validacion}.
 */
public class ParametroInvalidoException extends RuntimeException {

  private final String campo;

  public ParametroInvalidoException(String campo, String mensaje) {
    super(mensaje);
    this.campo = campo;
  }

  public String getCampo() {
    return campo;
  }
}
