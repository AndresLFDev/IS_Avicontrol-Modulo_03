package co.edu.unimagdalena.avicontrol.infrastructure.exception;

import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionNoEncontradaException;
import co.edu.unimagdalena.avicontrol.domain.exception.LiquidacionYaExistenteException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoEncontradoException;
import co.edu.unimagdalena.avicontrol.domain.exception.LoteNoLiquidableException;
import co.edu.unimagdalena.avicontrol.domain.exception.ParametroInvalidoException;
import co.edu.unimagdalena.avicontrol.domain.exception.VistaPreviaDesactualizadaException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Traduce toda excepción a {@code application/problem+json} (RFC 7807) con el formato del catálogo
 * de errores de la arquitectura general: {@code type} bajo {@link #TIPO_BASE}, campo {@code
 * timestamp} y, en los 400 de validación, la lista {@code errores}. Los errores de negocio de cada
 * caso de uso se agregan aquí a medida que se implementan los planes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  public static final String TIPO_BASE = "https://avicontrol.edu.co/errors/";

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  /** Error de un campo o parámetro en una respuesta {@code 400 validacion}. */
  public record ErrorCampo(String campo, String mensaje) {}

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errores =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()))
            .toList();
    return validacion(errores, request);
  }

  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(
      HandlerMethodValidationException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var errores =
        ex.getParameterValidationResults().stream()
            .flatMap(
                resultado -> {
                  if (resultado instanceof ParameterErrors errors) {
                    return errors.getFieldErrors().stream()
                        .map(e -> new ErrorCampo(e.getField(), e.getDefaultMessage()));
                  }
                  String parametro = resultado.getMethodParameter().getParameterName();
                  return resultado.getResolvableErrors().stream()
                      .map(e -> new ErrorCampo(parametro, e.getDefaultMessage()));
                })
            .toList();
    return validacion(errores, request);
  }

  @Override
  protected ResponseEntity<Object> handleTypeMismatch(
      TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    String campo = ex.getPropertyName() != null ? ex.getPropertyName() : "parametro";
    return validacion(
        List.of(new ErrorCampo(campo, "El valor '" + ex.getValue() + "' no es válido.")), request);
  }

  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return validacion(
        List.of(new ErrorCampo(ex.getParameterName(), "El parámetro es obligatorio.")), request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    var problema =
        problema(
            HttpStatus.BAD_REQUEST,
            "validacion",
            "Error de validación",
            "El cuerpo de la solicitud no es un JSON válido o tiene tipos incorrectos.",
            request);
    problema.setProperty("errores", List.of());
    return ResponseEntity.badRequest().body(problema);
  }

  /** Validación de parámetros en beans anotados con {@code @Validated}. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> handleConstraintViolation(
      ConstraintViolationException ex, WebRequest request) {
    var errores =
        ex.getConstraintViolations().stream()
            .map(v -> new ErrorCampo(ultimoNodo(v), v.getMessage()))
            .toList();
    return validacion(errores, request);
  }

  /** Parámetro que viola una regla de un caso de uso (p. ej. {@code etapa=LIQUIDADO} en CU01). */
  @ExceptionHandler(ParametroInvalidoException.class)
  public ResponseEntity<Object> handleParametroInvalido(
      ParametroInvalidoException ex, WebRequest request) {
    return validacion(List.of(new ErrorCampo(ex.getCampo(), ex.getMessage())), request);
  }

  @ExceptionHandler(LoteNoEncontradoException.class)
  public ResponseEntity<Object> handleLoteNoEncontrado(
      LoteNoEncontradoException ex, WebRequest request) {
    return respuesta(
        problema(
            HttpStatus.NOT_FOUND,
            "lote-no-encontrado",
            "Recurso no encontrado",
            ex.getMessage(),
            request));
  }

  @ExceptionHandler(LiquidacionNoEncontradaException.class)
  public ResponseEntity<Object> handleLiquidacionNoEncontrada(
      LiquidacionNoEncontradaException ex, WebRequest request) {
    return respuesta(
        problema(
            HttpStatus.NOT_FOUND,
            "liquidacion-no-encontrada",
            "Recurso no encontrado",
            ex.getMessage(),
            request));
  }

  /** CU03.FR-007; {@code idLiquidacionActiva} permite ofrecer "Ver liquidación" o "Anular". */
  @ExceptionHandler(LiquidacionYaExistenteException.class)
  public ResponseEntity<Object> handleLiquidacionYaExistente(
      LiquidacionYaExistenteException ex, WebRequest request) {
    var problema =
        problema(
            HttpStatus.CONFLICT,
            "liquidacion-ya-existente",
            "Conflicto de Liquidación",
            ex.getMessage(),
            request);
    if (ex.getIdLiquidacionActiva() != null) {
      problema.setProperty("idLiquidacionActiva", ex.getIdLiquidacionActiva());
    }
    return respuesta(problema);
  }

  @ExceptionHandler(VistaPreviaDesactualizadaException.class)
  public ResponseEntity<Object> handleVistaPreviaDesactualizada(
      VistaPreviaDesactualizadaException ex, WebRequest request) {
    return respuesta(
        problema(
            HttpStatus.CONFLICT,
            "vista-previa-desactualizada",
            "Los datos cambiaron",
            ex.getMessage(),
            request));
  }

  /** Precondiciones de CU03: {@code causa} y, si faltan precios, {@code partidasPendientes}. */
  @ExceptionHandler(LoteNoLiquidableException.class)
  public ResponseEntity<Object> handleLoteNoLiquidable(
      LoteNoLiquidableException ex, WebRequest request) {
    var problema =
        problema(
            HttpStatus.UNPROCESSABLE_CONTENT,
            "lote-no-liquidable",
            "Lote no apto para liquidar",
            ex.getMessage(),
            request);
    problema.setProperty("causa", ex.getCausa().name());
    if (!ex.getPartidasPendientes().isEmpty()) {
      problema.setProperty("partidasPendientes", ex.getPartidasPendientes());
    }
    return respuesta(problema);
  }

  /** Cualquier error no controlado: {@code 500 error-interno}, sin exponer trazas. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> handleNoControlada(Exception ex, WebRequest request) {
    log.error("Error no controlado", ex);
    var problema =
        problema(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "error-interno",
            "Error interno",
            "Ocurrió un error inesperado. Intente de nuevo más tarde.",
            request);
    return ResponseEntity.internalServerError().body(problema);
  }

  /**
   * Punto común de las respuestas de {@link ResponseEntityExceptionHandler} (404 de ruta, 405,
   * 415…): les asigna un {@code type} del catálogo y agrega {@code timestamp}.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    if (body instanceof ProblemDetail problema) {
      completar(problema, request);
    }
    return super.handleExceptionInternal(ex, body, headers, statusCode, request);
  }

  private static ResponseEntity<Object> respuesta(ProblemDetail problema) {
    return ResponseEntity.status(problema.getStatus()).body(problema);
  }

  private ResponseEntity<Object> validacion(List<ErrorCampo> errores, WebRequest request) {
    var problema =
        problema(
            HttpStatus.BAD_REQUEST,
            "validacion",
            "Error de validación",
            "La solicitud contiene parámetros inválidos.",
            request);
    problema.setProperty("errores", errores);
    return ResponseEntity.badRequest().body(problema);
  }

  private ProblemDetail problema(
      HttpStatus status, String tipo, String titulo, String detalle, WebRequest request) {
    var problema = ProblemDetail.forStatusAndDetail(status, detalle);
    problema.setType(URI.create(TIPO_BASE + tipo));
    problema.setTitle(titulo);
    completar(problema, request);
    return problema;
  }

  private void completar(ProblemDetail problema, WebRequest request) {
    if (problema.getType() == null || "about:blank".equals(problema.getType().toString())) {
      problema.setType(URI.create(TIPO_BASE + tipoPorEstado(problema.getStatus())));
    }
    if (problema.getInstance() == null && request instanceof ServletWebRequest servlet) {
      problema.setInstance(URI.create(servlet.getRequest().getRequestURI()));
    }
    problema.setProperty("timestamp", Instant.now(clock));
  }

  private static String tipoPorEstado(int status) {
    return switch (status) {
      case 400 -> "validacion";
      case 404 -> "recurso-no-encontrado";
      case 405 -> "metodo-no-permitido";
      case 406 -> "formato-no-aceptable";
      case 415 -> "tipo-contenido-no-soportado";
      default -> "error-interno";
    };
  }

  private static String ultimoNodo(ConstraintViolation<?> violacion) {
    String ruta = violacion.getPropertyPath().toString();
    int punto = ruta.lastIndexOf('.');
    return punto >= 0 ? ruta.substring(punto + 1) : ruta;
  }
}
