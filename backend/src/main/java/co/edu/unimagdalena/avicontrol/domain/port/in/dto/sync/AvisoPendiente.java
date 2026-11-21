package co.edu.unimagdalena.avicontrol.domain.port.in.dto.sync;

import java.util.UUID;

/** Aviso de utilización de un resultado que aún no se confirmó a Módulo 2. */
public record AvisoPendiente(long idAviso, UUID idResultado, UUID idLote, int intentos) {}
