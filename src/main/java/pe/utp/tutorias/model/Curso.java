package pe.utp.tutorias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Curso que dicta un tutor (ej. "Programacion", "Calculo 1").
 * Lo separe en su propia tabla para no repetir el nombre del curso
 * en cada perfil de tutor.
 */
@Entity
@Table(name = "curso")
@Getter
@Setter
@NoArgsConstructor
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_curso")
    private Integer idCurso;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 80)
    private String area;

    public Curso(String nombre, String area) {
        this.nombre = nombre;
        this.area = area;
    }

    public Integer getIdCurso() { return idCurso; }
    public void setIdCurso(Integer idCurso) { this.idCurso = idCurso; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
}
