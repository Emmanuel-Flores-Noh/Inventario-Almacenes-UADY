package mx.uady.inventario.common.modelo;

import java.util.Objects;

/**
 * Modelo de un rol de un usuario de la base de datos
 * @param idRol El id del rol
 * @param nombreRol El nombre del rol
 * @param descripcion La descripcion del rol
 */
public record Rol(int idRol, String nombreRol, String descripcion) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Rol rol)) {
            return false;
        }
        return idRol == rol.idRol()
                && Objects.equals(nombreRol, rol.nombreRol())
                && Objects.equals(descripcion, rol.descripcion());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idRol, nombreRol, descripcion);
    }

    @Override
    public String toString() {
        return "Rol{idRol=" + idRol + ", nombreRol='" + nombreRol + "', descripcion='" + descripcion + "'}";
    }
}
