package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import java.util.List;

/**
 * Respuesta paginada del historial de Liquidaciones (M3-CU06).
 *
 * @param mensaje {@code null} con resultados; el texto de FR-007 cuando {@code content} está vacío
 */
public record HistorialPageDto(
    List<HistorialItemDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    String mensaje) {}
