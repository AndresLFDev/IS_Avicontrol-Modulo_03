package co.edu.unimagdalena.avicontrol.domain.model;

/**
 * Etapa financiera del lote, derivada por M3 (M3-CU01.FR-013). No se almacena ni se comunica a
 * Módulo 1. {@link #LIQUIDADO} existe en la derivación, pero esos lotes no se listan (FR-014).
 */
public enum EtapaLote {
  PRODUCTIVO,
  EN_COSECHA,
  AISLAMIENTO,
  POR_LIQUIDAR,
  LIQUIDADO
}
