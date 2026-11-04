package co.edu.unbosque.horarios.motor.datos;

import co.edu.unbosque.horarios.motor.dominio.Asignatura;
import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Docente;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Parametros;
import co.edu.unbosque.horarios.motor.dominio.Seccion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Datos de prueba (no institucionales) del alcance del MVP (Entregable 1): 5.º, 6.º y 7.º semestre
 * de Ingeniería de Sistemas, con 22 asignaturas (7 + 7 + 8) y 51 créditos.
 *
 * <p>Supuestos (Anexo A del dossier): clases semanales según créditos (4 créditos → 3 clases;
 * 3 o 2 → 2; 1 → 1); siete asignaturas requieren computadores y dos, sillas móviles; secciones de
 * 18 a 44 estudiantes; uno de cada cuatro docentes solo dicta desde las 5:00 p. m. y los demás
 * tienen entre 4 y 8 franjas bloqueadas.</p>
 *
 * <p>Tamaño actual: 6 grupos (5A a 7B), 12 docentes, 8 aulas, 44 secciones, 84 clases y 32 franjas.</p>
 */
public final class GeneradorDatos {

    public static final String COMPUTADORES = "computadores";
    public static final String SILLAS_MOVILES = "sillas móviles";

    public record AulaSpec(String codigo, int capacidad, Set<String> caracteristicas) {
    }

    /**
     * @param disponiblesFijas si no es nulo, reemplaza la disponibilidad generada
     * @param habilitadas      códigos de las asignaturas que puede orientar (RD-08)
     */
    public record DocenteSpec(String codigo, String nombre, boolean nocturno, Set<Integer> disponiblesFijas,
                              Set<String> habilitadas) {

        DocenteSpec conHabilitadas(Set<String> h) {
            return new DocenteSpec(codigo, nombre, nocturno, disponiblesFijas, h);
        }
    }

    public record AsignaturaSpec(String codigo, String nombre, int semestre, int creditos, Set<String> requisitos) {
    }

    public record SeccionSpec(String grupo, String asignatura, String docente, int estudiantes) {
    }

    public record Plan(List<AulaSpec> aulas, List<DocenteSpec> docentes, List<AsignaturaSpec> asignaturas,
                       List<SeccionSpec> secciones) {
    }

    private GeneradorDatos() {
    }

    // ------------------------------------------------------------------ franjas

    /**
     * RD-04 y RD-05: lunes a viernes de 07:00 a 21:00 y sábado de 07:00 a 11:00, en franjas de dos horas,
     * sin clases entre 11:00 y 13:00 (receso). 32 franjas.
     */
    public static List<Franja> franjas() {
        int[] horas = {7, 9, 13, 15, 17, 19};
        List<Franja> r = new ArrayList<>();
        for (int d = 0; d < 6; d++) {
            for (int h : horas) {
                if (d == 5 && h >= 13) continue;
                r.add(new Franja(r.size(), d, h, h + 2));
            }
        }
        return r;
    }

    public static Set<Integer> franjasNocturnas() {
        Set<Integer> r = new TreeSet<>();
        for (Franja f : franjas()) if (f.nocturna()) r.add(f.id());
        return r;
    }

    // ------------------------------------------------------------------ plan base

