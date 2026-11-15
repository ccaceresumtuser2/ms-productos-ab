package cl.unab.productos.dto;

import cl.unab.productos.entity.Producto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Integer id,
        String codigo,
        String nombre,
        String descripcion,
        String categoria,
        BigDecimal precio,
        Integer stock,
        Boolean activo,
        LocalDateTime fechaCreacion
) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.getId(), p.getCodigo(), p.getNombre(), p.getDescripcion(),
                p.getCategoria(), p.getPrecio(), p.getStock(), p.getActivo(), p.getFechaCreacion());
    }
}
