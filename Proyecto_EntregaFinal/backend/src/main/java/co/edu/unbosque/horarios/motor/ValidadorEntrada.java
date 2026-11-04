package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Docente;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Seccion;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.restricciones.RestriccionUnaria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Revisión previa de los datos: detecta en milisegundos los casos en que no puede existir
 * un horario y nombra la causa y los elementos involucrados, sin necesidad de buscar.
 */
public final class ValidadorEntrada {

    /**
     * @param bloqueante si es verdadero no puede existir un horario; si no, es una advertencia.
     */
    public record Problema(String tipo, String mensaje, List<String> elementos, boolean bloqueante) {
    }

    private final boolean revisionPorGrupo;

    /**
     * @param revisionPorGrupo revisión previa mejorada: las clases de un grupo no pueden superar
     *                         las franjas en que sus docentes están disponibles.
     */
    public ValidadorEntrada(boolean revisionPorGrupo) {
        this.revisionPorGrupo = revisionPorGrupo;
    }

    public static ValidadorEntrada mejorada() {
        return new ValidadorEntrada(true);
    }

    public List<Problema> revisar(Instancia inst, RegistroRestricciones reglas) {
        List<Problema> r = new ArrayList<>();
        revisarHabilitacion(inst, r);
        revisarFrecuencia(inst, r);
        revisarSecciones(inst, reglas, r);
        revisarDocentes(inst, r);
        revisarDias(inst, r);
        if (revisionPorGrupo) revisarGrupos(inst, r);
        revisarRecursos(inst, r);
        revisarTamanos(inst, r);
        return r;
    }

    public static boolean hayBloqueantes(List<Problema> problemas) {
        return problemas.stream().anyMatch(Problema::bloqueante);
    }

    /** Cada sección necesita al menos un aula adecuada en alguna franja disponible de su docente. */
    private void revisarSecciones(Instancia inst, RegistroRestricciones reglas, List<Problema> r) {
        for (Seccion sec : inst.secciones()) {
            Sesion s = inst.sesionesDe(sec).isEmpty() ? null : inst.sesionesDe(sec).get(0);
            if (s == null) continue;
            boolean alguna = false;
            Map<String, String> motivo = new LinkedHashMap<>();
            for (Franja f : inst.franjas()) {
                for (Aula a : inst.aulas()) {
                    boolean ok = true;
                    for (RestriccionUnaria u : reglas.unarias()) {
                        if (!u.permite(s, f, a)) {
                            ok = false;
                            motivo.putIfAbsent(u.id(), u.descripcion());
                            break;
                        }
                    }
                    if (ok) {
                        alguna = true;
                        break;
                    }
                }
                if (alguna) break;
            }
            if (!alguna) {
                String req = sec.asignatura().requisitosAula().isEmpty() ? "" : " con " + String.join(" y ", sec.asignatura().requisitosAula());
                r.add(new Problema("SECCION_SIN_OPCIONES",
                        "La sección " + sec.codigo() + " (" + sec.asignatura().nombre() + ", grupo " + sec.grupo()
                                + ") necesita un aula" + req + " para " + sec.estudiantes()
                                + " estudiantes en una franja de " + sec.docente().codigo() + " y no existe ninguna",
                        List.of(sec.codigo(), sec.docente().codigo()), true));
            }
        }
    }

    /** Un docente no puede dictar más clases que franjas tiene disponibles. */
    private void revisarDocentes(Instancia inst, List<Problema> r) {
        for (Docente d : inst.docentes()) {
            int sesiones = inst.sesionesDe(d).size();
            int franjas = d.disponibles().size();
            if (sesiones > franjas) {
                r.add(new Problema("DOCENTE_SIN_FRANJAS",
                        "El docente " + d.codigo() + " debe dictar " + sesiones + " sesiones pero solo tiene "
                                + franjas + " franjas disponibles",
                        List.of(d.codigo()), true));
            }
        }
    }

    /** Como una asignatura no puede tener dos clases el mismo día, el docente debe estar disponible en suficientes días. */
    private void revisarDias(Instancia inst, List<Problema> r) {
        for (Seccion sec : inst.secciones()) {
            Set<Integer> dias = new HashSet<>();
            for (int f : sec.docente().disponibles()) dias.add(inst.franja(f).dia());
            int clases = sec.asignatura().sesionesSemanales();
            if (clases > dias.size()) {
                r.add(new Problema("DIAS_INSUFICIENTES",
                        "La sección " + sec.codigo() + " tiene " + clases + " clases semanales, pero el docente "
                                + sec.docente().codigo() + " solo está disponible " + dias.size()
                                + (dias.size() == 1 ? " día" : " días") + " y no puede haber dos clases el mismo día",
                        List.of(sec.codigo(), sec.docente().codigo()), true));
            }
        }
    }

