package cl.unab.productos.service;

import cl.unab.productos.dto.AlertaStockResponse;
import cl.unab.productos.dto.NivelAlerta;
import cl.unab.productos.dto.ProductoRequest;
import cl.unab.productos.dto.ProductoResponse;
import cl.unab.productos.entity.Producto;
import cl.unab.productos.exception.CodigoDuplicadoException;
import cl.unab.productos.exception.ProductoNotFoundException;
import cl.unab.productos.repository.ProductoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductoService {

    /** Regla de negocio: un producto con stock menor a este valor genera alerta. */
    public static final int STOCK_MINIMO = 10;

    /** Regla de negocio: con stock menor o igual a este valor la alerta es CRITICA. */
    public static final int STOCK_CRITICO = 5;

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    /**
     * Alerta de stock bajo.
     * Reglas de negocio:
     *  - Se alertan productos ACTIVOS con stock menor a 10 (los inactivos no se reponen).
     *  - Se ordenan del más urgente (menor stock) al menos urgente.
     *  - Nivel: SIN_STOCK (0), CRITICO (1 a 5), BAJO (6 a 9).
     *  - Se sugiere reponer lo necesario para llegar a 10.
     */
    @Transactional(readOnly = true)
    public AlertaStockResponse alertasStockBajo() {
        List<AlertaStockResponse.ProductoAlerta> alertas = repository
                .findByActivoTrueAndStockLessThanOrderByStockAscIdAsc(STOCK_MINIMO)
                .stream()
                .map(p -> new AlertaStockResponse.ProductoAlerta(
                        p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria(), p.getStock(),
                        clasificar(p.getStock()),
                        STOCK_MINIMO - p.getStock()))
                .toList();
        return new AlertaStockResponse(STOCK_MINIMO, alertas.size(), alertas);
    }

    static NivelAlerta clasificar(int stock) {
        if (stock == 0) return NivelAlerta.SIN_STOCK;
        if (stock <= STOCK_CRITICO) return NivelAlerta.CRITICO;
        return NivelAlerta.BAJO;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String categoria) {
        List<Producto> productos = (categoria == null || categoria.isBlank())
                ? repository.findAll(Sort.by("id"))
                : repository.findByCategoriaIgnoreCaseOrderByIdAsc(categoria);
        return productos.stream().map(ProductoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Integer id) {
        return ProductoResponse.from(buscar(id));
    }

    public ProductoResponse crear(ProductoRequest req) {
        if (repository.existsByCodigo(req.codigo())) {
            throw new CodigoDuplicadoException(req.codigo());
        }
        Producto p = new Producto();
        copiar(req, p);
        return ProductoResponse.from(repository.save(p));
    }

    public ProductoResponse actualizar(Integer id, ProductoRequest req) {
        Producto p = buscar(id);
        if (repository.existsByCodigoAndIdNot(req.codigo(), id)) {
            throw new CodigoDuplicadoException(req.codigo());
        }
        copiar(req, p);
        return ProductoResponse.from(repository.save(p));
    }

    public void eliminar(Integer id) {
        repository.delete(buscar(id));
    }

    private Producto buscar(Integer id) {
        return repository.findById(id).orElseThrow(() -> new ProductoNotFoundException(id));
    }

    private static void copiar(ProductoRequest req, Producto p) {
        p.setCodigo(req.codigo());
        p.setNombre(req.nombre());
        p.setDescripcion(req.descripcion());
        p.setCategoria(req.categoria());
        p.setPrecio(req.precio());
        p.setStock(req.stock());
        p.setActivo(req.activo() == null ? Boolean.TRUE : req.activo());
    }
}
