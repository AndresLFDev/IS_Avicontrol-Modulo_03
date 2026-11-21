package co.edu.unimagdalena.avicontrol.domain.port.out;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.LoteConsultableM2;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Escritura de la copia local de Módulo 2 (resultado, alimento y medicamento por lote). */
public interface CopiaLocalM2Repository {

  /** Resultado local del lote y si ya fue utilizado por una Liquidación. */
  record ResultadoLocal(UUID idResultado, boolean utilizado) {}

  /**
   * Lotes con alerta de vaciado y sin Liquidación ACTIVA: los únicos sobre los que M3 necesita los
   * datos de M2.
   */
  List<LoteConsultableM2> lotesAConsultar();

  Optional<ResultadoLocal> resultadoDelLote(UUID idLote);

  /**
   * Inserta o actualiza el resultado del lote (uno por lote, por su id). Conserva el estado de
   * utilización. Quien llama no lo usa con otro id si el lote ya tiene resultado (CU08.FR-010).
   */
  void guardarResultado(
      UUID idResultado,
      UUID idLote,
      UUID idGalpon,
      int cantidadFinalPollos,
      BigDecimal pesoTotalKg,
      Instant fechaRegistro,
      Instant ahora);

  /** Inserta o actualiza la partida de alimento por su id; precio nulo = pendiente de precio. */
  void guardarPartidaAlimento(
      UUID idPartida,
      UUID idLote,
      UUID idGalpon,
      String tipoAlimento,
      BigDecimal cantidadKg,
      BigDecimal precioUnitarioKgCop,
      BigDecimal valorImpuestoCop,
      String referenciaOrigen,
      Instant ahora);

  /** Inserta o actualiza el consumo de medicamento por su id. */
  void guardarConsumoMedicamento(
      UUID idConsumo,
      UUID idLote,
      UUID idGalpon,
      String medicamento,
      LocalDate fechaConsumo,
      BigDecimal cantidadUnidadBase,
      String unidadBase,
      Instant ahora);

  /** Inserta o actualiza el tramo de un consumo por su recepción; el consumo debe existir. */
  void guardarTramoMedicamento(
      UUID idConsumo,
      String referenciaRecepcion,
      BigDecimal cantidadUnidadBase,
      BigDecimal precioUnitarioBaseCop,
      BigDecimal valorImpuestoCop);
}
