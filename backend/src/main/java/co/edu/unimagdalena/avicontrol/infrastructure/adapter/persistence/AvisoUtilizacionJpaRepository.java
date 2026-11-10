package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface AvisoUtilizacionJpaRepository
    extends JpaRepository<AvisoUtilizacionResultadoEntity, Long> {

  /**
   * Inserta el aviso salvo que el resultado ya tenga uno. Se usa una sola sentencia con ON CONFLICT
   * para que dos confirmaciones simultáneas no fallen por la restricción UNIQUE de id_resultado.
   */
  @Modifying
  @Query(
      value =
          """
          INSERT INTO aviso_utilizacion_resultado (id_resultado, id_liquidacion, fecha_hora_emision)
          VALUES (:idResultado, :idLiquidacion, :fechaHora)
          ON CONFLICT (id_resultado) DO NOTHING
          """,
      nativeQuery = true)
  int insertarSiNoExiste(UUID idResultado, long idLiquidacion, Instant fechaHora);
}
