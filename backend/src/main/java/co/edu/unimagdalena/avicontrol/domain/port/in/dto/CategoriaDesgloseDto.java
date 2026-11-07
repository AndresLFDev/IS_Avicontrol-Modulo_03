package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.util.List;

/** Partidas de una categoría de costo con su subtotal (CU05.FR-001). */
public record CategoriaDesgloseDto(
    String categoria, String etiqueta, long subtotalCop, List<PartidaDesgloseDto> partidas) {}
