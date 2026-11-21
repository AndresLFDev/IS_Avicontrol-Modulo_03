package co.edu.unimagdalena.avicontrol.application.service.sync;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaAlimentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.PartidaMedicamentoM2;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.ResultadoM2;
import co.edu.unimagdalena.avicontrol.domain.port.out.CopiaLocalM2Repository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aplica a la copia local los datos de un lote que entrega M2 (CU08 a CU10), en una sola
 * transacción. Valida antes de escribir: un dato inválido se omite y se informa, sin sobrescribir
 * lo que ya había. Un consumo de medicamento se aplica completo o no se aplica: guardar solo
 * algunos de sus tramos haría que la Liquidación cobrara de menos sin avisar. Es un bean aparte de
 * {@link SyncModulo2Service} para que la transacción abarque solo la escritura de un lote y no las
 * llamadas HTTP a M2.
 */
@Service
public class AplicarLoteM2Service {

  static final int MAXIMO_TIPO_ALIMENTO = 50;
  static final int MAXIMO_REFERENCIA = 200;
  static final int MAXIMO_MEDICAMENTO = 150;
  static final int MAXIMO_UNIDAD = 30;

  private final CopiaLocalM2Repository copia;

  public AplicarLoteM2Service(CopiaLocalM2Repository copia) {
    this.copia = copia;
  }

  /** Resultado de aplicar un lote: filas escritas y datos omitidos. */
  public record Aplicacion(int registros, List<String> incidencias) {}

  @Transactional
  public Aplicacion aplicar(
      LoteConsultableM2 lote,
      Optional<ResultadoM2> resultado,
      List<PartidaAlimentoM2> alimentos,
      List<PartidaMedicamentoM2> medicamentos,
      Instant ahora) {
    List<String> incidencias = new ArrayList<>();
    int registros = 0;
    registros += aplicarResultado(lote, resultado, ahora, incidencias);
    registros += aplicarAlimentos(lote, alimentos, ahora, incidencias);
    registros += aplicarMedicamentos(lote, medicamentos, ahora, incidencias);
    return new Aplicacion(registros, incidencias);
  }

  /** Id de la partida de alimento: el mismo par suministro-recepción siempre da el mismo id. */
  static UUID idPartidaAlimento(UUID suministroId, UUID recepcionId) {
    return UUID.nameUUIDFromBytes(
        ("alimento:" + suministroId + ":" + recepcionId).getBytes(StandardCharsets.UTF_8));
  }

  // --- CU08: resultado final de sacrificio ---

  private int aplicarResultado(
      LoteConsultableM2 lote,
      Optional<ResultadoM2> resultado,
      Instant ahora,
      List<String> incidencias) {
    if (resultado.isEmpty()) {
      return 0; // M2 aún no lo registró: no es un error
    }
    ResultadoM2 r = resultado.get();
    String error = validarResultado(lote, r);
    if (error != null) {
      incidencias.add("Resultado de sacrificio del lote " + lote.idLote() + " omitido: " + error);
      return 0;
    }
    var local = copia.resultadoDelLote(lote.idLote());
    if (local.isPresent() && !local.get().idResultado().equals(r.id())) {
      // CU08.FR-010: un segundo resultado para el mismo lote es una incidencia; se conserva el
      // primero
      incidencias.add(
          "Resultado de sacrificio del lote "
              + lote.idLote()
              + " omitido: M2 entrega el resultado "
              + r.id()
              + " pero ya hay otro sincronizado ("
              + local.get().idResultado()
              + "); se conserva el anterior");
      return 0;
    }
    if (local.isPresent() && local.get().utilizado()) {
      return 0; // ya se liquidó con él y M2 bloquea su corrección: no se toca
    }
    copia.guardarResultado(
        r.id(),
        lote.idLote(),
        lote.idGalpon(),
        r.cantidadFinalPollos(),
        r.pesoTotalKg(),
        r.fechaRegistro(),
        ahora);
    return 1;
  }

  private static String validarResultado(LoteConsultableM2 lote, ResultadoM2 r) {
    if (r.id() == null) {
      return "sin id";
    }
    if (r.loteId() != null && !r.loteId().equals(lote.idLote())) {
      return "pertenece a otro lote (" + r.loteId() + ")";
    }
    if (r.cantidadFinalPollos() == null || r.cantidadFinalPollos() <= 0) {
      return "cantidad final de pollos inválida";
    }
    if (r.pesoTotalKg() == null || r.pesoTotalKg().signum() <= 0) {
      return "peso total inválido";
    }
    if (r.fechaRegistro() == null) {
      return "sin fecha de registro";
    }
    return null;
  }

  // --- CU09: alimento ---

  private int aplicarAlimentos(
      LoteConsultableM2 lote,
      List<PartidaAlimentoM2> alimentos,
      Instant ahora,
      List<String> incidencias) {
    int registros = 0;
    Set<UUID> vistas = new HashSet<>();
    for (PartidaAlimentoM2 p : alimentos) {
      String error = validarAlimento(p);
      if (error != null) {
        incidencias.add("Partida de alimento " + etiqueta(p) + " omitida: " + error);
        continue;
      }
      UUID id = idPartidaAlimento(p.suministroId(), p.recepcionId());
      if (!vistas.add(id)) {
        incidencias.add(
            "Partida de alimento " + etiqueta(p) + " omitida: repetida en la respuesta");
        continue;
      }
      String tipo = !esVacio(p.tipoAlimento()) ? p.tipoAlimento() : p.nombreAlimento();
      String referencia =
          "recepción "
              + p.recepcionId()
              + (esVacio(p.nombreAlimento()) ? "" : " " + p.nombreAlimento());
      copia.guardarPartidaAlimento(
          id,
          lote.idLote(),
          lote.idGalpon(),
          recortar(tipo.strip(), MAXIMO_TIPO_ALIMENTO),
          p.cantidadKg(),
          p.precioNetoKg(),
          p.valorImpuestoCop(),
          recortar(referencia, MAXIMO_REFERENCIA),
          ahora);
      registros++;
    }
    return registros;
  }

