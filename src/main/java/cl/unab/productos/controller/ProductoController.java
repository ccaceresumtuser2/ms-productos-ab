package cl.unab.productos.controller;

import cl.unab.productos.dto.AlertaStockResponse;
import cl.unab.productos.dto.ProductoRequest;
import cl.unab.productos.dto.ProductoResponse;
import cl.unab.productos.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "CRUD de productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos", description = "Lista todos los productos, opcionalmente filtrados por categoría")
    public List<ProductoResponse> listar(
            @Parameter(description = "Filtrar por categoría", example = "Audio")
            @RequestParam(required = false) String categoria) {
        return service.listar(categoria);
    }

    @GetMapping("/alertas/stock-bajo")
    @Operation(summary = "Alerta de stock bajo",
            description = "Productos activos con stock menor o igual a 10, ordenados del más urgente al menos urgente. "
                    + "Nivel: SIN_STOCK (0), CRITICO (1-5), BAJO (6-10).")
    @ApiResponse(responseCode = "200", description = "Reporte de alertas (puede venir vacío)")
    public AlertaStockResponse alertasStockBajo() {
        return service.alertasStockBajo();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductoResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crear producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Código duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest req) {
        ProductoResponse creado = service.crear(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Código duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductoResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ProductoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
