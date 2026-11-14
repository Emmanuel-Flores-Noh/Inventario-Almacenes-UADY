package mx.uady.inventario.central.service.jdbc;

import java.util.List;

import mx.uady.inventario.central.model.VentaHistorial;
import mx.uady.inventario.central.repository.CatalogoRepository;
import mx.uady.inventario.central.repository.ProveedorConexion;
import mx.uady.inventario.central.repository.VentaRepository;
import mx.uady.inventario.central.service.ConsultasVentaService;
import mx.uady.inventario.common.modelo.TiendaAlmacen;
import mx.uady.inventario.common.modelo.Usuario;

/** {@link ConsultasVentaService} sobre MariaDB. */
public final class ConsultasVentaJdbc implements ConsultasVentaService {

    private final Transacciones transacciones;
    private final VentaRepository ventas;
    private final CatalogoRepository catalogo = new CatalogoRepository();

    public ConsultasVentaJdbc(ProveedorConexion conexiones) {
        this.transacciones = new Transacciones(conexiones);
        this.ventas = new VentaRepository(conexiones);
    }

    @Override
    public List<VentaHistorial> listarHistorial() {
        return transacciones.leer("consultar el historial de ventas", ventas::listarHistorial);
    }

    @Override
    public List<TiendaAlmacen> listarTiendas() {
        return transacciones.leer("consultar las tiendas", catalogo::listarTiendas);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return transacciones.leer("consultar los usuarios", catalogo::listarUsuarios);
    }

    /**
     * Lista los usuarios con rol {@link ConsultasVentaService#ROL_PROVEEDOR} desde MariaDB.
     *
     * @return usuarios proveedores ordenados por nombre
     * @throws mx.uady.inventario.central.service.AccesoDatosException si falla la base de datos
     */
    @Override
    public List<Usuario> listarUsuariosProveedores() {
        return transacciones.leer("consultar los usuarios proveedores",
                c -> catalogo.listarUsuariosPorRol(c, ROL_PROVEEDOR));
    }
}
