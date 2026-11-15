package cl.unab.productos.service;

import cl.unab.productos.dto.ProductoRequest;
import cl.unab.productos.dto.ProductoResponse;
import cl.unab.productos.entity.Producto;
import cl.unab.productos.exception.CodigoDuplicadoException;
import cl.unab.productos.exception.ProductoNotFoundException;
import cl.unab.productos.repository.ProductoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Tests unitarios: la lógica del servicio aislada, con el repositorio simulado. */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @InjectMocks
    private ProductoService service;

    private static Producto producto(Integer id, String codigo) {
        Producto p = new Producto();
        p.setId(id);
        p.setCodigo(codigo);
        p.setNombre("Mouse");
        p.setCategoria("Accesorios");
        p.setPrecio(new BigDecimal("9990"));
        p.setStock(10);
        p.setActivo(true);
        return p;
    }

    private static ProductoRequest request(String codigo, Boolean activo) {
        return new ProductoRequest(codigo, "Mouse", "desc", "Accesorios", new BigDecimal("9990"), 10, activo);
    }

    @Test
    @DisplayName("listar sin categoría devuelve todos ordenados por id")
    void listarTodos() {
        when(repository.findAll(any(Sort.class))).thenReturn(List.of(producto(1, "A"), producto(2, "B")));

        List<ProductoResponse> res = service.listar(null);

        assertThat(res).extracting(ProductoResponse::codigo).containsExactly("A", "B");
        verify(repository, never()).findByCategoriaIgnoreCaseOrderByIdAsc(any());
    }

    @Test
    @DisplayName("listar con categoría filtra en el repositorio")
    void listarPorCategoria() {
        when(repository.findByCategoriaIgnoreCaseOrderByIdAsc("audio")).thenReturn(List.of(producto(5, "X")));

        assertThat(service.listar("audio")).hasSize(1);
        verify(repository, never()).findAll(any(Sort.class));
    }

    @Test
    void obtenerExistente() {
        when(repository.findById(1)).thenReturn(Optional.of(producto(1, "PRD-1")));

        assertThat(service.obtener(1).codigo()).isEqualTo("PRD-1");
    }

    @Test
    void obtenerInexistenteLanzaNotFound() {
        when(repository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtener(99))
                .isInstanceOf(ProductoNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("crear guarda el producto y activo por defecto es true")
    void crear() {
        when(repository.existsByCodigo("PRD-9")).thenReturn(false);
        when(repository.save(any(Producto.class))).thenAnswer(inv -> {
            Producto p = inv.getArgument(0);
            p.setId(9);
            return p;
        });

        ProductoResponse res = service.crear(request("PRD-9", null));

        assertThat(res.id()).isEqualTo(9);
        assertThat(res.activo()).isTrue();
    }

    @Test
    void crearConCodigoDuplicadoLanzaConflicto() {
        when(repository.existsByCodigo("PRD-1")).thenReturn(true);

        assertThatThrownBy(() -> service.crear(request("PRD-1", true)))
                .isInstanceOf(CodigoDuplicadoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar() {
        Producto existente = producto(1, "PRD-1");
        when(repository.findById(1)).thenReturn(Optional.of(existente));
        when(repository.existsByCodigoAndIdNot("PRD-1", 1)).thenReturn(false);
        when(repository.save(existente)).thenReturn(existente);

        ProductoRequest req = new ProductoRequest("PRD-1", "Nuevo", null, "Audio", new BigDecimal("500"), 3, false);
        ProductoResponse res = service.actualizar(1, req);

        assertThat(res.nombre()).isEqualTo("Nuevo");
        assertThat(res.categoria()).isEqualTo("Audio");
        assertThat(res.activo()).isFalse();
    }

    @Test
    void actualizarConCodigoDeOtroProductoLanzaConflicto() {
        when(repository.findById(1)).thenReturn(Optional.of(producto(1, "PRD-1")));
        when(repository.existsByCodigoAndIdNot("PRD-2", 1)).thenReturn(true);

        assertThatThrownBy(() -> service.actualizar(1, request("PRD-2", true)))
                .isInstanceOf(CodigoDuplicadoException.class);
    }

    @Test
    void actualizarInexistenteLanzaNotFound() {
        when(repository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(5, request("X", true)))
                .isInstanceOf(ProductoNotFoundException.class);
    }

    @Test
    void eliminar() {
        Producto p = producto(1, "PRD-1");
        when(repository.findById(1)).thenReturn(Optional.of(p));

        service.eliminar(1);

        verify(repository).delete(p);
    }

    @Test
    void eliminarInexistenteLanzaNotFound() {
        when(repository.findById(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(7)).isInstanceOf(ProductoNotFoundException.class);
        verify(repository, never()).delete(any());
    }
}
