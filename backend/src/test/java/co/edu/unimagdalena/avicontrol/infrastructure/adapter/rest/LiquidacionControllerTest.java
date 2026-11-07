package co.edu.unimagdalena.avicontrol.infrastructure.adapter.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaAnuladaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoLiquidableException;
import co.edu.unimagdalena.avicontrol.domain.exception.VistaPreviaDesactualizadaException;
import co.edu.unimagdalena.avicontrol.domain.model.CategoriaCosto;
import co.edu.unimagdalena.avicontrol.domain.model.CausaNoLiquidable;
import co.edu.unimagdalena.avicontrol.domain.model.PartidaPendiente;
import co.edu.unimagdalena.avicontrol.domain.model.RegistroAnulacion;
import co.edu.unimagdalena.avicontrol.domain.model.SubtotalesCategoria;
import co.edu.unimagdalena.avicontrol.domain.port.in.AnularLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.ConsultarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.GenerarLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.PreviewLiquidacionUseCase;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.AnulacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.GenerarLiquidacionCommand;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionDto;
import co.edu.unimagdalena.avicontrol.domain.port.in.dto.LiquidacionPreviaDto;
import co.edu.unimagdalena.avicontrol.infrastructure.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Plan 003, T007, T010, T013 y T016: contrato REST de las liquidaciones. */
class LiquidacionControllerTest {

  private static final UUID ID_LOTE = UUID.fromString("b0000000-0000-0000-0000-000000000003");
  private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

  private PreviewLiquidacionUseCase vistaPrevia;
  private GenerarLiquidacionUseCase generar;
  private ConsultarLiquidacionUseCase consultar;
  private AnularLiquidacionUseCase anular;
  private MockMvc mvc;

  @BeforeEach
  void configurar() {
    vistaPrevia = mock(PreviewLiquidacionUseCase.class);
    generar = mock(GenerarLiquidacionUseCase.class);
    consultar = mock(ConsultarLiquidacionUseCase.class);
    anular = mock(AnularLiquidacionUseCase.class);
    var validador = new LocalValidatorFactoryBean();
    validador.afterPropertiesSet();
    mvc =
        MockMvcBuilders.standaloneSetup(
                new LiquidacionController(vistaPrevia, generar, consultar, anular))
            .setControllerAdvice(new GlobalExceptionHandler(Clock.fixed(AHORA, ZoneOffset.UTC)))
            .setValidator(validador)
            .build();
  }

  @Test
  void laVistaPreviaResponde200ConElUsuarioRecortado() throws Exception {
    when(vistaPrevia.calcularVistaPrevia(any())).thenReturn(previa());

    mvc.perform(
            post("/api/v1/liquidaciones/previa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500.00,"usuario":"  Jorge  "}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.esVistaPrevia").value(true))
        .andExpect(jsonPath("$.aviso").value("Vista previa · sin generar"))
        .andExpect(jsonPath("$.ventaBrutaCop").value(107_100_000))
        .andExpect(jsonPath("$.porcentajeMortalidad").value(5.56))
        .andExpect(jsonPath("$.subtotalesCategoria.costoPoblacion").value(18_000_000))
        .andExpect(jsonPath("$.huellaDatos").value("abc"));

    verify(vistaPrevia)
        .calcularVistaPrevia(
            new GenerarLiquidacionCommand(ID_LOTE, new BigDecimal("4500.00"), "Jorge", null));
  }

