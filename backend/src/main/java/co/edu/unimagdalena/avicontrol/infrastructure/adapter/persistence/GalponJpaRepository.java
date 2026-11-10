package co.edu.unimagdalena.avicontrol.infrastructure.adapter.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface GalponJpaRepository extends JpaRepository<GalponEntity, UUID> {

  /** Solo el nombre: evita leer campos de un proxy perezoso ya presente en la sesión. */
  @Query("SELECT g.nombre FROM GalponEntity g WHERE g.idGalpon = :idGalpon")
  Optional<String> findNombre(UUID idGalpon);
}
