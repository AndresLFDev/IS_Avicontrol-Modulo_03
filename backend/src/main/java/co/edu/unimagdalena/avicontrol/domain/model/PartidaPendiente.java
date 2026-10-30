package co.edu.unimagdalena.avicontrol.domain.model;

/** Partida sin precio aplicable que bloquea la Liquidación (CU03.FR-006). */
public record PartidaPendiente(
    CategoriaCosto categoria, String concepto, String referenciaOrigen) {}
