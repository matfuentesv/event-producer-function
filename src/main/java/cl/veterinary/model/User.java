package cl.veterinary.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {


    private Long id;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String rut;
    private String direccion;
    private String celular;
    private String email;
    private String password;
    private Boolean activo;
    private Rol rol;

}
