package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.dominio.Docente;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Seccion;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Evalúa las restricciones blandas (RB-01 a RB-06 del Entregable 1) de un horario factible:
 * no invalidan el horario, pero dan un puntaje de calidad (RF-10, MVP-15) y la lista de casos
 * concretos para que el coordinador decida si los mejora.
 */
public final class EvaluadorBlandas {

    /** Un caso concreto en que el horario no cumple una preferencia. */
    public record Hallazgo(String criterio, String texto, List<Integer> sesiones, String grupo, String docente) {
    }

    /**
     * @param puntaje                 0 a 100; combina todos los criterios
     * @param porcentajePreferidas    RB-02: clases en franjas preferidas por su docente
     * @param asignaturasDiasSeguidos RB-01: asignaturas de 2 o 3 clases con clases en días consecutivos
     * @param huecosDocentes          RB-03 y RB-05: franjas libres entre clases de un docente en un mismo día
     * @param asignaturasSeparadas    RB-04: asignaturas con clases a más de 3 días de distancia
     * @param diasCargados            RB-06: días en que un grupo tiene 4 o más clases (8 horas)
     * @param asignaturasRepetidasDia secciones con dos clases el mismo día (debe ser 0: es obligatoria)
     */
    public record Metricas(double puntaje, double porcentajePreferidas, int asignaturasDiasSeguidos, int huecosDocentes,
                           int asignaturasSeparadas, int diasCargados, int asignaturasRepetidasDia,
                           List<Hallazgo> hallazgos) {
    }

    public static final String RB01 = "RB-01";
    public static final String RB02 = "RB-02";
    public static final String RB04 = "RB-04";
    public static final String RB05 = "RB-05";
    public static final String RB06 = "RB-06";

    private EvaluadorBlandas() {
    }

    public static Metricas evaluar(Instancia inst, int[] franjaDe) {
        List<Hallazgo> hallazgos = new ArrayList<>();

        // RB-02: franjas preferidas por el docente.
        int pref = 0;
        int total = 0;
        for (Sesion s : inst.sesiones()) {
            int f = franjaDe[s.id()];
            if (f < 0) continue;
            total++;
            Franja fr = inst.franja(f);
            if (s.docente().prefiere(fr)) {
                pref++;
            } else {
                hallazgos.add(new Hallazgo(RB02, s.asignatura().nombre() + " (" + s.grupo() + ") el " + fr.etiqueta()
                        + ": no es una franja preferida de " + s.docente().nombre(), List.of(s.id()), s.grupo(), s.docente().codigo()));
            }
        }

        // RB-01 y RB-04: distribución de las clases de cada asignatura en la semana.
        int seguidos = 0;
        int separadas = 0;
        int repetidas = 0;
        int multi = 0;
        for (Seccion sec : inst.secciones()) {
            List<Sesion> ss = new ArrayList<>();
            for (Sesion s : inst.sesionesDe(sec)) if (franjaDe[s.id()] >= 0) ss.add(s);
            ss.sort(Comparator.comparingInt(s -> franjaDe[s.id()]));
            if (ss.size() < 2) continue;
            multi++;
            boolean consecutivo = false;
            boolean lejos = false;
            List<Integer> parSeguido = null;
            List<Integer> parLejos = null;
            for (int i = 0; i + 1 < ss.size(); i++) {
                int d1 = inst.franja(franjaDe[ss.get(i).id()]).dia();
                int d2 = inst.franja(franjaDe[ss.get(i + 1).id()]).dia();
                if (d1 == d2) repetidas++;
                if (d2 - d1 == 1 && !consecutivo) {
                    consecutivo = true;
                    parSeguido = List.of(ss.get(i + 1).id(), ss.get(i).id());
                }
                if (d2 - d1 > 3 && !lejos) {
                    lejos = true;
                    parLejos = List.of(ss.get(i + 1).id(), ss.get(i).id());
                }
            }
            int n = sec.asignatura().sesionesSemanales();
            if (consecutivo && (n == 2 || n == 3)) {
                seguidos++;
                hallazgos.add(new Hallazgo(RB01, sec.asignatura().nombre() + " (" + sec.grupo() + ") tiene clases el "
                        + dias(inst, franjaDe, ss) + ", en días consecutivos", parSeguido, sec.grupo(), sec.docente().codigo()));
            }
            if (lejos) {
                separadas++;
                hallazgos.add(new Hallazgo(RB04, sec.asignatura().nombre() + " (" + sec.grupo() + ") tiene clases el "
                        + dias(inst, franjaDe, ss) + ", con más de tres días entre una y otra", parLejos, sec.grupo(),
                        sec.docente().codigo()));
            }
        }

        // RB-03 y RB-05: aprovechamiento del tiempo del docente (franjas libres entre clases el mismo día).
        int huecos = 0;
        for (Docente d : inst.docentes()) {
            for (int dia = 0; dia < Franja.DIAS.length; dia++) {
                List<Franja> delDia = inst.franjasDelDia(dia);
                List<Integer> posiciones = new ArrayList<>();
                List<Integer> sesionesDia = new ArrayList<>();
                for (Sesion s : inst.sesionesDe(d)) {
                    int f = franjaDe[s.id()];
                    if (f >= 0 && inst.franja(f).dia() == dia) {
                        posiciones.add(delDia.indexOf(inst.franja(f)));
                        sesionesDia.add(s.id());
                    }
                }
                if (posiciones.size() < 2) continue;
                int min = posiciones.stream().min(Integer::compare).orElse(0);
                int max = posiciones.stream().max(Integer::compare).orElse(0);
                Set<Integer> distintas = new TreeSet<>(posiciones);
                int libres = (max - min + 1) - distintas.size();
                if (libres > 0) {
                    huecos += libres;
                    hallazgos.add(new Hallazgo(RB05, d.nombre() + " (" + d.codigo() + ") tiene " + libres
                            + (libres == 1 ? " franja libre" : " franjas libres") + " entre clases el "
                            + Franja.DIAS[dia].toLowerCase(), sesionesDia, null, d.codigo()));
                }
            }
        }

        // RB-06: distribución equilibrada de la semana de cada grupo.
        int cargados = 0;
        for (String g : inst.grupos()) {
            int[] porDia = new int[Franja.DIAS.length];
            List<List<Integer>> ids = new ArrayList<>();
            for (int i = 0; i < porDia.length; i++) ids.add(new ArrayList<>());
            for (Sesion s : inst.sesionesDelGrupo(g)) {
                int f = franjaDe[s.id()];
                if (f < 0) continue;
                int dia = inst.franja(f).dia();
                porDia[dia]++;
                ids.get(dia).add(s.id());
            }
            for (int dia = 0; dia < porDia.length; dia++) {
                if (porDia[dia] >= 4) {
                    cargados++;
                    hallazgos.add(new Hallazgo(RB06, "El grupo " + g + " tiene " + porDia[dia] + " clases el "
                            + Franja.DIAS[dia].toLowerCase() + " (" + (2 * porDia[dia]) + " horas)", ids.get(dia), g, null));
                }
            }
        }

        double pPref = total == 0 ? 100 : 100.0 * pref / total;
        double pSeguidos = 100 * (1 - (double) seguidos / Math.max(1, multi));
        double pSeparadas = 100 * (1 - (double) separadas / Math.max(1, multi));
        double pHuecos = 100 * Math.max(0, 1 - (double) huecos / Math.max(1, total));
        double pEquilibrio = 100 * (1 - (double) cargados / Math.max(1, inst.grupos().size() * Franja.DIAS.length));
        double puntaje = 0.30 * pPref + 0.25 * pSeguidos + 0.20 * pHuecos + 0.10 * pSeparadas + 0.15 * pEquilibrio;

        return new Metricas(Math.round(puntaje * 10) / 10.0, pPref, seguidos, huecos, separadas, cargados, repetidas,
                List.copyOf(hallazgos));
    }

