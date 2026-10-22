package pe.utp.tutorias.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.utp.tutorias.dao.PagoDAO;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.dto.FilaConteo;
import pe.utp.tutorias.dto.KpiDashboard;
import pe.utp.tutorias.model.Curso;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.Pago;
import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.model.Usuario;
import pe.utp.tutorias.service.impl.DashboardServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Pruebas del calculo de KPIs del dashboard. Se arma un escenario chico con
 * numeros faciles de comprobar a mano.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private ReservaDAO reservaDAO;
    @Mock
    private PagoDAO pagoDAO;
    @Mock
    private PerfilTutorDAO perfilTutorDAO;

    private DashboardServiceImpl servicio;

    private PerfilTutor perfilAna;
    private PerfilTutor perfilLuis;

    @BeforeEach
    void preparar() {
        servicio = new DashboardServiceImpl(reservaDAO, pagoDAO, perfilTutorDAO);
        perfilAna = perfil(1, "Ana Torres", "Matematica I");
        perfilLuis = perfil(2, "Luis Rojas", "Fisica I");
    }

    private PerfilTutor perfil(int idUsuario, String nombre, String curso) {
        Usuario u = new Usuario();
        u.setIdUsuario(idUsuario);
        u.setNombre(nombre);
        PerfilTutor p = new PerfilTutor();
        p.setIdPerfil(idUsuario);
        p.setUsuario(u);
        p.setCurso(new Curso(curso, "Ciencias"));
        return p;
    }

    private Reserva reserva(PerfilTutor perfil, EstadoReserva estado, LocalDateTime horario) {
        Reserva r = new Reserva();
        r.setPerfilTutor(perfil);
        r.setEstado(estado);
        r.setHorario(horario);
        return r;
    }

    private Pago pago(String total, String comision) {
        Pago p = new Pago();
        p.setMontoTotal(new BigDecimal(total));
        p.setComision(new BigDecimal(comision));
        return p;
    }

    private void datos(List<Reserva> reservas, List<Pago> pagos, List<PerfilTutor> perfiles) {
        when(reservaDAO.findAll()).thenReturn(reservas);
        when(pagoDAO.findAll()).thenReturn(pagos);
        when(perfilTutorDAO.findAll()).thenReturn(perfiles);
    }

    @Test
    void sinDatosTodoQuedaEnCeroYLosPromediosSonNulos() {
        datos(List.of(), List.of(), List.of());

        KpiDashboard kpi = servicio.calcularKpis();

        assertEquals(0, kpi.totalReservas());
        assertNull(kpi.tasaCompletadas());
        assertNull(kpi.calificacionPromedio());
        assertEquals(BigDecimal.ZERO, kpi.comisionTotal());
        assertTrue(kpi.topTutores().isEmpty());
        assertTrue(kpi.sesionesPorSemana().isEmpty());
    }

    @Test
    void cuentaLasReservasPorEstado() {
        LocalDateTime f = LocalDateTime.of(2026, 10, 14, 10, 0);
        datos(List.of(
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f),
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f.plusDays(1)),
                        reserva(perfilLuis, EstadoReserva.CONFIRMADA, f),
                        reserva(perfilLuis, EstadoReserva.PENDIENTE, f),
                        reserva(perfilLuis, EstadoReserva.CANCELADA, f)),
                List.of(), List.of(perfilAna, perfilLuis));

        KpiDashboard kpi = servicio.calcularKpis();

        assertEquals(5, kpi.totalReservas());
        assertEquals(2, kpi.completadas());
        assertEquals(1, kpi.confirmadas());
        assertEquals(1, kpi.pendientes());
        assertEquals(1, kpi.canceladas());
    }

    @Test
    void laTasaComparaCompletadasContraCanceladasIgnorandoLasAbiertas() {
        LocalDateTime f = LocalDateTime.of(2026, 10, 14, 10, 0);
        List<Reserva> reservas = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            reservas.add(reserva(perfilAna, EstadoReserva.COMPLETADA, f));
        }
        reservas.add(reserva(perfilAna, EstadoReserva.CANCELADA, f));
        reservas.add(reserva(perfilAna, EstadoReserva.PENDIENTE, f));   // no entra en la tasa
        reservas.add(reserva(perfilAna, EstadoReserva.CONFIRMADA, f));  // no entra en la tasa
        datos(reservas, List.of(), List.of(perfilAna));

        // 3 / (3 + 1) = 75.0 %
        assertEquals(new BigDecimal("75.0"), servicio.calcularKpis().tasaCompletadas());
    }

    @Test
    void soloCuentaComoActivoAlTutorConAlMenosUnaSesionCompletada() {
        LocalDateTime f = LocalDateTime.of(2026, 10, 14, 10, 0);
        datos(List.of(
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f),
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f.plusDays(1)),  // mismo tutor: cuenta una vez
                        reserva(perfilLuis, EstadoReserva.PENDIENTE, f)),
                List.of(), List.of(perfilAna, perfilLuis));

        KpiDashboard kpi = servicio.calcularKpis();

        assertEquals(1, kpi.tutoresActivos());
        assertEquals(2, kpi.totalTutores());
    }

    @Test
    void laCalificacionPromedioIgnoraLosPerfilesSinCalificacion() {
        perfilAna.setCalificacionProm(new BigDecimal("4.00"));
        perfilLuis.setCalificacionProm(BigDecimal.ZERO);   // todavia sin resenas
        PerfilTutor perfilMaria = perfil(3, "Maria Diaz", "Quimica");
        perfilMaria.setCalificacionProm(new BigDecimal("5.00"));
        datos(List.of(), List.of(), List.of(perfilAna, perfilLuis, perfilMaria));

        // (4.00 + 5.00) / 2 = 4.50
        assertEquals(new BigDecimal("4.50"), servicio.calcularKpis().calificacionPromedio());
    }

    @Test
    void sumaLaComisionYElMontoProcesado() {
        datos(List.of(), List.of(pago("80.00", "12.00"), pago("120.00", "18.00"), pago("50.00", "7.50")),
                List.of());

        KpiDashboard kpi = servicio.calcularKpis();

        assertEquals(new BigDecimal("37.50"), kpi.comisionTotal());
        assertEquals(new BigDecimal("250.00"), kpi.montoProcesado());
        assertEquals(3, kpi.pagosRegistrados());
    }

    @Test
    void losRankingsNoContanLasCanceladasYOrdenanDeMayorAMenor() {
        LocalDateTime f = LocalDateTime.of(2026, 10, 14, 10, 0);
        datos(List.of(
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f),
                        reserva(perfilLuis, EstadoReserva.COMPLETADA, f),
                        reserva(perfilLuis, EstadoReserva.PENDIENTE, f),
                        reserva(perfilAna, EstadoReserva.CANCELADA, f),
                        reserva(perfilAna, EstadoReserva.CANCELADA, f)),
                List.of(), List.of(perfilAna, perfilLuis));

        KpiDashboard kpi = servicio.calcularKpis();

        // Luis: 2 no canceladas; Ana: 1 (sus 2 canceladas no cuentan)
        assertEquals(List.of(new FilaConteo("Luis Rojas", 2), new FilaConteo("Ana Torres", 1)), kpi.topTutores());
        assertEquals(List.of(new FilaConteo("Fisica I", 2), new FilaConteo("Matematica I", 1)), kpi.topCursos());
    }

    @Test
    void enUnEmpateElRankingQuedaEnOrdenAlfabetico() {
        LocalDateTime f = LocalDateTime.of(2026, 10, 14, 10, 0);
        datos(List.of(
                        reserva(perfilLuis, EstadoReserva.COMPLETADA, f),
                        reserva(perfilAna, EstadoReserva.COMPLETADA, f)),
                List.of(), List.of(perfilAna, perfilLuis));

        List<FilaConteo> top = servicio.calcularKpis().topTutores();

        assertEquals("Ana Torres", top.get(0).nombre());
        assertEquals("Luis Rojas", top.get(1).nombre());
    }

    @Test
    void agrupaLasSesionesPorSemanaEmpezandoEnLunes() {
        // 12/10/2026 es lunes: lunes y domingo de esa semana van juntos; el lunes siguiente va aparte
        datos(List.of(
                        reserva(perfilAna, EstadoReserva.COMPLETADA, LocalDateTime.of(2026, 10, 12, 9, 0)),
                        reserva(perfilAna, EstadoReserva.COMPLETADA, LocalDateTime.of(2026, 10, 18, 9, 0)),
                        reserva(perfilAna, EstadoReserva.PENDIENTE, LocalDateTime.of(2026, 10, 19, 9, 0))),
                List.of(), List.of(perfilAna));

        List<FilaConteo> semanas = servicio.calcularKpis().sesionesPorSemana();

        assertEquals(List.of(new FilaConteo("12/10", 2), new FilaConteo("19/10", 1)), semanas);
    }

    @Test
    void muestraComoMaximoLasUltimas8Semanas() {
        List<Reserva> reservas = new ArrayList<>();
        LocalDateTime lunes = LocalDateTime.of(2026, 8, 3, 9, 0);   // lunes
        for (int i = 0; i < 10; i++) {
            reservas.add(reserva(perfilAna, EstadoReserva.PENDIENTE, lunes.plusWeeks(i)));
        }
        datos(reservas, List.of(), List.of(perfilAna));

        List<FilaConteo> semanas = servicio.calcularKpis().sesionesPorSemana();

        assertEquals(8, semanas.size());
        assertEquals("17/08", semanas.get(0).nombre());   // se descartaron las 2 mas antiguas (03/08 y 10/08)
        assertEquals("05/10", semanas.get(7).nombre());
    }
}
