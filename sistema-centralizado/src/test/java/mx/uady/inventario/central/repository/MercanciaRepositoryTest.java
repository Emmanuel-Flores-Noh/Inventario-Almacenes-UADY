package mx.uady.inventario.central.repository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;

import org.junit.jupiter.api.Test;

class MercanciaRepositoryTest {

    @Test
    void reconoceLaClaveDuplicadaDeMariaDb() {
        assertTrue(MercanciaRepository.esClaveDuplicada(new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'SN-1' for key 'PRIMARY'", "23000", 1062)));
    }

    @Test
    void noConfundeOtrasViolacionesDeIntegridad() {
        // 1452: clave foránea inexistente; también usa SQLState 23000.
        assertFalse(MercanciaRepository.esClaveDuplicada(new SQLIntegrityConstraintViolationException(
                "Cannot add or update a child row", "23000", 1452)));
        assertFalse(MercanciaRepository.esClaveDuplicada(new SQLException("Socket fail", "08000")));
    }
}