    /** Revisión previa mejorada: las clases de un grupo no pueden superar las franjas de sus docentes. */
    private void revisarGrupos(Instancia inst, List<Problema> r) {
        for (String g : inst.grupos()) {
            List<Sesion> ss = inst.sesionesDelGrupo(g);
            Set<String> docentes = new TreeSet<>();
            Set<Integer> franjas = new HashSet<>();
            for (Sesion s : ss) {
                docentes.add(s.docente().codigo());
                franjas.addAll(s.docente().disponibles());
            }
            if (ss.size() > franjas.size()) {
                List<String> elementos = new ArrayList<>();
                elementos.add("grupo " + g);
                elementos.addAll(docentes);
                r.add(new Problema("GRUPO_SIN_FRANJAS",
                        "El grupo " + g + " tiene " + ss.size() + " sesiones con los docentes " + String.join(", ", docentes)
                                + ", pero entre todos solo están disponibles en " + franjas.size() + " franjas",
                        elementos, true));
            }
        }
    }

    /** Recursos escasos: cada característica de aula y el total de cupos aula-franja. */
    private void revisarRecursos(Instancia inst, List<Problema> r) {
        int nf = inst.franjas().size();
        Set<String> caracteristicas = new LinkedHashSet<>();
        for (Seccion s : inst.secciones()) caracteristicas.addAll(s.asignatura().requisitosAula());
        for (String c : caracteristicas) {
            int sesiones = 0;
            for (Sesion s : inst.sesiones()) if (s.asignatura().requisitosAula().contains(c)) sesiones++;
            long aulas = inst.aulas().stream().filter(a -> a.caracteristicas().contains(c)).count();
            long cupos = aulas * nf;
            if (sesiones > cupos) {
                r.add(new Problema("RECURSO_INSUFICIENTE",
                        "Hay " + sesiones + " sesiones que requieren " + c + " y solo " + cupos + " cupos aula-franja",
                        List.of(c), true));
            }
        }
        int total = inst.sesiones().size();
        int cupos = inst.aulas().size() * nf;
        if (total > cupos) {
            r.add(new Problema("CUPOS_INSUFICIENTES",
                    "Hay " + total + " sesiones y solo " + cupos + " cupos aula-franja", List.of("aulas"), true));
        }
    }

    /** RD-08: cada sección debe tener un docente habilitado para orientar su asignatura. */
    private void revisarHabilitacion(Instancia inst, List<Problema> r) {
        for (Seccion s : inst.secciones()) {
            if (!s.docente().puedeDictar(s.asignatura())) {
                r.add(new Problema("DOCENTE_NO_HABILITADO",
                        "El docente " + s.docente().codigo() + " (" + s.docente().nombre() + ") no está habilitado para "
                                + s.asignatura().nombre() + " (sección " + s.codigo() + ")",
                        List.of(s.codigo(), s.docente().codigo()), true));
            }
        }
    }

    /** RD-10: el número de clases semanales debe estar entre el mínimo y el máximo configurados. */
    private void revisarFrecuencia(Instancia inst, List<Problema> r) {
        int min = inst.parametros().sesionesMin();
        int max = inst.parametros().sesionesMax();
        for (var a : inst.asignaturas()) {
            int n = a.sesionesSemanales();
            if (n < min || n > max) {
                r.add(new Problema("FRECUENCIA_INVALIDA",
                        a.nombre() + " tiene " + n + " clases semanales y el rango permitido es de " + min + " a " + max,
                        List.of(a.codigo()), true));
            }
        }
    }

    /**
     * RD-11 (obligatoria): ninguna sección puede superar el tope institucional (tamaño objetivo + tolerancia).
     * RF-14 (advertencia): se marcan las secciones con menos estudiantes que el mínimo para revisión administrativa.
     */
    private void revisarTamanos(Instancia inst, List<Problema> r) {
        int max = inst.parametros().tamanoMaximo();
        int min = inst.parametros().minimoEstudiantes();
        for (Seccion s : inst.secciones()) {
            if (s.estudiantes() > max) {
                r.add(new Problema("SECCION_SUPERA_TOPE",
                        "La sección " + s.codigo() + " (" + s.asignatura().nombre() + ", grupo " + s.grupo() + ") tiene "
                                + s.estudiantes() + " estudiantes y supera el tope de " + max + ". Debe dividirse",
                        List.of(s.codigo()), true));
            } else if (s.estudiantes() < min) {
                r.add(new Problema("SECCION_BAJO_MINIMO",
                        "La sección " + s.codigo() + " (" + s.asignatura().nombre() + ", grupo " + s.grupo() + ") tiene "
                                + s.estudiantes() + " estudiantes, menos del mínimo de " + min + ". Revisar con la administración",
                        List.of(s.codigo()), false));
            }
        }
    }
}