  @Test
  void precioCeroYUsuarioVacioResponden400() throws Exception {
    mvc.perform(
            post("/api/v1/liquidaciones/previa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":0,"usuario":"   "}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
        .andExpect(
            jsonPath("$.errores[?(@.campo == 'precioKgCop')].mensaje")
                .value("El precio por kg debe ser mayor que 0"))
        .andExpect(jsonPath("$.errores[?(@.campo == 'usuario')]").exists());
    verifyNoInteractions(vistaPrevia);
  }

  @Test
  void loteNoLiquidableResponde422ConCausaYPartidas() throws Exception {
    when(vistaPrevia.calcularVistaPrevia(any()))
        .thenThrow(
            new LoteNoLiquidableException(
                CausaNoLiquidable.PARTIDAS_SIN_PRECIO,
                List.of(new PartidaPendiente(CategoriaCosto.ALIMENTO, "Engorde", "REC-2026-121"))));

    mvc.perform(
            post("/api/v1/liquidaciones/previa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500,"usuario":"Jorge"}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/lote-no-liquidable"))
        .andExpect(jsonPath("$.causa").value("PARTIDAS_SIN_PRECIO"))
        .andExpect(jsonPath("$.partidasPendientes[0].concepto").value("Engorde"))
        .andExpect(jsonPath("$.partidasPendientes[0].referenciaOrigen").value("REC-2026-121"));
  }

  @Test
  void loteYaLiquidadoResponde409ConLaActiva() throws Exception {
    when(vistaPrevia.calcularVistaPrevia(any()))
        .thenThrow(new LiquidacionYaExistenteException(ID_LOTE, 42L));

    mvc.perform(
            post("/api/v1/liquidaciones/previa")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500,"usuario":"Jorge"}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.type").value("https://avicontrol.edu.co/errors/liquidacion-ya-existente"))
        .andExpect(jsonPath("$.idLiquidacionActiva").value(42));
  }

  @Test
  void confirmarResponde201ConLocation() throws Exception {
    when(generar.generarLiquidacion(any())).thenReturn(liquidacion());

    mvc.perform(
            post("/api/v1/liquidaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500,"usuario":"Jorge","huellaDatos":"abc"}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/liquidaciones/42"))
        .andExpect(jsonPath("$.estado").value("ACTIVA"))
        .andExpect(jsonPath("$.accionesDisponibles[1]").value("ANULAR"));
  }

  @Test
  void confirmarSinHuellaResponde400() throws Exception {
    mvc.perform(
            post("/api/v1/liquidaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500,"usuario":"Jorge"}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errores[0].campo").value("huellaDatos"));
  }

  @Test
  void confirmarConDatosCambiadosResponde409() throws Exception {
    when(generar.generarLiquidacion(any())).thenThrow(new VistaPreviaDesactualizadaException());

    mvc.perform(
            post("/api/v1/liquidaciones")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"idLote":"%s","precioKgCop":4500,"usuario":"Jorge","huellaDatos":"vieja"}
                    """
                        .formatted(ID_LOTE)))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.type")
                .value("https://avicontrol.edu.co/errors/vista-previa-desactualizada"));
  }

  @Test
  void consultarUnaInexistenteResponde404() throws Exception {
    when(consultar.consultarLiquidacion(999L))
        .thenThrow(new LiquidacionNoEncontradaException(999L));

    mvc.perform(get("/api/v1/liquidaciones/999"))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.type").value("https://avicontrol.edu.co/errors/liquidacion-no-encontrada"))
        .andExpect(jsonPath("$.instance").value("/api/v1/liquidaciones/999"));
  }

  @Test
  void consultarDevuelveLaLiquidacion() throws Exception {
    when(consultar.consultarLiquidacion(42L)).thenReturn(liquidacion());

    mvc.perform(get("/api/v1/liquidaciones/42"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idLiquidacion").value(42))
        .andExpect(jsonPath("$.usuarioResponsable").value("Jorge"))
        .andExpect(jsonPath("$.fechaHoraGeneracion").value("2026-10-29T15:00:00Z"));
  }

  @Test
  void anularResponde200ConElMotivoYElUsuarioRecortados() throws Exception {
    var registro = new RegistroAnulacion(7L, "Error en el precio", AHORA, "Ana");
    when(anular.anularLiquidacion(42L, "Error en el precio", "Ana"))
        .thenReturn(new AnulacionDto(42L, "ANULADA", registro));

    mvc.perform(
            put("/api/v1/liquidaciones/42/anulacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"motivo":"  Error en el precio  ","usuario":"  Ana  "}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idLiquidacion").value(42))
        .andExpect(jsonPath("$.estado").value("ANULADA"))
        .andExpect(jsonPath("$.registroAnulacion.idAnulacion").value(7))
        .andExpect(jsonPath("$.registroAnulacion.motivo").value("Error en el precio"))
        .andExpect(jsonPath("$.registroAnulacion.fechaHoraAnulacion").value("2026-10-29T15:00:00Z"))
        .andExpect(jsonPath("$.registroAnulacion.usuarioResponsable").value("Ana"));
  }

  @Test
  void motivoDeSoloEspaciosOCortoYUsuarioVacioResponden400() throws Exception {
    for (var motivo : new String[] {"            ", "  corto  ", "x".repeat(501)}) {
      mvc.perform(
              put("/api/v1/liquidaciones/42/anulacion")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"motivo":"%s","usuario":" "}
                      """
                          .formatted(motivo)))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("https://avicontrol.edu.co/errors/validacion"))
          .andExpect(jsonPath("$.errores[?(@.campo == 'motivo')]").isNotEmpty())
          .andExpect(jsonPath("$.errores[?(@.campo == 'usuario')]").isNotEmpty());
    }
    verifyNoInteractions(anular);
  }

  @Test
  void anularUnaYaAnuladaResponde409ConSuRegistro() throws Exception {
    var registro = new RegistroAnulacion(7L, "Error en el precio", AHORA, "Ana");
    when(anular.anularLiquidacion(42L, "Otro motivo cualquiera", "Luis"))
        .thenThrow(new LiquidacionYaAnuladaException(42L, registro));

    mvc.perform(
            put("/api/v1/liquidaciones/42/anulacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"motivo":"Otro motivo cualquiera","usuario":"Luis"}
                    """))
        .andExpect(status().isConflict())
        .andExpect(
            jsonPath("$.type").value("https://avicontrol.edu.co/errors/liquidacion-ya-anulada"))
        .andExpect(jsonPath("$.detail").value("La Liquidación 42 ya fue anulada."))
        .andExpect(jsonPath("$.registroAnulacion.motivo").value("Error en el precio"))
        .andExpect(jsonPath("$.registroAnulacion.usuarioResponsable").value("Ana"));
  }

  @Test
  void anularUnaInexistenteResponde404() throws Exception {
    when(anular.anularLiquidacion(999L, "Error en el precio", "Ana"))
        .thenThrow(new LiquidacionNoEncontradaException(999L));

    mvc.perform(
            put("/api/v1/liquidaciones/999/anulacion")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"motivo":"Error en el precio","usuario":"Ana"}
                    """))
        .andExpect(status().isNotFound())
        .andExpect(
            jsonPath("$.type").value("https://avicontrol.edu.co/errors/liquidacion-no-encontrada"));
  }

  private static LiquidacionPreviaDto previa() {
    return new LiquidacionPreviaDto(
        true,
        "Vista previa · sin generar",
        ID_LOTE,
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        false,
        new BigDecimal("4500.00"),
        8500,
        new BigDecimal("23800.00"),
        new BigDecimal("2.80"),
        107_100_000,
        500,
        new BigDecimal("5.56"),
        new SubtotalesCategoria(64_800_000, 2_200_000, 18_000_000),
        85_000_000,
        22_100_000,
        AHORA,
        AHORA,
        "abc");
  }

  private static LiquidacionDto liquidacion() {
    return new LiquidacionDto(
        42L,
        "ACTIVA",
        ID_LOTE,
        UUID.randomUUID(),
        "L-2026-03",
        "Galpón Este",
        false,
        new BigDecimal("4500.00"),
        8500,
        new BigDecimal("23800.00"),
        new BigDecimal("2.80"),
        107_100_000,
        500,
        new BigDecimal("5.56"),
        new SubtotalesCategoria(64_800_000, 2_200_000, 18_000_000),
        85_000_000,
        22_100_000,
        AHORA,
        "Jorge",
        AHORA,
        AHORA,
        null,
        List.of("VER_DESGLOSE", "ANULAR"));
  }
}