    public static Plan planBase() {
        List<AulaSpec> aulas = List.of(
                new AulaSpec("S101", 45, Set.of()),
                new AulaSpec("S102", 45, Set.of()),
                new AulaSpec("S103", 40, Set.of()),
                new AulaSpec("S201", 40, Set.of(SILLAS_MOVILES)),
                new AulaSpec("S202", 45, Set.of()),
                new AulaSpec("LAB1", 40, Set.of(COMPUTADORES)),
                new AulaSpec("LAB2", 40, Set.of(COMPUTADORES)),
                new AulaSpec("LAB3", 40, Set.of(COMPUTADORES)));

        Set<String> pc = Set.of(COMPUTADORES);
        Set<String> sm = Set.of(SILLAS_MOVILES);
        Set<String> no = Set.of();
        List<AsignaturaSpec> asignaturas = List.of(
                // 5.º semestre: 7 asignaturas, 17 créditos, 14 clases
                new AsignaturaSpec("SIS-501", "Bases de datos 2", 5, 4, pc),
                new AsignaturaSpec("SIS-502", "Ingeniería de software 1", 5, 3, no),
                new AsignaturaSpec("SIS-503", "Dinámica de sistemas", 5, 3, no),
                new AsignaturaSpec("SIS-504", "Microprocesadores y programación assembler", 5, 2, pc),
                new AsignaturaSpec("SIS-505", "Sistemas operacionales", 5, 2, pc),
                new AsignaturaSpec("SIS-506", "Probabilidad y estadística", 5, 2, no),
                new AsignaturaSpec("SIS-507", "Electiva 3", 5, 1, no),
                // 6.º semestre: 7 asignaturas, 17 créditos, 14 clases
                new AsignaturaSpec("SIS-601", "Ingeniería de software 2", 6, 4, sm),
                new AsignaturaSpec("SIS-602", "Redes de computadores", 6, 3, pc),
                new AsignaturaSpec("SIS-603", "Inteligencia artificial", 6, 3, no),
                new AsignaturaSpec("SIS-604", "Investigación de operaciones", 6, 2, no),
                new AsignaturaSpec("SIS-605", "Compiladores", 6, 2, pc),
                new AsignaturaSpec("SIS-606", "Ética profesional", 6, 1, no),
                new AsignaturaSpec("SIS-607", "Electiva 4", 6, 2, no),
                // 7.º semestre: 8 asignaturas, 17 créditos, 14 clases
                new AsignaturaSpec("SIS-701", "Arquitectura de software", 7, 4, no),
                new AsignaturaSpec("SIS-702", "Computación en la nube", 7, 2, pc),
                new AsignaturaSpec("SIS-703", "Gerencia de proyectos", 7, 3, sm),
                new AsignaturaSpec("SIS-704", "Seguridad informática", 7, 3, no),
                new AsignaturaSpec("SIS-705", "Sistemas distribuidos", 7, 2, pc),
                new AsignaturaSpec("SIS-706", "Electiva 5", 7, 1, no),
                new AsignaturaSpec("SIS-707", "Emprendimiento", 7, 1, no),
                new AsignaturaSpec("SIS-708", "Seminario de investigación", 7, 1, no));

        // asignatura → { docente grupo A, estudiantes A, docente grupo B, estudiantes B }
        Object[][] oferta = {
                {"SIS-501", "D01", 30, "D01", 26},
                {"SIS-502", "D02", 32, "D02", 29},
                {"SIS-503", "D05", 42, "D05", 40},
                {"SIS-504", "D03", 28, "D03", 24},
                {"SIS-505", "D04", 30, "D04", 25},
                {"SIS-506", "D09", 44, "D09", 41},
                {"SIS-507", "D07", 22, "D10", 18},
                {"SIS-601", "D01", 31, "D02", 27},
                {"SIS-602", "D12", 27, "D12", 25},
                {"SIS-603", "D08", 39, "D06", 33},
                {"SIS-604", "D09", 43, "D11", 40},
                {"SIS-605", "D06", 30, "D06", 26},
                {"SIS-606", "D07", 35, "D07", 30},
                {"SIS-607", "D10", 24, "D10", 20},
                {"SIS-701", "D02", 42, "D03", 36},
                {"SIS-702", "D08", 24, "D12", 22},
                {"SIS-703", "D11", 28, "D11", 25},
                {"SIS-704", "D06", 39, "D05", 34},
                {"SIS-705", "D03", 26, "D08", 23},
                {"SIS-706", "D07", 21, "D10", 18},
                {"SIS-707", "D11", 23, "D05", 20},
                {"SIS-708", "D09", 19, "D07", 18}};
        List<SeccionSpec> secciones = new ArrayList<>();
        for (int sem = 5; sem <= 7; sem++) {
            for (String letra : new String[]{"A", "B"}) {
                for (Object[] o : oferta) {
                    String asig = (String) o[0];
                    if (asig.charAt(4) - '0' != sem) continue;
                    boolean a = letra.equals("A");
                    secciones.add(new SeccionSpec(sem + letra, asig, (String) o[a ? 1 : 3], (int) o[a ? 2 : 4]));
                }
            }
        }

        List<DocenteSpec> docentes = List.of(
                new DocenteSpec("D01", "Andrea Rincón", false, null, Set.of("SIS-502")),
                new DocenteSpec("D02", "Carlos Méndez", false, null, Set.of("SIS-501")),
                new DocenteSpec("D03", "Juliana Torres", false, null, Set.of()),
                new DocenteSpec("D04", "Hernán Castillo", true, null, Set.of("SIS-705")),
                new DocenteSpec("D05", "Paola Guzmán", false, null, Set.of()),
                new DocenteSpec("D06", "Diego Salazar", false, null, Set.of("SIS-604")),
                new DocenteSpec("D07", "Marcela Ortiz", false, null, Set.of()),
                new DocenteSpec("D08", "Ricardo Pineda", true, null, Set.of()),
                new DocenteSpec("D09", "Natalia Vargas", false, null, Set.of("SIS-603")),
                new DocenteSpec("D10", "Sergio Cárdenas", false, null, Set.of()),
                new DocenteSpec("D11", "Lucía Herrera", false, null, Set.of()),
                new DocenteSpec("D12", "Fernando Rojas", true, null, Set.of()));
        return conHabilitacionesDeLaOferta(new Plan(aulas, docentes, asignaturas, secciones));
    }

