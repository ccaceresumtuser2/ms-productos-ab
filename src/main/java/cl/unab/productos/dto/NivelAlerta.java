package cl.unab.productos.dto;

/** Nivel de una alerta de stock (la clasificación está en ProductoService). */
public enum NivelAlerta {
    /** stock 0 */
    SIN_STOCK,
    /** stock 1 a 5 */
    CRITICO,
    /** stock 6 a 10 */
    BAJO
}
