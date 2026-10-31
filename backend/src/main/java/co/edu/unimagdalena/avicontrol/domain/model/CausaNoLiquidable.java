package co.edu.unimagdalena.avicontrol.domain.model;

/** Causas de {@code 422 lote-no-liquidable}, en el orden en que se evalúan (plan 003). */
public enum CausaNoLiquidable {
  SIN_ALERTA_VACIADO(
      "La Liquidación se habilita cuando Módulo 1 registre la alerta de vaciado sanitario tras"
          + " retirar todas las aves del galpón."),
  SIN_RESULTADO_SACRIFICIO(
      "El lote no cuenta con resultado final de sacrificio sincronizado desde Módulo 2. No es"
          + " posible liquidar"),
  SIN_PARTIDAS_ALIMENTO("No hay partidas de alimento sincronizadas para este ciclo"),
  PARTIDAS_SIN_PRECIO("Hay partidas de costo sin precio aplicable. No es posible liquidar.");

  private final String mensaje;

  CausaNoLiquidable(String mensaje) {
    this.mensaje = mensaje;
  }

  public String mensaje() {
    return mensaje;
  }
}
