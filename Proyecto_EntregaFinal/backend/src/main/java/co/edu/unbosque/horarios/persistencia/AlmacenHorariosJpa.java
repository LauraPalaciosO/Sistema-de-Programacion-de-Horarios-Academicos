package co.edu.unbosque.horarios.persistencia;

import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.persistencia.entidad.Asignacion;
import co.edu.unbosque.horarios.persistencia.entidad.Aula;
import co.edu.unbosque.horarios.persistencia.entidad.EstadoHorario;
import co.edu.unbosque.horarios.persistencia.entidad.Franja;
import co.edu.unbosque.horarios.persistencia.entidad.Horario;
import co.edu.unbosque.horarios.persistencia.entidad.Seccion;
import co.edu.unbosque.horarios.persistencia.repositorio.AulaRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.FranjaRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.HorarioRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.SeccionRepository;
import co.edu.unbosque.horarios.servicio.AlmacenHorarios;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Guarda y recupera horarios (Horario + Asignacion) en la base de datos H2. */
@Component
public class AlmacenHorariosJpa implements AlmacenHorarios {

    private static final String PERIODO = "2026-2";

    private final HorarioRepository horarios;
    private final SeccionRepository secciones;
    private final FranjaRepository franjas;
    private final AulaRepository aulas;

    public AlmacenHorariosJpa(HorarioRepository horarios, SeccionRepository secciones,
                              FranjaRepository franjas, AulaRepository aulas) {
        this.horarios = horarios;
        this.secciones = secciones;
        this.franjas = franjas;
        this.aulas = aulas;
    }

    @Override
    @Transactional
    public void guardar(String clave, String descripcion, boolean conConflictos, int[] franjaDe, int[] aulaDe, Instancia inst) {
        List<Seccion> ss = secciones.findAllByOrderByIdAsc();
        List<Franja> fs = franjas.findAllByOrderByIdAsc();
        List<Aula> as = aulas.findAllByOrderByIdAsc();
        List<Asignacion> nuevas = new ArrayList<>();
        for (Sesion s : inst.sesiones()) {
            int f = s.id() < franjaDe.length ? franjaDe[s.id()] : -1;
            if (f < 0) continue;
            nuevas.add(new Asignacion(ss.get(s.seccion().id()), s.indice(), fs.get(f), as.get(aulaDe[s.id()])));
        }
        Horario h = horarios.findByClave(clave).orElseGet(() -> new Horario(clave, descripcion, PERIODO));
        h.setDescripcion(descripcion);
        h.setGeneradoEn(LocalDateTime.now());
        h.setEstado(conConflictos ? EstadoHorario.CON_CONFLICTOS : EstadoHorario.SIN_CONFLICTOS);
        h.reemplazarAsignaciones(nuevas);
        horarios.save(h);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<HorarioGuardado> obtener(String clave, Instancia inst) {
        Optional<Horario> oh = horarios.findByClave(clave);
        if (oh.isEmpty()) return Optional.empty();
        Map<Long, Integer> idxSeccion = indices(secciones.findAllByOrderByIdAsc().stream().map(Seccion::getId).toList());
        Map<Long, Integer> idxFranja = indices(franjas.findAllByOrderByIdAsc().stream().map(Franja::getId).toList());
        Map<Long, Integer> idxAula = indices(aulas.findAllByOrderByIdAsc().stream().map(Aula::getId).toList());

        Map<String, Integer> sesionPor = new HashMap<>();
        for (Sesion s : inst.sesiones()) sesionPor.put(s.seccion().id() + ":" + s.indice(), s.id());

        int n = inst.sesiones().size();
        int[] franjaDe = new int[n];
        int[] aulaDe = new int[n];
        Arrays.fill(franjaDe, -1);
        Arrays.fill(aulaDe, -1);
        Horario h = oh.get();
        for (Asignacion a : h.getAsignaciones()) {
            Integer sec = idxSeccion.get(a.getSeccion().getId());
            Integer s = sec == null ? null : sesionPor.get(sec + ":" + a.getIndiceSesion());
            if (s == null) continue;
            franjaDe[s] = idxFranja.get(a.getFranja().getId());
            aulaDe[s] = idxAula.get(a.getAula().getId());
        }
        return Optional.of(new HorarioGuardado(h.getClave(), h.getDescripcion(), h.getGeneradoEn(), franjaDe, aulaDe));
    }

    @Override
    @Transactional
    public void borrarTodos() {
        horarios.deleteAll();
        horarios.flush();
    }

    private static Map<Long, Integer> indices(List<Long> ids) {
        Map<Long, Integer> m = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) m.put(ids.get(i), i);
        return m;
    }
}
