package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM2Exception;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo2UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarFinanzasM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * CU08 a CU10: consulta a M2, lote por lote, el resultado final, el alimento y el medicamento, y
 * mantiene la copia local. Solo consulta los lotes que ya tienen alerta de vaciado y no tienen
 * Liquidación activa: son los únicos que M3 puede liquidar. Como con M1, no es transaccional como
 * un todo (hace llamadas HTTP): cada lote se escribe en su propia transacción y una ejecución que
 * falla a medias deja intacta la copia de los lotes restantes.
 */
@Service
public class SyncModulo2Service implements SincronizarModulo2UseCase {

  static final String RUTA_BITACORA = "GET /api/integracion/modulo3/lotes/{loteId}";

  private final ConsultarFinanzasM2 m2;
  private final CopiaLocalM2Repository copia;
  private final AplicarLoteM2Service aplicador;
  private final RegistroSincronizacionRepository bitacora;
  private final Clock clock;

  public SyncModulo2Service(
      ConsultarFinanzasM2 m2,
      CopiaLocalM2Repository copia,
      AplicarLoteM2Service aplicador,
      RegistroSincronizacionRepository bitacora,
      Clock clock) {
    this.m2 = m2;
    this.copia = copia;
    this.aplicador = aplicador;
    this.bitacora = bitacora;
    this.clock = clock;
  }

  @Override
  public ResumenSincronizacion sincronizar() {
    long idRegistro =
        bitacora.iniciar(FuenteSincronizacion.MODULO_2, RUTA_BITACORA, clock.instant());
    int registros = 0;
    List<String> incidencias = new ArrayList<>();
    try {
      for (LoteConsultableM2 lote : copia.lotesAConsultar()) {
        var resultado = m2.resultado(lote.idLote());
        var alimentos = m2.alimentos(lote.idLote());
        var medicamentos = m2.medicamentos(lote.idLote());
        var aplicado = aplicador.aplicar(lote, resultado, alimentos, medicamentos, clock.instant());
        registros += aplicado.registros();
        incidencias.addAll(aplicado.incidencias());
      }
    } catch (ConsultaM2Exception e) {
      bitacora.fallar(idRegistro, e.getMessage(), clock.instant());
      return new ResumenSincronizacion(false, registros, incidencias, e.getMessage());
    } catch (RuntimeException e) {
      // Error inesperado (base de datos, datos imposibles de leer): también queda registrado
      String causa = e.getClass().getSimpleName() + ": " + e.getMessage();
      bitacora.fallar(idRegistro, causa, clock.instant());
      return new ResumenSincronizacion(false, registros, incidencias, causa);
    }
    String nota = incidencias.isEmpty() ? null : String.join("; ", incidencias);
    bitacora.completar(idRegistro, registros, nota, clock.instant());
    return new ResumenSincronizacion(true, registros, incidencias, null);
  }
}
