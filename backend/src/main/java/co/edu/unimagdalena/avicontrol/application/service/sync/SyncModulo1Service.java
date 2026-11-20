package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.exception.ConsultaM1Exception;
import co.edu.unimagdalena.avicontrol.domain.model.FuenteSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.in.SincronizarModulo1UseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.GalponM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.HistorialLotesM1;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResumenSincronizacion;
import co.edu.unimagdalena.avicontrol.domain.port.out.ConsultarGalponesM1;
import co.edu.unimagdalena.avicontrol.domain.port.out.RegistroSincronizacionRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * CU07: consulta a M1 y mantiene la copia local de galpones, lotes y alertas de vaciado. No es
 * transaccional como un todo (hace llamadas HTTP): cada galpón se escribe en su propia transacción
 * y una ejecución que falla a medias deja la copia como estaba para los galpones restantes.
 */
@Service
public class SyncModulo1Service implements SincronizarModulo1UseCase {

  static final String RUTA_BITACORA = "GET /api/v1/galpones";

  private final ConsultarGalponesM1 m1;
  private final AplicarGalponM1Service aplicador;
  private final RegistroSincronizacionRepository bitacora;
  private final Clock clock;

  public SyncModulo1Service(
      ConsultarGalponesM1 m1,
      AplicarGalponM1Service aplicador,
      RegistroSincronizacionRepository bitacora,
      Clock clock) {
    this.m1 = m1;
    this.aplicador = aplicador;
    this.bitacora = bitacora;
    this.clock = clock;
  }

  @Override
  public ResumenSincronizacion sincronizar() {
    long idRegistro =
        bitacora.iniciar(FuenteSincronizacion.MODULO_1, RUTA_BITACORA, clock.instant());
    int registros = 0;
    List<String> incidencias = new ArrayList<>();
    try {
      for (GalponM1 resumen : m1.listarGalpones()) {
        Optional<GalponM1> detalle = m1.detalle(resumen.id());
        if (detalle.isEmpty()) {
          continue; // M1 ya no lo conoce: la copia local conserva lo que tenía
        }
        HistorialLotesM1 historial =
            m1.historial(resumen.id()).orElseGet(() -> HistorialLotesM1.vacio(resumen.id()));
        var aplicado = aplicador.aplicar(detalle.get(), historial, idRegistro, clock.instant());
        registros += aplicado.registros();
        incidencias.addAll(aplicado.incidencias());
      }
    } catch (ConsultaM1Exception e) {
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
