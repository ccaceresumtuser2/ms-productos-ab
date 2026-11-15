package cl.unab.productos.controller;

import cl.unab.productos.dto.ProductoRequest;
import cl.unab.productos.dto.ProductoResponse;
import cl.unab.productos.exception.CodigoDuplicadoException;
import cl.unab.productos.exception.ProductoNotFoundException;
import cl.unab.productos.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tests unitarios de la capa web: solo el controlador, con el servicio simulado. */
@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductoService service;

    private static final String JSON_VALIDO = """
            {"codigo":"PRD-100","nombre":"Mouse","descripcion":"RGB","categoria":"Accesorios",
             "precio":9990,"stock":5,"activo":true}
            """;

    private static ProductoResponse response(Integer id) {
        return new ProductoResponse(id, "PRD-100", "Mouse", "RGB", "Accesorios",
                new BigDecimal("9990"), 5, true, LocalDateTime.now());
    }

    @Test
    void listar() throws Exception {
        when(service.listar(null)).thenReturn(List.of(response(1), response(2)));

        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("PRD-100"));
    }

    @Test
    void listarPorCategoria() throws Exception {
        when(service.listar("Audio")).thenReturn(List.of());

        mvc.perform(get("/api/productos").param("categoria", "Audio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        verify(service).listar("Audio");
    }

    @Test
    void obtener() throws Exception {
        when(service.obtener(1)).thenReturn(response(1));

        mvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void obtenerInexistenteDevuelve404() throws Exception {
        when(service.obtener(99)).thenThrow(new ProductoNotFoundException(99));

        mvc.perform(get("/api/productos/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("99")));
    }

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        when(service.crear(any(ProductoRequest.class))).thenReturn(response(10));

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/productos/10")))
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void crearInvalidoDevuelve400ConErrores() throws Exception {
        String invalido = """
                {"codigo":"","nombre":"Mouse","categoria":"Accesorios","precio":-1,"stock":-5}
                """;

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.codigo").exists())
                .andExpect(jsonPath("$.errores.precio").exists())
                .andExpect(jsonPath("$.errores.stock").exists());
        verifyNoInteractions(service);
    }

    @Test
    void crearDuplicadoDevuelve409() throws Exception {
        when(service.crear(any())).thenThrow(new CodigoDuplicadoException("PRD-100"));

        mvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isConflict());
    }

    @Test
    void actualizar() throws Exception {
        when(service.actualizar(eq(1), any())).thenReturn(response(1));

        mvc.perform(put("/api/productos/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void eliminarDevuelve204() throws Exception {
        mvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
        verify(service).eliminar(1);
    }

    @Test
    void eliminarInexistenteDevuelve404() throws Exception {
        doThrow(new ProductoNotFoundException(8)).when(service).eliminar(8);

        mvc.perform(delete("/api/productos/8"))
                .andExpect(status().isNotFound());
    }
}
