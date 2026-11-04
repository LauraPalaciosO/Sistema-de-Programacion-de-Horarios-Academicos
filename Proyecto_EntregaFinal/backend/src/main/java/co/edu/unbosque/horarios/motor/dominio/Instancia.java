package co.edu.unbosque.horarios.motor.dominio;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos de entrada del motor: franjas, aulas, docentes, asignaturas y secciones.
 * Es inmutable; las sesiones (clases semanales) se derivan de las secciones.
 * Los ids de cada elemento coinciden con su posición en la lista.
 */
public final class Instancia {

    private final List<Franja> franjas;
    private final List<Aula> aulas;
    private final List<Docente> docentes;
    private final List<Asignatura> asignaturas;
    private final List<Seccion> secciones;
    private final Parametros parametros;
    private final List<Sesion> sesiones;

    private final List<List<Franja>> franjasPorDia;
    private final Map<Integer, List<Sesion>> sesionesPorDocente = new LinkedHashMap<>();
    private final Map<String, List<Sesion>> sesionesPorGrupo = new LinkedHashMap<>();
    private final Map<Integer, List<Sesion>> sesionesPorSeccion = new LinkedHashMap<>();

    public Instancia(List<Franja> franjas, List<Aula> aulas, List<Docente> docentes,
                     List<Asignatura> asignaturas, List<Seccion> secciones, Parametros parametros) {
        this.franjas = List.copyOf(franjas);
        this.aulas = List.copyOf(aulas);
        this.docentes = List.copyOf(docentes);
        this.asignaturas = List.copyOf(asignaturas);
        this.secciones = List.copyOf(secciones);
        this.parametros = parametros;
        comprobarIds();

        List<Sesion> ss = new ArrayList<>();
        for (Seccion sec : secciones) {
            for (int i = 0; i < sec.asignatura().sesionesSemanales(); i++) {
                Sesion s = new Sesion(ss.size(), sec, i);
                ss.add(s);
                sesionesPorDocente.computeIfAbsent(sec.docente().id(), k -> new ArrayList<>()).add(s);
                sesionesPorGrupo.computeIfAbsent(sec.grupo(), k -> new ArrayList<>()).add(s);
                sesionesPorSeccion.computeIfAbsent(sec.id(), k -> new ArrayList<>()).add(s);
            }
        }
        this.sesiones = Collections.unmodifiableList(ss);

        List<List<Franja>> porDia = new ArrayList<>();
        for (int d = 0; d < Franja.DIAS.length; d++) porDia.add(new ArrayList<>());
        for (Franja f : franjas) porDia.get(f.dia()).add(f);
        this.franjasPorDia = porDia;
    }

    private void comprobarIds() {
        for (int i = 0; i < franjas.size(); i++) if (franjas.get(i).id() != i) throw new IllegalArgumentException("Franja con id fuera de orden");
        for (int i = 0; i < aulas.size(); i++) if (aulas.get(i).id() != i) throw new IllegalArgumentException("Aula con id fuera de orden");
        for (int i = 0; i < docentes.size(); i++) if (docentes.get(i).id() != i) throw new IllegalArgumentException("Docente con id fuera de orden");
        for (int i = 0; i < secciones.size(); i++) if (secciones.get(i).id() != i) throw new IllegalArgumentException("Sección con id fuera de orden");
    }

    public List<Franja> franjas() { return franjas; }
    public List<Aula> aulas() { return aulas; }
    public List<Docente> docentes() { return docentes; }
    public List<Asignatura> asignaturas() { return asignaturas; }
    public List<Seccion> secciones() { return secciones; }
    public List<Sesion> sesiones() { return sesiones; }
    public Parametros parametros() { return parametros; }

    public Franja franja(int id) { return franjas.get(id); }
    public Aula aula(int id) { return aulas.get(id); }
    public Sesion sesion(int id) { return sesiones.get(id); }

    public List<Franja> franjasDelDia(int dia) {
        return dia >= 0 && dia < franjasPorDia.size() ? franjasPorDia.get(dia) : List.of();
    }

    public List<Sesion> sesionesDe(Docente d) {
        return sesionesPorDocente.getOrDefault(d.id(), List.of());
    }

    public List<Sesion> sesionesDelGrupo(String grupo) {
        return sesionesPorGrupo.getOrDefault(grupo, List.of());
    }

    public List<Sesion> sesionesDe(Seccion s) {
        return sesionesPorSeccion.getOrDefault(s.id(), List.of());
    }

    public List<String> grupos() {
        return new ArrayList<>(sesionesPorGrupo.keySet());
    }

    public Instancia conParametros(Parametros p) {
        return new Instancia(franjas, aulas, docentes, asignaturas, secciones, p);
    }
}
