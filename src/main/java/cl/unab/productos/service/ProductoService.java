package cl.unab.productos.service;

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

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
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
