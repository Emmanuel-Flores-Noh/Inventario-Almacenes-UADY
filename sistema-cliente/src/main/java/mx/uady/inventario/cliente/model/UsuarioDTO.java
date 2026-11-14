package mx.uady.inventario.cliente.model;

/**
 * Objeto de transferencia de datos (DTO) para representar a un usuario 
 * con rol de proveedor en las opciones de la interfaz de ventas.
 */
public class UsuarioDTO {
    private Long idUsuario;
    private String nombres;
    private String apellidoPaterno;

    /**
     * Obtiene el identificador único del usuario.
     * 
     * @return El ID del usuario.
     */
    public Long getIdUsuario() {
        return idUsuario;
    }

    /**
     * Obtiene los nombres del usuario.
     * 
     * @return Los nombres del usuario.
     */
    public String getNombres() {
        return nombres;
    }
    /**
     * Obtiene el apellido paterno del usuario.
     * 
     * @return El apellido paterno.
     */
    public String getApellidoPaterno() {
        return apellidoPaterno;
    }

    /**
     * Representación en cadena para mostrar el usuario amigablemente en el componente {@link javax.swing.JComboBox}.
     * 
     * @return Nombre completo formateado del proveedor.
     */
    @Override
    public String toString() {
        String n = nombres != null ? nombres : "";
        String ap = apellidoPaterno != null ? apellidoPaterno : "";
        return (n + " " + ap).trim();
    }
}