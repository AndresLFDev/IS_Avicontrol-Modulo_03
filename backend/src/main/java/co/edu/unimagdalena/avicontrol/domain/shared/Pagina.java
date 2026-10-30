package co.edu.unimagdalena.avicontrol.domain.shared;

import java.util.List;

/**
 * Página de resultados en Java puro, para que el dominio no dependa de {@code
 * org.springframework.data.domain.Page}. {@code totalElements} y {@code totalPages} reflejan el
 * total real del filtro aunque la página pedida esté vacía.
 */
public record Pagina<T>(List<T> contenido, int page, int size, long totalElements, int totalPages) {

  public Pagina {
    contenido = List.copyOf(contenido);
  }

  public static <T> Pagina<T> de(List<T> contenido, int page, int size, long totalElements) {
    int totalPages = (int) ((totalElements + size - 1) / size);
    return new Pagina<>(contenido, page, size, totalElements, totalPages);
  }
}