    private static String dias(Instancia inst, int[] franjaDe, List<Sesion> ss) {
        List<String> nombres = new ArrayList<>();
        for (Sesion s : ss) nombres.add(inst.franja(franjaDe[s.id()]).nombreDia().toLowerCase());
        if (nombres.size() == 1) return nombres.get(0);
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    /** Secciones (2 o 3 clases semanales) con clases el mismo día o en días consecutivos. */
    public static Set<Integer> seccionesEnDiasSeguidos(Instancia inst, int[] franjaDe) {
        Set<Integer> r = new TreeSet<>();
        for (Seccion sec : inst.secciones()) {
            int n = sec.asignatura().sesionesSemanales();
            if (n != 2 && n != 3) continue;
            List<Integer> dias = new ArrayList<>();
            for (Sesion s : inst.sesionesDe(sec)) {
                int f = s.id() < franjaDe.length ? franjaDe[s.id()] : -1;
                if (f >= 0) dias.add(inst.franja(f).dia());
            }
            for (int i = 0; i < dias.size(); i++)
                for (int j = i + 1; j < dias.size(); j++)
                    if (Math.abs(dias.get(i) - dias.get(j)) <= 1) r.add(sec.id());
        }
        return r;
    }

    /**
     * Preferencias que rompería ubicar una clase en una franja. No impiden el cambio:
     * el editor las muestra como advertencia para que el coordinador decida.
     *
     * @param diasSeguidosEsObligatoria si la regla de días no consecutivos ya es obligatoria, no se repite como aviso
     */
    public static List<String> avisos(Instancia inst, Estado estado, Sesion s, Franja f, boolean diasSeguidosEsObligatoria) {
        List<String> r = new ArrayList<>();
        if (!s.docente().prefiere(f)) {
            r.add("El " + f.etiqueta() + " no es una franja preferida de " + s.docente().nombre() + ".");
        }
        int n = s.asignatura().sesionesSemanales();
        if (!diasSeguidosEsObligatoria && (n == 2 || n == 3)) {
            for (Sesion otra : inst.sesionesDe(s.seccion())) {
                if (otra.id() == s.id() || !estado.estaAsignada(otra.id())) continue;
                Franja fo = inst.franja(estado.franjaDe(otra.id()));
                if (Math.abs(fo.dia() - f.dia()) == 1) {
                    r.add(s.asignatura().nombre() + " quedaría con clases en días seguidos ("
                            + fo.nombreDia().toLowerCase() + " y " + f.nombreDia().toLowerCase() + ").");
                    break;
                }
            }
        }
        return r;
    }
}
