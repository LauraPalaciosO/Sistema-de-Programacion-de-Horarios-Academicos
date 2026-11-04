package co.edu.unbosque.horarios.persistencia;

import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Parametros;
import co.edu.unbosque.horarios.persistencia.entidad.Asignatura;
import co.edu.unbosque.horarios.persistencia.entidad.Aula;
import co.edu.unbosque.horarios.persistencia.entidad.DiaSemana;
import co.edu.unbosque.horarios.persistencia.entidad.Docente;
import co.edu.unbosque.horarios.persistencia.entidad.Franja;
import co.edu.unbosque.horarios.persistencia.entidad.ParametrosInstitucionales;
import co.edu.unbosque.horarios.persistencia.entidad.Seccion;
import co.edu.unbosque.horarios.persistencia.repositorio.AsignaturaRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.AulaRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.DocenteRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.FranjaRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.ParametrosRepository;
import co.edu.unbosque.horarios.persistencia.repositorio.SeccionRepository;
import co.edu.unbosque.horarios.servicio.FuenteInstancia;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Lee de la base de datos H2 las entidades y las convierte en el modelo del motor (y al revés). */
@Component
public class FuenteInstanciaJpa implements FuenteInstancia {

    private final FranjaRepository franjas;
    private final AulaRepository aulas;
    private final DocenteRepository docentes;
    private final AsignaturaRepository asignaturas;
    private final SeccionRepository secciones;
    private final ParametrosRepository parametros;

    public FuenteInstanciaJpa(FranjaRepository franjas, AulaRepository aulas, DocenteRepository docentes,
                              AsignaturaRepository asignaturas, SeccionRepository secciones, ParametrosRepository parametros) {
        this.franjas = franjas;
        this.aulas = aulas;
        this.docentes = docentes;
        this.asignaturas = asignaturas;
        this.secciones = secciones;
        this.parametros = parametros;
    }

    @Override
    @Transactional(readOnly = true)
    public Instancia cargar() {
        List<Franja> fs = franjas.findAllByOrderByIdAsc();
        Map<Long, Integer> idxFranja = new HashMap<>();
        List<co.edu.unbosque.horarios.motor.dominio.Franja> mFranjas = new ArrayList<>();
        for (Franja f : fs) {
            idxFranja.put(f.getId(), mFranjas.size());
            mFranjas.add(new co.edu.unbosque.horarios.motor.dominio.Franja(mFranjas.size(), f.getDia().ordinal(),
                    f.getInicio().getHour(), f.getFin().getHour()));
        }

        List<co.edu.unbosque.horarios.motor.dominio.Aula> mAulas = new ArrayList<>();
        for (Aula a : aulas.findAllByOrderByIdAsc()) {
            mAulas.add(new co.edu.unbosque.horarios.motor.dominio.Aula(mAulas.size(), a.getCodigo(), a.getCapacidad(),
                    Set.copyOf(a.getCaracteristicas())));
        }

        Map<Long, co.edu.unbosque.horarios.motor.dominio.Asignatura> asigPorId = new HashMap<>();
        List<co.edu.unbosque.horarios.motor.dominio.Asignatura> mAsignaturas = new ArrayList<>();
        for (Asignatura a : asignaturas.findAllByOrderByIdAsc()) {
            var ma = new co.edu.unbosque.horarios.motor.dominio.Asignatura(mAsignaturas.size(), a.getCodigo(), a.getNombre(),
                    a.getSemestre(), a.getCreditos(), a.getSesionesSemanales(), Set.copyOf(a.getRequisitosAula()));
            mAsignaturas.add(ma);
            asigPorId.put(a.getId(), ma);
        }

        Map<Long, co.edu.unbosque.horarios.motor.dominio.Docente> docPorId = new HashMap<>();
        List<co.edu.unbosque.horarios.motor.dominio.Docente> mDocentes = new ArrayList<>();
        for (Docente d : docentes.findAllByOrderByIdAsc()) {
            Set<Integer> disp = new HashSet<>();
            for (Franja f : d.getDisponibilidad()) disp.add(idxFranja.get(f.getId()));
            Set<Integer> pref = new HashSet<>();
            for (Franja f : d.getPreferidas()) pref.add(idxFranja.get(f.getId()));
            Set<Integer> hab = new HashSet<>();
            for (Asignatura a : d.getAsignaturasHabilitadas()) hab.add(asigPorId.get(a.getId()).id());
            var md = new co.edu.unbosque.horarios.motor.dominio.Docente(mDocentes.size(), d.getCodigo(), d.getNombre(),
                    Set.copyOf(disp), Set.copyOf(pref), Set.copyOf(hab));
            mDocentes.add(md);
            docPorId.put(d.getId(), md);
        }

        List<co.edu.unbosque.horarios.motor.dominio.Seccion> mSecciones = new ArrayList<>();
        for (Seccion s : secciones.findAllByOrderByIdAsc()) {
            mSecciones.add(new co.edu.unbosque.horarios.motor.dominio.Seccion(mSecciones.size(), s.getCodigo(), s.getGrupo(),
                    s.getEstudiantes(), asigPorId.get(s.getAsignatura().getId()), docPorId.get(s.getDocente().getId())));
        }

        ParametrosInstitucionales p = parametros.findFirstByOrderByIdAsc().orElseGet(ParametrosInstitucionales::new);
        Parametros mp = new Parametros(p.getTamanoObjetivo(), p.getTolerancia(), p.getMinimoEstudiantes(),
                p.getSesionesMin(), p.getSesionesMax(), p.isDiasNoConsecutivos());
        return new Instancia(mFranjas, mAulas, mDocentes, mAsignaturas, mSecciones, mp);
    }

