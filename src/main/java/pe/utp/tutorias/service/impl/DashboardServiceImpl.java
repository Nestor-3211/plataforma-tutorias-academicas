package pe.utp.tutorias.service.impl;

import com.google.common.collect.EnumMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.TreeMultiset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utp.tutorias.dao.PagoDAO;
import pe.utp.tutorias.dao.PerfilTutorDAO;
import pe.utp.tutorias.dao.ReservaDAO;
import pe.utp.tutorias.dto.FilaConteo;
import pe.utp.tutorias.dto.KpiDashboard;
import pe.utp.tutorias.model.EstadoReserva;
import pe.utp.tutorias.model.Pago;
import pe.utp.tutorias.model.PerfilTutor;
import pe.utp.tutorias.model.Reserva;
import pe.utp.tutorias.service.DashboardService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Calcula los 5 KPIs del Lean Canvas y los rankings del administrador.
 *
 * Como son pocos datos, traigo las listas con findAll() y las cuento en
 * memoria con Guava (Multiset). Si algun dia hay muchos registros, estos
 * conteos se pueden pasar a consultas @Query con GROUP BY en los DAO.
 *
 * @Transactional(readOnly = true): las relaciones LAZY (reserva -> perfil ->
 * curso) se leen dentro de esta transaccion, sin depender de open-in-view.
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private static final int SEMANAS_A_MOSTRAR = 8;
    private static final int TAMANO_RANKING = 5;
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final ReservaDAO reservaDAO;
    private final PagoDAO pagoDAO;
    private final PerfilTutorDAO perfilTutorDAO;

    public DashboardServiceImpl(ReservaDAO reservaDAO, PagoDAO pagoDAO, PerfilTutorDAO perfilTutorDAO) {
        this.reservaDAO = reservaDAO;
        this.pagoDAO = pagoDAO;
        this.perfilTutorDAO = perfilTutorDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public KpiDashboard calcularKpis() {
        List<Reserva> reservas = reservaDAO.findAll();
        List<Pago> pagos = pagoDAO.findAll();
        List<PerfilTutor> perfiles = perfilTutorDAO.findAll();

        // ---- KPI 3: cuantas reservas hay en cada estado (EnumMultiset de Guava) ----
        Multiset<EstadoReserva> porEstado = EnumMultiset.create(EstadoReserva.class);
        for (Reserva r : reservas) {
            porEstado.add(r.getEstado());
        }
        int completadas = porEstado.count(EstadoReserva.COMPLETADA);
        int confirmadas = porEstado.count(EstadoReserva.CONFIRMADA);
        int pendientes = porEstado.count(EstadoReserva.PENDIENTE);
        int canceladas = porEstado.count(EstadoReserva.CANCELADA);

        // La tasa solo compara las sesiones "cerradas": completadas contra canceladas.
        BigDecimal tasaCompletadas = null;
        if (completadas + canceladas > 0) {
            tasaCompletadas = BigDecimal.valueOf(completadas).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(completadas + canceladas), 1, RoundingMode.HALF_UP);
        }

        // ---- KPI 1: sesiones reservadas por semana (semana de la fecha de la sesion) ----
        List<FilaConteo> sesionesPorSemana = contarPorSemana(reservas);

        // ---- KPI 2: tutores activos = tutores con al menos una sesion COMPLETADA ----
        Set<Integer> tutoresActivos = reservas.stream()
                .filter(r -> r.getEstado() == EstadoReserva.COMPLETADA)
                .map(r -> r.getPerfilTutor().getUsuario().getIdUsuario())
                .collect(Collectors.toSet());
        Set<Integer> tutoresConPerfil = perfiles.stream()
                .map(p -> p.getUsuario().getIdUsuario())
                .collect(Collectors.toSet());

        // ---- KPI 4: calificacion promedio (solo perfiles que ya tienen calificacion) ----
        List<BigDecimal> notas = perfiles.stream()
                .map(PerfilTutor::getCalificacionProm)
                .filter(n -> n != null && n.signum() > 0)
                .toList();
        BigDecimal calificacionPromedio = null;
        if (!notas.isEmpty()) {
            calificacionPromedio = sumar(notas).divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
        }

        // ---- KPI 5: ingreso por comision ----
        BigDecimal comisionTotal = sumar(pagos.stream().map(Pago::getComision).toList());
        BigDecimal montoProcesado = sumar(pagos.stream().map(Pago::getMontoTotal).toList());

        // ---- Rankings: tutores y cursos mas solicitados (sin contar las canceladas) ----
        TreeMultiset<String> pedidosPorTutor = TreeMultiset.create();
        TreeMultiset<String> pedidosPorCurso = TreeMultiset.create();
        for (Reserva r : reservas) {
            if (r.getEstado() != EstadoReserva.CANCELADA) {
                pedidosPorTutor.add(r.getPerfilTutor().getUsuario().getNombre());
                pedidosPorCurso.add(r.getPerfilTutor().getCurso().getNombre());
            }
        }

        KpiDashboard kpi = new KpiDashboard(
                reservas.size(), sesionesPorSemana,
                tutoresActivos.size(), tutoresConPerfil.size(),
                completadas, confirmadas, pendientes, canceladas, tasaCompletadas,
                calificacionPromedio,
                comisionTotal, montoProcesado, pagos.size(),
                ranking(pedidosPorTutor), ranking(pedidosPorCurso));
        log.info("KPIs calculados: {} reservas, {} pagos, comision total={}", reservas.size(), pagos.size(), comisionTotal);
        return kpi;
    }

    /**
     * Agrupa las reservas por semana (el lunes de la semana de la sesion).
     * TreeMultiset deja las semanas ordenadas de la mas antigua a la mas nueva;
     * me quedo con las ultimas SEMANAS_A_MOSTRAR.
     */
    private static List<FilaConteo> contarPorSemana(List<Reserva> reservas) {
        TreeMultiset<LocalDate> porSemana = TreeMultiset.create();
        for (Reserva r : reservas) {
            porSemana.add(r.getHorario().toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
        }
        List<FilaConteo> todas = porSemana.entrySet().stream()
                .map(e -> new FilaConteo(e.getElement().format(DIA_MES), e.getCount()))
                .toList();
        int desde = Math.max(0, todas.size() - SEMANAS_A_MOSTRAR);
        return todas.subList(desde, todas.size());
    }

    /**
     * Los mas pedidos primero. Guava copyHighestCountFirst ordena de mayor a
     * menor; como parto de un TreeMultiset (orden alfabetico), los empates
     * quedan en orden alfabetico y el ranking no cambia entre recargas.
     */
    private static List<FilaConteo> ranking(TreeMultiset<String> conteo) {
        return Multisets.copyHighestCountFirst(conteo).entrySet().stream()
                .limit(TAMANO_RANKING)
                .map(e -> new FilaConteo(e.getElement(), e.getCount()))
                .toList();
    }

    private static BigDecimal sumar(List<BigDecimal> valores) {
        return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