    /** Cada docente queda habilitado para lo que dicta, además de las habilitaciones extra que tenga. */
    private static Plan conHabilitacionesDeLaOferta(Plan p) {
        Map<String, Set<String>> hab = new HashMap<>();
        for (SeccionSpec s : p.secciones()) hab.computeIfAbsent(s.docente(), k -> new TreeSet<>()).add(s.asignatura());
        List<DocenteSpec> docentes = new ArrayList<>();
        for (DocenteSpec d : p.docentes()) {
            Set<String> h = new TreeSet<>(d.habilitadas());
            h.addAll(hab.getOrDefault(d.codigo(), Set.of()));
            docentes.add(d.conHabilitadas(h));
        }
        return new Plan(p.aulas(), docentes, p.asignaturas(), p.secciones());
    }

    // ------------------------------------------------------------------ construcción

    public static Instancia base(long semilla) {
        return construir(planBase(), semilla, Parametros.porDefecto());
    }

    /** Convierte un plan en una instancia: genera la disponibilidad y las preferencias de cada docente. */
    public static Instancia construir(Plan plan, long semilla, Parametros parametros) {
        List<Franja> franjas = franjas();
        Set<Integer> nocturnas = franjasNocturnas();

        List<Aula> aulas = new ArrayList<>();
        for (AulaSpec a : plan.aulas()) aulas.add(new Aula(aulas.size(), a.codigo(), a.capacidad(), a.caracteristicas()));

        List<Asignatura> asignaturas = new ArrayList<>();
        Map<String, Asignatura> asigPorCodigo = new HashMap<>();
        for (AsignaturaSpec a : plan.asignaturas()) {
            Asignatura as = new Asignatura(asignaturas.size(), a.codigo(), a.nombre(), a.semestre(), a.creditos(),
                    Asignatura.sesionesPorCreditos(a.creditos()), a.requisitos());
            asignaturas.add(as);
            asigPorCodigo.put(a.codigo(), as);
        }

        List<Docente> docentes = new ArrayList<>();
        Map<String, Docente> docPorCodigo = new HashMap<>();
        for (DocenteSpec d : plan.docentes()) {
            Random rnd = new Random(semilla * 7919L + d.codigo().hashCode());
            Set<Integer> disp;
            Set<Integer> pref;
            if (d.disponiblesFijas() != null) {
                disp = new TreeSet<>(d.disponiblesFijas());
                pref = new TreeSet<>(disp);
            } else if (d.nocturno()) {
                disp = new TreeSet<>(nocturnas);
                pref = new TreeSet<>(disp);
            } else {
                List<Integer> ids = new ArrayList<>();
                for (Franja f : franjas) ids.add(f.id());
                Collections.shuffle(ids, rnd);
                int bloqueadas = 4 + rnd.nextInt(5);
                disp = new TreeSet<>(ids.subList(bloqueadas, ids.size()));
                pref = new TreeSet<>();
                for (int f : disp) if (rnd.nextDouble() < 0.80) pref.add(f);
            }
            Set<Integer> hab = new TreeSet<>();
            for (String c : d.habilitadas()) {
                Asignatura a = asigPorCodigo.get(c);
                if (a != null) hab.add(a.id());
            }
            Docente doc = new Docente(docentes.size(), d.codigo(), d.nombre(), Set.copyOf(disp), Set.copyOf(pref), Set.copyOf(hab));
            docentes.add(doc);
            docPorCodigo.put(d.codigo(), doc);
        }

        List<Seccion> secciones = new ArrayList<>();
        for (SeccionSpec s : plan.secciones()) {
            Asignatura as = asigPorCodigo.get(s.asignatura());
            Docente d = docPorCodigo.get(s.docente());
            if (as == null || d == null) throw new IllegalArgumentException("Sección con datos incompletos: " + s);
            secciones.add(new Seccion(secciones.size(), as.codigo() + "-" + s.grupo(), s.grupo(), s.estudiantes(), as, d));
        }
        return new Instancia(franjas, aulas, docentes, asignaturas, secciones, parametros);
    }

