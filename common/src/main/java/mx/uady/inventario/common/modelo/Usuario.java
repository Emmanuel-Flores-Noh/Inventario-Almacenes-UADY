package mx.uady.inventario.common.modelo;

import java.util.Objects;

/**
 * Modelo de un usuario en la base de datos
 * @param idUsuario El id del usuario
 * @param idRol El id del rol del usuario
 * @param nombres Los nombres del usuario
 * @param apellidoPaterno Apellido paterno del usuario
 * @param apellidoMaterno Apellido materno del usuario
 */
public record Usuario(int idUsuario, int idRol, String nombres, String apellidoPaterno, String apellidoMaterno) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario usuario)) {
            return false;
        }
        return idUsuario == usuario.idUsuario()
                && idRol == usuario.idRol()
                && Objects.equals(nombres, usuario.nombres())
                && Objects.equals(apellidoPaterno, usuario.apellidoPaterno())
                && Objects.equals(apellidoMaterno, usuario.apellidoMaterno());
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idRol, nombres, apellidoPaterno, apellidoMaterno);
    }

    @Override
    public String toString() {
        return "Usuario{idUsuario=" + idUsuario + ", idRol=" + idRol + ", nombres='" + nombres + "'}";
    }
}
