package cl.unab.productos.exception;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(Integer id) {
        super("Producto con id " + id + " no existe");
    }
}