    // ------------------------------------------------------------------ escenarios

    /**
     * Multiplica cursos (grupos con sus secciones y docentes) y aulas.
     *
     * @param factorCursos puede ser fraccionario (copia grupos completos hasta llegar a ese múltiplo de clases)
     */
    public static Plan escalar(Plan base, double factorCursos, int factorAulas) {
        List<AulaSpec> aulas = new ArrayList<>(base.aulas());
        for (int k = 1; k < factorAulas; k++) {
            for (AulaSpec a : base.aulas()) aulas.add(new AulaSpec(a.codigo() + "-" + (k + 1), a.capacidad(), a.caracteristicas()));
        }
        Map<String, Integer> sesionesPorAsig = new HashMap<>();
        for (AsignaturaSpec a : base.asignaturas()) sesionesPorAsig.put(a.codigo(), Asignatura.sesionesPorCreditos(a.creditos()));
        int sesionesBase = base.secciones().stream().mapToInt(s -> sesionesPorAsig.get(s.asignatura())).sum();
        int objetivo = (int) Math.round(sesionesBase * factorCursos);

        List<DocenteSpec> docentes = new ArrayList<>(base.docentes());
        List<SeccionSpec> secciones = new ArrayList<>(base.secciones());
        List<String> grupos = new ArrayList<>(new LinkedHashSet<>(base.secciones().stream().map(SeccionSpec::grupo).toList()));
        int total = sesionesBase;
        int nd = base.docentes().size();
        for (int k = 1; total < objetivo; k++) {
            Map<String, String> docCopia = new HashMap<>();
            for (int i = 0; i < nd; i++) {
                DocenteSpec d = base.docentes().get(i);
                String codigo = String.format("D%02d", k * nd + i + 1);
                docCopia.put(d.codigo(), codigo);
                docentes.add(new DocenteSpec(codigo, "Docente " + codigo, d.nocturno(), d.disponiblesFijas(), d.habilitadas()));
            }
            for (String g : grupos) {
                if (total >= objetivo) break;
                String nuevo = grupoCopia(g, k);
                for (SeccionSpec s : base.secciones()) {
                    if (!s.grupo().equals(g)) continue;
                    secciones.add(new SeccionSpec(nuevo, s.asignatura(), docCopia.get(s.docente()), s.estudiantes()));
                    total += sesionesPorAsig.get(s.asignatura());
                }
            }
        }
        Set<String> usados = new HashSet<>();
        for (SeccionSpec s : secciones) usados.add(s.docente());
        docentes.removeIf(d -> !usados.contains(d.codigo()));
        return new Plan(aulas, docentes, base.asignaturas(), secciones);
    }

