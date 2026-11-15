package cl.unab.productos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Reporte de productos con stock bajo")
public record AlertaStockResponse(
        @Schema(description = "Se alertan los productos con stock menor o igual a este valor", example = "10")
        int stockMinimo,
        @Schema(example = "3")
        int totalAlertas,
        List<ProductoAlerta> productos
) {

    @Schema(description = "Producto con stock bajo")
    public record ProductoAlerta(
            Integer id,
            String codigo,
            String nombre,
            String categoria,
            Integer stock,
            NivelAlerta nivel,
            @Schema(description = "Unidades a reponer para llegar al stock mínimo", example = "7")
            int unidadesSugeridas
    ) {
    }
}
