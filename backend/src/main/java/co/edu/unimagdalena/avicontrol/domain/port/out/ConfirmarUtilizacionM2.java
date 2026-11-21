package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import java.util.UUID;

/** Confirma a Módulo 2 que M3 utilizó un resultado de sacrificio (bloquea su corrección). */
public interface ConfirmarUtilizacionM2 {

  /**
   * Envía la confirmación. Reintentar con el mismo {@code idEvento} es seguro: M2 responde igual.
   *
   * @throws ConsultaM2Exception si M2 no respondió o no confirmó el aviso
   */
  void confirmar(UUID idEvento, UUID idResultado, UUID idLote);
}
