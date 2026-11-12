package mx.uady.inventario.central.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

class MercanciaTableModelTest {

    @Test
    void muestraNombreDelProductoEstadoLegibleYSiTieneQr() {
        MercanciaTableModel modelo = new MercanciaTableModel();
        modelo.setFilas(List.of(
                new Mercancia("SN-1", 1, EstadoMercancia.EN_ALMACEN, null),
                new Mercancia("SN-2", 9, EstadoMercancia.VENDIDO, "/qr/SN-2.png")),
                Map.of(1, "Laptop"));

        assertEquals("SN-1", modelo.getValueAt(0, MercanciaTableModel.COL_SERIE));
        assertEquals("Laptop", modelo.getValueAt(0, MercanciaTableModel.COL_PRODUCTO));
        assertEquals("En almacén", modelo.getValueAt(0, MercanciaTableModel.COL_ESTADO));
        assertEquals("Pendiente", modelo.getValueAt(0, MercanciaTableModel.COL_QR));

        assertEquals("Producto #9", modelo.getValueAt(1, MercanciaTableModel.COL_PRODUCTO));
        assertEquals("Vendido", modelo.getValueAt(1, MercanciaTableModel.COL_ESTADO));
        assertEquals("Generado", modelo.getValueAt(1, MercanciaTableModel.COL_QR));
    }

    @Test
    void actualizaLaFilaDeUnaUnidadSinDistinguirMayusculas() {
        MercanciaTableModel modelo = new MercanciaTableModel();
        modelo.setFilas(List.of(new Mercancia("SN-1", 1, EstadoMercancia.EN_ALMACEN, null)), Map.of());

        modelo.actualizar(new Mercancia("SN-1", 1, EstadoMercancia.EN_ALMACEN, "/qr/SN-1.png"));

        assertEquals("Generado", modelo.getValueAt(0, MercanciaTableModel.COL_QR));
        assertEquals(0, modelo.indiceDe("sn-1"));
        assertEquals(-1, modelo.indiceDe("SN-404"));
    }
}