  private static String validarAlimento(PartidaAlimentoM2 p) {
    if (p == null || p.suministroId() == null || p.recepcionId() == null) {
      return "sin suministroId o recepcionId";
    }
    if (p.cantidadKg() == null || p.cantidadKg().signum() <= 0) {
      return "cantidad inválida";
    }
    if (p.precioNetoKg() != null && p.precioNetoKg().signum() < 0) {
      return "precio negativo";
    }
    if (p.valorImpuestoCop() != null && p.valorImpuestoCop().signum() < 0) {
      return "impuesto negativo";
    }
    if (esVacio(p.tipoAlimento()) && esVacio(p.nombreAlimento())) {
      return "sin tipo ni nombre de alimento";
    }
    return null;
  }

  private static String etiqueta(PartidaAlimentoM2 p) {
    return p == null ? "(vacía)" : "(" + p.suministroId() + ", " + p.recepcionId() + ")";
  }

  // --- CU10: medicamento ---

  private int aplicarMedicamentos(
      LoteConsultableM2 lote,
      List<PartidaMedicamentoM2> medicamentos,
      Instant ahora,
      List<String> incidencias) {
    // Un consumo puede abarcar varias recepciones: llega como un elemento por recepción
    var porConsumo = new LinkedHashMap<UUID, List<PartidaMedicamentoM2>>();
    for (PartidaMedicamentoM2 p : medicamentos) {
      if (p == null || p.consumoId() == null) {
        incidencias.add("Tramo de medicamento omitido: sin consumoId");
        continue;
      }
      porConsumo.computeIfAbsent(p.consumoId(), k -> new ArrayList<>()).add(p);
    }
    int registros = 0;
    for (var entrada : porConsumo.entrySet()) {
      String error = validarConsumo(entrada.getValue());
      if (error != null) {
        incidencias.add("Consumo de medicamento " + entrada.getKey() + " omitido: " + error);
        continue;
      }
      registros += aplicarConsumo(lote, entrada.getKey(), entrada.getValue(), ahora);
    }
    return registros;
  }

  private int aplicarConsumo(
      LoteConsultableM2 lote, UUID idConsumo, List<PartidaMedicamentoM2> tramos, Instant ahora) {
    PartidaMedicamentoM2 primero = tramos.get(0);
    BigDecimal total =
        tramos.stream()
            .map(PartidaMedicamentoM2::cantidadUnidadBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    String nombre =
        !esVacio(primero.nombreMedicamento())
            ? primero.nombreMedicamento().strip()
            : "Medicamento " + primero.medicamentoId();
    copia.guardarConsumoMedicamento(
        idConsumo,
        lote.idLote(),
        lote.idGalpon(),
        recortar(nombre, MAXIMO_MEDICAMENTO),
        primero.fechaAplicacion(),
        total,
        recortar(primero.unidadBase().strip(), MAXIMO_UNIDAD),
        ahora);
    for (PartidaMedicamentoM2 t : tramos) {
      copia.guardarTramoMedicamento(
          idConsumo,
          t.recepcionId().toString(),
          t.cantidadUnidadBase(),
          t.precioNetoUnidadBase(),
          t.valorImpuestoCop());
    }
    return 1 + tramos.size();
  }

  private static String validarConsumo(List<PartidaMedicamentoM2> tramos) {
    PartidaMedicamentoM2 primero = tramos.get(0);
    if (primero.fechaAplicacion() == null) {
      return "sin fecha de aplicación";
    }
    if (esVacio(primero.unidadBase())) {
      return "sin unidad base";
    }
    if (esVacio(primero.nombreMedicamento()) && primero.medicamentoId() == null) {
      return "sin nombre ni id de medicamento";
    }
    Set<UUID> recepciones = new HashSet<>();
    for (PartidaMedicamentoM2 t : tramos) {
      if (t.recepcionId() == null) {
        return "un tramo sin recepcionId";
      }
      if (!recepciones.add(t.recepcionId())) {
        return "recepción repetida (" + t.recepcionId() + ")";
      }
      if (t.cantidadUnidadBase() == null || t.cantidadUnidadBase().signum() <= 0) {
        return "cantidad inválida en la recepción " + t.recepcionId();
      }
      if (t.precioNetoUnidadBase() != null && t.precioNetoUnidadBase().signum() < 0) {
        return "precio negativo en la recepción " + t.recepcionId();
      }
      if (t.valorImpuestoCop() != null && t.valorImpuestoCop().signum() < 0) {
        return "impuesto negativo en la recepción " + t.recepcionId();
      }
      if (t.unidadBase() == null || !t.unidadBase().strip().equals(primero.unidadBase().strip())) {
        return "unidades base distintas entre sus tramos";
      }
    }
    return null;
  }

  private static boolean esVacio(String texto) {
    return texto == null || texto.isBlank();
  }

  private static String recortar(String texto, int maximo) {
    return texto.length() <= maximo ? texto : texto.substring(0, maximo);
  }
}
