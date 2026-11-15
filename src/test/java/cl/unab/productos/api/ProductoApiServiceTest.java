package cl.unab.productos.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de servicio (end-to-end): levanta el servidor HTTP real en un puerto aleatorio
 * y lo consume como lo haría un cliente externo, incluyendo la documentación Swagger.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProductoApiServiceTest {

    @Value("${local.server.port}")
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> { }) // no lanzar en 4xx/5xx
                .build();
    }

    private ResponseEntity<String> crear(String codigo) {
        String body = """
                {"codigo":"%s","nombre":"Teclado","categoria":"Accesorios","precio":29990,"stock":7}
                """.formatted(codigo);
        return client.post().uri("/api/productos")
                .contentType(MediaType.APPLICATION_JSON).body(body)
                .retrieve().toEntity(String.class);
    }

    @Test
    void crearYConsultarPorHttp() {
        String codigo = "API-" + UUID.randomUUID().toString().substring(0, 8);

        ResponseEntity<String> creado = crear(codigo);
        assertThat(creado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(creado.getHeaders().getLocation()).isNotNull();

        ResponseEntity<String> consultado = client.get().uri(creado.getHeaders().getLocation())
                .retrieve().toEntity(String.class);
        assertThat(consultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(consultado.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(consultado.getBody()).contains("\"codigo\":\"" + codigo + "\"");
    }

    @Test
    void errorDevuelveProblemDetail() {
        ResponseEntity<String> res = client.get().uri("/api/productos/987654")
                .retrieve().toEntity(String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getHeaders().getContentType().toString()).contains("application/problem+json");
        assertThat(res.getBody()).contains("no existe");
    }

    @Test
    void validacionDevuelve400() {
        ResponseEntity<String> res = client.post().uri("/api/productos")
                .contentType(MediaType.APPLICATION_JSON).body("{}")
                .retrieve().toEntity(String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody()).contains("errores", "nombre", "precio");
    }

    @Test
    void documentacionOpenApiDisponible() {
        ResponseEntity<String> docs = client.get().uri("/v3/api-docs").retrieve().toEntity(String.class);

        assertThat(docs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(docs.getBody()).contains("API Productos - UNAB", "/api/productos", "/api/productos/{id}");
    }

    @Test
    void healthCheckUp() {
        ResponseEntity<String> health = client.get().uri("/actuator/health").retrieve().toEntity(String.class);

        assertThat(health.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(health.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    void swaggerUiDisponible() {
        ResponseEntity<String> ui = client.get().uri("/swagger-ui/index.html").retrieve().toEntity(String.class);

        assertThat(ui.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ui.getBody()).containsIgnoringCase("swagger");
    }
}
