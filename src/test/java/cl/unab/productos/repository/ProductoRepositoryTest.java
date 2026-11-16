package cl.unab.productos.repository;

import cl.unab.productos.entity.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Test de integración de la capa de persistencia (JPA + H2). */
@DataJpaTest
@ActiveProfiles("test")
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository repository;

    private Producto guardar(String codigo, String categoria) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre("Producto " + codigo);
        p.setCategoria(categoria);
        p.setPrecio(new BigDecimal("1000.50"));
        p.setStock(3);
        return repository.save(p);
    }

    @BeforeEach
    void datos() {
        guardar("A-1", "Audio");
        guardar("A-2", "audio");
        guardar("C-1", "Computación");
    }

    @Test
    void guardarAsignaIdYValoresPorDefecto() {
        Producto p = guardar("N-1", "Redes");

        assertThat(p.getId()).isNotNull();
        assertThat(p.getActivo()).isTrue();
        assertThat(p.getFechaCreacion()).isNotNull();
    }

    @Test
    void buscarPorCategoriaIgnoraMayusculas() {
        assertThat(repository.findByCategoriaIgnoreCaseOrderByIdAsc("AUDIO"))
                .extracting(Producto::getCodigo)
                .containsExactly("A-1", "A-2");
    }

    @Test
    void existsByCodigo() {
        assertThat(repository.existsByCodigo("C-1")).isTrue();
        assertThat(repository.existsByCodigo("NO-EXISTE")).isFalse();
    }

    /**
     * PA-4 / BUG-01: borde de la regla "stock <= 10" probado contra la base (H2),
     * es decir, la consulta SQL que genera Spring Data, no un mock.
     */
    @ParameterizedTest(name = "stock {0} -> en alerta: {1}")
    @CsvSource({
            "0,  true",
            "9,  true",
            "10, true",   // borde: stock igual al mínimo SÍ genera alerta
            "11, false"   // justo sobre el borde: NO genera alerta
    })
    void alertaStockRespetaElBordeDe10(int stock, boolean enAlerta) {
        Producto p = guardar("B-" + stock, "Borde");
        p.setStock(stock);
        repository.save(p);

        assertThat(repository.findByActivoTrueAndStockLessThanEqualOrderByStockAscIdAsc(10))
                .extracting(Producto::getCodigo)
                .as("producto con stock %d", stock)
                .matches(codigos -> codigos.contains("B-" + stock) == enAlerta);
    }

    @Test
    void existsByCodigoAndIdNotExcluyeElPropioProducto() {
        Producto c1 = repository.findAll().stream().filter(p -> p.getCodigo().equals("C-1")).findFirst().orElseThrow();

        assertThat(repository.existsByCodigoAndIdNot("C-1", c1.getId())).isFalse();
        assertThat(repository.existsByCodigoAndIdNot("A-1", c1.getId())).isTrue();
    }
}
