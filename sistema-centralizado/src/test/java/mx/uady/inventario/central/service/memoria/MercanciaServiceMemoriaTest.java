package mx.uady.inventario.central.service.memoria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import mx.uady.inventario.central.service.AccesoDatosException;
import mx.uady.inventario.central.service.NegocioException;
import mx.uady.inventario.central.service.SerieDuplicadaException;
import mx.uady.inventario.central.service.ValidacionException;
import mx.uady.inventario.common.modelo.EstadoMercancia;
import mx.uady.inventario.common.modelo.Mercancia;

class MercanciaServiceMemoriaTest {

    @TempDir
    Path directorioQr;

    private AlmacenMemoria almacen;
    private MercanciaServiceMemoria servicio;

    @BeforeEach
    void preparar() {
        almacen = AlmacenMemoria.conDatosDeEjemplo(Duration.ZERO);
        servicio = new MercanciaServiceMemoria(almacen, new QrServiceTemporal(directorioQr));
    }

    @Test
    void registraLaUnidadEnAlmacenConUnQrLegible() throws Exception {
        Mercancia unidad = servicio.registrar(2, "  SN-NUEVA-01 ");

        assertEquals("SN-NUEVA-01", unidad.numeroSerie());
        assertEquals(EstadoMercancia.EN_ALMACEN, unidad.estado());
        Path qr = Path.of(unidad.rutaQr());
        assertTrue(Files.size(qr) > 0);

        BufferedImage imagen = ImageIO.read(qr.toFile());
        String contenido = new QRCodeReader().decode(new BinaryBitmap(
                new HybridBinarizer(new BufferedImageLuminanceSource(imagen)))).getText();
        assertEquals("SN-NUEVA-01", contenido);
    }

    @Test
    void rechazaUnaSerieDuplicadaSinImportarMayusculas() {
        SerieDuplicadaException e = assertThrows(SerieDuplicadaException.class,
                () -> servicio.registrar(1, "sn-uady-0001"));
        assertEquals("sn-uady-0001", e.getNumeroSerie());
        assertEquals(10, servicio.listar(null, null).size());
    }

    @Test
    void rechazaSerieVaciaYProductoInexistente() {
        assertThrows(ValidacionException.class, () -> servicio.registrar(1, "   "));
        assertThrows(NegocioException.class, () -> servicio.registrar(99, "SN-X"));
    }

    @Test
    void siFallaElQrNoRegistraLaUnidad() {
        MercanciaServiceMemoria conQrRoto = new MercanciaServiceMemoria(almacen, serie -> {
            throw new AccesoDatosException("disco lleno", null);
        });

        assertThrows(AccesoDatosException.class, () -> conQrRoto.registrar(1, "SN-SIN-QR"));
        assertTrue(servicio.listar(null, null).stream().noneMatch(m -> m.numeroSerie().equals("SN-SIN-QR")));
    }

    @Test
    void filtraPorProductoYPorEstado() {
        assertEquals(2, servicio.listar(1, null).size());
        assertEquals(4, servicio.listar(null, EstadoMercancia.VENDIDO).size());
        assertEquals(6, servicio.listar(null, EstadoMercancia.EN_ALMACEN).size());
        assertEquals(1, servicio.listar(1, EstadoMercancia.VENDIDO).size());
        assertEquals("SN-UADY-0002", servicio.listar(1, EstadoMercancia.VENDIDO).get(0).numeroSerie());
    }

    @Test
    void elNombreDelArchivoQrEsSeguro() {
        assertEquals("SN_01_A.png", QrServiceTemporal.nombreArchivo("SN/01:A"));
    }
}
