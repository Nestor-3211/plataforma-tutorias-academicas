package pe.utp.tutorias.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.tutorias.dao.CursoDAO;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.model.Curso;
import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.PerfilTutorService;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Implementacion de PerfilTutorService.
 */
@Service
public class PerfilTutorServiceImpl implements PerfilTutorService {

    private final PerfilTutorDAO perfilTutorDAO;
    private final CursoDAO cursoDAO;

    public PerfilTutorServiceImpl(PerfilTutorDAO perfilTutorDAO, CursoDAO cursoDAO) {
        this.perfilTutorDAO = perfilTutorDAO;
        this.cursoDAO = cursoDAO;
    }

    @Override
    @Transactional
    public PerfilTutor publicarPerfil(Usuario tutor, Integer idCurso, BigDecimal tarifaHora, String disponibilidad) {
        if (!tutor.esTutor()) {
            throw new IllegalStateException("Solo un usuario de tipo Tutor puede publicar un perfil.");
        }
        if (tarifaHora == null || tarifaHora.signum() <= 0) {
            throw new IllegalArgumentException("La tarifa por hora debe ser mayor a 0.");
        }
        if (perfilTutorDAO.existsByUsuario_IdUsuarioAndCurso_IdCurso(tutor.getIdUsuario(), idCurso)) {
            throw new IllegalStateException("Ya tienes un perfil publicado para este curso.");
        }
        Curso curso = cursoDAO.findById(idCurso)
                .orElseThrow(() -> new NoSuchElementException("Curso no encontrado: " + idCurso));

        PerfilTutor perfil = new PerfilTutor();
        perfil.setUsuario(tutor);
        perfil.setCurso(curso);
        perfil.setTarifaHora(tarifaHora);
        perfil.setDisponibilidad(disponibilidad);
        return perfilTutorDAO.save(perfil);
    }

    @Override
    public List<PerfilTutor> buscar(String curso, BigDecimal tarifaMax, BigDecimal calificacionMin) {
        String filtroCurso = curso == null ? "" : curso.trim();
        return perfilTutorDAO.buscar(filtroCurso, tarifaMax, calificacionMin);
    }

    @Override
    public List<PerfilTutor> misPerfiles(Integer idUsuarioTutor) {
        return perfilTutorDAO.findByUsuario_IdUsuario(idUsuarioTutor);
    }

    @Override
    public PerfilTutor obtener(Integer idPerfil) {
        return perfilTutorDAO.findById(idPerfil)
                .orElseThrow(() -> new NoSuchElementException("Perfil de tutor no encontrado: " + idPerfil));
    }
}