    @Override
    @Transactional
    public void guardarParametros(Parametros p) {
        ParametrosInstitucionales e = parametros.findFirstByOrderByIdAsc().orElseGet(ParametrosInstitucionales::new);
        e.setTamanoObjetivo(p.tamanoObjetivo());
        e.setTolerancia(p.tolerancia());
        e.setMinimoEstudiantes(p.minimoEstudiantes());
        e.setSesionesMin(p.sesionesMin());
        e.setSesionesMax(p.sesionesMax());
        e.setDiasNoConsecutivos(p.diasNoConsecutivos());
        parametros.save(e);
    }

    /**
     * Borra los datos del periodo y guarda los de la instancia (datos de prueba).
     * Los horarios deben haberse borrado antes, porque sus asignaciones apuntan a las secciones.
     */
    @Override
    @Transactional
    public void reemplazar(Instancia inst) {
        secciones.deleteAll();
        docentes.deleteAll();
        asignaturas.deleteAll();
        aulas.deleteAll();
        franjas.deleteAll();
        parametros.deleteAll();
        secciones.flush();

        List<Franja> fs = new ArrayList<>();
        for (var f : inst.franjas()) {
            fs.add(franjas.save(new Franja(DiaSemana.values()[f.dia()], LocalTime.of(f.horaInicio(), 0), LocalTime.of(f.horaFin(), 0))));
        }
        for (var a : inst.aulas()) aulas.save(new Aula(a.codigo(), a.capacidad(), a.caracteristicas()));
        List<Asignatura> as = new ArrayList<>();
        for (var a : inst.asignaturas()) {
            as.add(asignaturas.save(new Asignatura(a.codigo(), a.nombre(), a.semestre(), a.creditos(),
                    a.sesionesSemanales(), a.requisitosAula())));
        }
        List<Docente> ds = new ArrayList<>();
        for (var d : inst.docentes()) {
            Docente e = new Docente(d.codigo(), d.nombre());
            for (int f : d.disponibles()) e.getDisponibilidad().add(fs.get(f));
            for (int f : d.preferidas()) e.getPreferidas().add(fs.get(f));
            for (int a : d.habilitadas()) e.getAsignaturasHabilitadas().add(as.get(a));
            ds.add(docentes.save(e));
        }
        for (var s : inst.secciones()) {
            secciones.save(new Seccion(s.codigo(), s.grupo(), s.estudiantes(), as.get(s.asignatura().id()), ds.get(s.docente().id())));
        }
        ParametrosInstitucionales p = new ParametrosInstitucionales();
        p.setTamanoObjetivo(inst.parametros().tamanoObjetivo());
        p.setTolerancia(inst.parametros().tolerancia());
        p.setMinimoEstudiantes(inst.parametros().minimoEstudiantes());
        p.setSesionesMin(inst.parametros().sesionesMin());
        p.setSesionesMax(inst.parametros().sesionesMax());
        p.setDiasNoConsecutivos(inst.parametros().diasNoConsecutivos());
        parametros.save(p);
    }
}
