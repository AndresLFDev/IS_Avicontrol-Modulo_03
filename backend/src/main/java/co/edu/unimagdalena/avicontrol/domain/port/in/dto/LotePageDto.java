package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.util.List;

/**
 * Respuesta paginada de la lista de lotes (M3-CU01).
 *
 * @param mensaje "N de M lotes" (FR-012), "No hay lotes disponibles para consultar" (FR-006) o "No
 *     se encontraron resultados" (FR-010)
 * @param sincronizacion última sincronización exitosa por fuente (RT-09)
 */
public record LotePageDto(
    List<LoteResumenDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String mensaje,
    EstadoSincronizacionDto sincronizacion) {}
