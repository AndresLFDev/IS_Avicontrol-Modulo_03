package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync.AvisoPendiente;
import co.edu.unimagdalena.avicontrol.domain.port.out.AvisoUtilizacionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Outbox del aviso de utilización: el envío a M2 por REST lo hace el publicador programado (plan
 * 001).
 */
@Repository
public class AvisoUtilizacionJpaAdapter implements AvisoUtilizacionRepository {

  private static final String UTILIZADO = "UTILIZADO";
  private static final String PENDIENTE = "PENDIENTE";
  private static final String ENTREGADO = "ENTREGADO";

  private final ResultadoFinalSacrificioJpaRepository resultados;
  private final AvisoUtilizacionJpaRepository avisos;

  public AvisoUtilizacionJpaAdapter(
      ResultadoFinalSacrificioJpaRepository resultados, AvisoUtilizacionJpaRepository avisos) {
    this.resultados = resultados;
    this.avisos = avisos;
  }

  @Override
  public void registrarUtilizacion(UUID idResultado, long idLiquidacion, Instant fechaHora) {
    resultados.findById(idResultado).ifPresent(r -> r.estadoUtilizacion = UTILIZADO);
    // id_resultado es UNIQUE: tras anular y volver a liquidar se conserva el aviso original
    avisos.insertarSiNoExiste(idResultado, idLiquidacion, fechaHora);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AvisoPendiente> pendientes() {
    return avisos.findByEstadoEntregaOrderByIdAsc(PENDIENTE).stream()
        .map(
            a ->
                new AvisoPendiente(
                    a.id,
                    a.idResultado,
                    resultados
                        .findById(a.idResultado)
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "El aviso " + a.id + " no tiene resultado " + a.idResultado))
                        .idLote,
                    a.intentos))
        .toList();
  }

  @Override
  @Transactional
  public void marcarEntregado(long idAviso) {
    // findById (no getReferenceById): los campos son de paquete y un proxy no los propagaría
    avisos
        .findById(idAviso)
        .ifPresent(
            a -> {
              a.estadoEntrega = ENTREGADO;
              a.intentos++;
            });
  }

  @Override
  @Transactional
  public void registrarIntentoFallido(long idAviso) {
    avisos.findById(idAviso).ifPresent(a -> a.intentos++);
  }
}
