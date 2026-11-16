package cl.unab.productos.integration;

import cl.unab.productos.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test de integración: contexto Spring completo (controlador + servicio + JPA + BD H2),
 * sin mocks. Verifica que las capas funcionan juntas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ProductoRepository repository;

    @BeforeEach
    void limpiar() {
        repository.deleteAll();
    }

    private String json(String codigo, String nombre, int stock) {
        return """
                {"codigo":"%s","nombre":"%s","descripcion":"Prueba","categoria":"Audio",
                 "precio":19990.90,"stock":%d}
                """.formatted(codigo, nombre, stock);
    }

    private int crear(String codigo) throws Exception {
        String body = mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(json(codigo, "Parlante", 5)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return Integer.parseInt(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }

    @Test
    void flujoCrudCompleto() throws Exception {
        // CREATE
        int id = crear("INT-1");
        assertThat(repository.count()).isEqualTo(1);

        // READ
        mvc.perform(get("/api/productos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("INT-1"))
                .andExpect(jsonPath("$.precio").value(19990.90))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.fechaCreacion").exists());

        // UPDATE
        mvc.perform(put("/api/productos/" + id)
                        .contentType(MediaType.APPLICATION_JSON).content(json("INT-1", "Parlante XL", 99)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Parlante XL"))
                .andExpect(jsonPath("$.stock").value(99));
        assertThat(repository.findById(id)).get()
                .satisfies(p -> assertThat(p.getStock()).isEqualTo(99));

        // DELETE
        mvc.perform(delete("/api/productos/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/productos/" + id)).andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void listarYFiltrarPorCategoria() throws Exception {
        crear("INT-A");
        crear("INT-B");

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/productos").param("categoria", "audio"))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/productos").param("categoria", "Redes"))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void alertasStockBajoConDatosReales() throws Exception {
        // stock: 0 (sin stock), 3 (crítico), 8 (bajo), 10 (ok: la regla es stock < 10),
        // 11 (ok), 50 (ok) y uno inactivo con stock 1
        for (String[] p : new String[][]{{"S-0", "0", "true"}, {"S-3", "3", "true"}, {"S-8", "8", "true"},
                {"S-10", "10", "true"}, {"S-11", "11", "true"},
                {"S-50", "50", "true"}, {"S-INACTIVO", "1", "false"}}) {
            mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("""
                            {"codigo":"%s","nombre":"Prod","categoria":"Test","precio":100,"stock":%s,"activo":%s}
                            """.formatted(p[0], p[1], p[2])))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/productos/alertas/stock-bajo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockMinimo").value(10))
                .andExpect(jsonPath("$.totalAlertas").value(3))       // 10, 11, 50 y el inactivo no aparecen
                .andExpect(jsonPath("$.productos[0].codigo").value("S-0"))
                .andExpect(jsonPath("$.productos[0].nivel").value("SIN_STOCK"))
                .andExpect(jsonPath("$.productos[1].nivel").value("CRITICO"))
                .andExpect(jsonPath("$.productos[2].codigo").value("S-8"))
                .andExpect(jsonPath("$.productos[2].nivel").value("BAJO"))
                .andExpect(jsonPath("$.productos[2].unidadesSugeridas").value(2));
    }

    @Test
    void crearCodigoDuplicadoDevuelve409() throws Exception {
        crear("DUP-1");

        mvc.perform(post("/api/productos")
                        .contentType(MediaType.APPLICATION_JSON).content(json("DUP-1", "Otro", 1)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Código duplicado"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void actualizarConCodigoDeOtroProductoDevuelve409() throws Exception {
        crear("X-1");
        int id2 = crear("X-2");

        mvc.perform(put("/api/productos/" + id2)
                        .contentType(MediaType.APPLICATION_JSON).content(json("X-1", "Choque", 1)))
                .andExpect(status().isConflict());
    }
}
