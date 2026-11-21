package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.port.in.PublicarAvisosUtilizacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.AvisoPendiente;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenAvisos;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConfirmarUtilizacionM2;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Entrega a M2 los avisos de utilización pendientes (outbox). Un aviso solo pasa a entregado cuando
 * M2 responde 200; cualquier otra cosa lo deja pendiente y se reintenta con el mismo {@code
 * eventId}, porque M2 puede haber confirmado y perdido la respuesta. No es transaccional como un
 * todo: cada cambio de estado se guarda por separado, después de la llamada HTTP.
 */
@Service
public class PublicarAvisosM2Service implements PublicarAvisosUtilizacionUseCase {

  private final AvisoUtilizacionRepository avisos;
  private final ConfirmarUtilizacionM2 m2;

  public PublicarAvisosM2Service(AvisoUtilizacionRepository avisos, ConfirmarUtilizacionM2 m2) {
    this.avisos = avisos;
    this.m2 = m2;
  }

  /** Id del evento: el mismo resultado siempre produce el mismo id, también tras un reinicio. */
  static UUID idEvento(UUID idResultado) {
    return UUID.nameUUIDFromBytes(
        ("aviso-resultado-utilizado:" + idResultado).getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public ResumenAvisos publicar() {
    int entregados = 0;
    List<String> errores = new ArrayList<>();
    for (AvisoPendiente aviso : avisos.pendientes()) {
      try {
        m2.confirmar(idEvento(aviso.idResultado()), aviso.idResultado(), aviso.idLote());
        avisos.marcarEntregado(aviso.idAviso());
        entregados++;
      } catch (RuntimeException e) {
        // Sin respuesta de M2, respuesta de error o fallo al guardar: sigue pendiente
        errores.add("Aviso del resultado " + aviso.idResultado() + ": " + e.getMessage());
        registrarIntento(aviso);
      }
    }
    return new ResumenAvisos(entregados, errores.size(), errores);
  }

  private void registrarIntento(AvisoPendiente aviso) {
    try {
      avisos.registrarIntentoFallido(aviso.idAviso());
    } catch (RuntimeException e) {
      // El contador es solo informativo: no debe impedir procesar los demás avisos
    }
  }
}
