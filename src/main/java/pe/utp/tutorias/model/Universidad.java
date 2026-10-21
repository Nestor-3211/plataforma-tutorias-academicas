package pe.utp.tutorias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Universidad de la que viene el usuario.
 * La agregue cuando decidimos que la plataforma no sea solo para la UTP.
 */
@Entity
@Table(name = "universidad")
@Getter
@Setter
@NoArgsConstructor
public class Universidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_universidad")
    private Integer idUniversidad;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 20, unique = true)
    private String codigo;

    @Column(length = 60)
    private String pais;

    public Universidad(String nombre, String codigo, String pais) {
        this.nombre = nombre;
        this.codigo = codigo;
        this.pais = pais;
    }

    public Integer getIdUniversidad() { return idUniversidad; }
    public void setIdUniversidad(Integer idUniversidad) { this.idUniversidad = idUniversidad; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getPais() { return pais; }
    public void setPais(String pais) { this.pais = pais; }
}
