package co.edu.unimagdalena.avicontrol.domain.port.in.dto;

import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;

/** Respuesta de {@code PUT /api/v1/liquidaciones/{id}/anulacion} (plan 003). */
public record AnulacionDto(
    long idLiquidacion, String estado, RegistroAnulacion registroAnulacion) {}
