package co.edu.unimagdalena.avicontrol.domain.model;

/** Subtotales de los Costos Operativos por categoría, en COP enteros (CU03.FR-014). */
public record SubtotalesCategoria(long alimento, long insumosMedicos, long costoPoblacion) {

  public long total() {
    return alimento + insumosMedicos + costoPoblacion;
  }
}
