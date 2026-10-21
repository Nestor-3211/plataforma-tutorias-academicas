package pe.utp.tutorias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * El perfil que publica un tutor para UN curso especifico: su tarifa,
 * su disponibilidad y su calificacion promedio. Un mismo tutor puede
 * tener varios perfiles, uno por cada curso que dicta (por eso hay una
 * llave unica usuario+curso, para que no repita perfil en el mismo curso).
 */
@Entity
@Table(name = "perfil_tutor", uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_curso"}))
@Getter
@Setter
@NoArgsConstructor
public class PerfilTutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_perfil")
    private Integer idPerfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_curso", nullable = false)
    private Curso curso;

    @Column(name = "tarifa_hora", nullable = false, precision = 6, scale = 2)
    private BigDecimal tarifaHora;

    @Column(length = 255)
    private String disponibilidad;

    @Column(name = "calificacion_prom", precision = 3, scale = 2)
    private BigDecimal calificacionProm = BigDecimal.ZERO;

    public Integer getIdPerfil() { return idPerfil; }
    public void setIdPerfil(Integer idPerfil) { this.idPerfil = idPerfil; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public BigDecimal getTarifaHora() { return tarifaHora; }
    public void setTarifaHora(BigDecimal tarifaHora) { this.tarifaHora = tarifaHora; }
    public String getDisponibilidad() { return disponibilidad; }
    public void setDisponibilidad(String disponibilidad) { this.disponibilidad = disponibilidad; }
    public BigDecimal getCalificacionProm() { return calificacionProm; }
    public void setCalificacionProm(BigDecimal calificacionProm) { this.calificacionProm = calificacionProm; }
}
