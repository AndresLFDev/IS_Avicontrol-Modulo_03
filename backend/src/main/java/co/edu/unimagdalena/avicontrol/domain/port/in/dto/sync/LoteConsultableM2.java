package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.UUID;

/** Lote de la copia local sobre el que hay que consultar a Módulo 2, con su galpón. */
public record LoteConsultableM2(UUID idLote, UUID idGalpon) {}