    /** 5A → 5C (copia 1), 5E (copia 2)... */
    private static String grupoCopia(String grupo, int k) {
        char letra = grupo.charAt(grupo.length() - 1);
        String prefijo = grupo.substring(0, grupo.length() - 1);
        int idx = letra - 'A' + 2 * k;
        String l = idx < 26 ? String.valueOf((char) ('A' + idx)) : "Z" + idx;
        return prefijo + l;
    }

    /** Reasigna secciones (asignatura + grupo) a otro docente, que queda habilitado para ellas si se pide. */
    private static Plan reasignar(Plan base, Map<String, String> seccionADocente, boolean habilitar) {
        List<SeccionSpec> secciones = new ArrayList<>();
        Map<String, Set<String>> nuevas = new HashMap<>();
        for (SeccionSpec s : base.secciones()) {
            String nuevo = seccionADocente.get(s.asignatura() + "-" + s.grupo());
            if (nuevo != null) {
                secciones.add(new SeccionSpec(s.grupo(), s.asignatura(), nuevo, s.estudiantes()));
                nuevas.computeIfAbsent(nuevo, k -> new TreeSet<>()).add(s.asignatura());
            } else {
                secciones.add(s);
            }
        }
        List<DocenteSpec> docentes = new ArrayList<>();
        for (DocenteSpec d : base.docentes()) {
            Set<String> h = new TreeSet<>(d.habilitadas());
            if (habilitar) h.addAll(nuevas.getOrDefault(d.codigo(), Set.of()));
            docentes.add(d.conHabilitadas(h));
        }
        return new Plan(base.aulas(), docentes, base.asignaturas(), secciones);
    }

    /** Un docente (D06) con 11 clases y solo 4 franjas disponibles. */
    public static Plan docenteSinFranjas(Plan base) {
        Plan p = reasignar(base, Map.of("SIS-704-7B", "D06", "SIS-707-7B", "D06"), true);
        List<DocenteSpec> docentes = new ArrayList<>();
        for (DocenteSpec d : p.docentes()) {
            docentes.add(d.codigo().equals("D06")
                    ? new DocenteSpec(d.codigo(), d.nombre(), false, Set.of(0, 6, 12, 18), d.habilitadas())
                    : d);
        }
        return new Plan(p.aulas(), docentes, p.asignaturas(), p.secciones());
    }

    /** RD-08: una sección queda con un docente que no está habilitado para orientarla. */
    public static Plan docenteNoHabilitado(Plan base) {
        return reasignar(base, Map.of("SIS-605-6B", "D07"), false);
    }

    /** El grupo 7A (14 clases) queda con dos docentes que solo dictan de noche (10 franjas nocturnas). */
    public static Plan grupoNocturno(Plan base) {
        List<DocenteSpec> docentes = new ArrayList<>(base.docentes());
        docentes.add(new DocenteSpec("DX1", "Óscar Medina", true, null, Set.of()));
        docentes.add(new DocenteSpec("DX2", "Camila Peña", true, null, Set.of()));
        Plan conNuevos = new Plan(base.aulas(), docentes, base.asignaturas(), base.secciones());
        Map<String, String> m = new LinkedHashMap<>();
        for (String a : List.of("SIS-701", "SIS-703", "SIS-705")) m.put(a + "-7A", "DX1");
        for (String a : List.of("SIS-702", "SIS-704", "SIS-706", "SIS-707", "SIS-708")) m.put(a + "-7A", "DX2");
        return reasignar(conNuevos, m, true);
    }

    /** El doble de cursos con una sola sala de cómputo. */
    public static Plan unaSalaComputo(Plan base) {
        Plan doble = escalar(base, 2.0, 1);
        List<AulaSpec> aulas = new ArrayList<>();
        boolean primera = true;
        for (AulaSpec a : doble.aulas()) {
            if (a.caracteristicas().contains(COMPUTADORES)) {
                aulas.add(primera ? a : new AulaSpec(a.codigo(), a.capacidad(), Set.of()));
                primera = false;
            } else {
                aulas.add(a);
            }
        }
        return new Plan(aulas, doble.docentes(), doble.asignaturas(), doble.secciones());
    }
}
