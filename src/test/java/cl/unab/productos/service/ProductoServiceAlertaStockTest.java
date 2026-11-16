package cl.unab.productos.service;

import cl.unab.productos.dto.AlertaStockResponse;
import cl.unab.productos.dto.NivelAlerta;
import cl.unab.productos.entity.Producto;
import cl.unab.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PA-4 / BUG-01: prueba ProductoService.alertasStockBajo() con el repositorio REAL (JPA + H2),
 * sin mocks. Así se ejecuta de verdad la línea:
 *
 *   repository.findByActivoTrueAndStockLessThanEqualOrderByStockAscIdAsc(STOCK_MINIMO)
 *
 * Regla de negocio: genera alerta todo producto activo con stock <= STOCK_MINIMO (10).
 */
@DataJpaTest
@Import(ProductoService.class)
@ActiveProfiles("test")
class ProductoServiceAlertaStockTest {

    @Autowired
    private ProductoService service;

    @Autowired
    private ProductoRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    private void guardar(String codigo, int stock, boolean activo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre("Producto " + codigo);
        p.setCategoria("Test");
        p.setPrecio(new BigDecimal("1000"));
        p.setStock(stock);
        p.setActivo(activo);
        repository.save(p);
    }

    private static Optional<AlertaStockResponse.ProductoAlerta> buscar(AlertaStockResponse res, String codigo) {
        return res.productos().stream().filter(a -> a.codigo().equals(codigo)).findFirst();
    }

    @ParameterizedTest(name = "stock {0} -> en alerta: {1}, nivel {2}, sugeridas {3}")
    @CsvSource({
            "0,  true,  SIN_STOCK, 10",
            "5,  true,  CRITICO,   5",
            "9,  true,  BAJO,      1",
            "10, true,  BAJO,      0",    // borde: stock igual a STOCK_MINIMO SÍ genera alerta
            "11, false, ,          "      // justo sobre el borde: NO genera alerta
    })
    @DisplayName("borde de STOCK_MINIMO (parametrizado)")
    void bordeDeStockMinimo(int stock, boolean enAlerta, NivelAlerta nivel, Integer sugeridas) {
        guardar("P-" + stock, stock, true);

        AlertaStockResponse res = service.alertasStockBajo();

        Optional<AlertaStockResponse.ProductoAlerta> alerta = buscar(res, "P-" + stock);
        assertThat(alerta.isPresent()).as("¿stock %d aparece en la alerta?", stock).isEqualTo(enAlerta);
        alerta.ifPresent(a -> {
            assertThat(a.nivel()).isEqualTo(nivel);
            assertThat(a.unidadesSugeridas()).isEqualTo(sugeridas);
        });
    }

    @Test
    @DisplayName("stock 10 aparece y stock 11 no aparece")
    void stock10ApareceYStock11No() {
        guardar("S-10", 10, true);
        guardar("S-11", 11, true);

        AlertaStockResponse res = service.alertasStockBajo();

        assertThat(res.stockMinimo()).isEqualTo(ProductoService.STOCK_MINIMO);
        assertThat(res.totalAlertas()).isEqualTo(1);
        assertThat(res.productos()).extracting(AlertaStockResponse.ProductoAlerta::codigo)
                .containsExactly("S-10")
                .doesNotContain("S-11");
    }

    @Test
    @DisplayName("ordena por stock ascendente y excluye inactivos")
    void ordenaYExcluyeInactivos() {
        guardar("S-10", 10, true);
        guardar("S-2", 2, true);
        guardar("S-9", 9, true);
        guardar("INACTIVO", 1, false);

        AlertaStockResponse res = service.alertasStockBajo();

        assertThat(res.productos()).extracting(AlertaStockResponse.ProductoAlerta::codigo)
                .containsExactly("S-2", "S-9", "S-10");
    }
}
