package cl.unab.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Datos para crear o actualizar un producto")
public record ProductoRequest(

        @Schema(example = "PRD-100")
        @NotBlank @Size(max = 20)
        String codigo,

        @Schema(example = "Mouse Gamer Logitech G203")
        @NotBlank @Size(max = 100)
        String nombre,

        @Schema(example = "Mouse RGB 8000 DPI")
        @Size(max = 255)
        String descripcion,

        @Schema(example = "Accesorios")
        @NotBlank @Size(max = 50)
        String categoria,

        @Schema(example = "24990")
        @NotNull @DecimalMin(value = "0.0") @Digits(integer = 10, fraction = 2)
        BigDecimal precio,

        @Schema(example = "50")
        @NotNull @Min(0)
        Integer stock,

        @Schema(example = "true", description = "Si se omite, se asume true")
        Boolean activo
) {
}
