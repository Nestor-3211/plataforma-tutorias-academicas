package pe.utp.tutorias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Consideracion de seguridad del silabo: las contrasenas nunca se
 * almacenan en texto plano. Se usa BCrypt (hash con salt incorporado).
 * Nota: este avance no incluye Spring Security completo (filtros de
 * autorizacion por URL); el control de acceso por rol se hace a nivel de
 * controlador/servicio mientras se completa el resto del proyecto.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
