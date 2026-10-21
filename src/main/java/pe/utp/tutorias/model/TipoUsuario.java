package pe.utp.tutorias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla de roles: Tutor, Alumno o Administrador.
 * La cree para no repetir el texto del rol en cada usuario.
 */
@Entity
@Table(name = "tipo_usuario")
@Getter
@Setter
@NoArgsConstructor
public class TipoUsuario {

    public static final String TUTOR = "Tutor";
    public static final String ALUMNO = "Alumno";
    public static final String ADMINISTRADOR = "Administrador";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo")
    private Integer idTipo;

    @Column(name = "nombre_tipo", nullable = false, length = 30, unique = true)
    private String nombreTipo;

    public TipoUsuario(String nombreTipo) {
        this.nombreTipo = nombreTipo;
    }

    public Integer getIdTipo() { return idTipo; }
    public void setIdTipo(Integer idTipo) { this.idTipo = idTipo; }
    public String getNombreTipo() { return nombreTipo; }
    public void setNombreTipo(String nombreTipo) { this.nombreTipo = nombreTipo; }
}
