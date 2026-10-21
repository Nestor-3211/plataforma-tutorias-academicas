package pe.utp.tutorias.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String correo;

    // Nunca se guarda en texto plano: ver PasswordEncoder (BCrypt) en SecurityConfig.
    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_universidad")
    private Universidad universidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo", nullable = false)
    private TipoUsuario tipoUsuario;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    public boolean esTutor() {
        return tipoUsuario != null && TipoUsuario.TUTOR.equals(tipoUsuario.getNombreTipo());
    }

    public boolean esAdmin() {
        return tipoUsuario != null && TipoUsuario.ADMINISTRADOR.equals(tipoUsuario.getNombreTipo());
    }
}
