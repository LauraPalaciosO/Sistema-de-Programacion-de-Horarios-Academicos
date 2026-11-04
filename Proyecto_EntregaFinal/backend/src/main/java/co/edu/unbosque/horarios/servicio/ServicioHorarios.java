package co.edu.unbosque.horarios.servicio;

import co.edu.unbosque.horarios.motor.Editor;
import co.edu.unbosque.horarios.motor.EvaluadorBlandas;
import co.edu.unbosque.horarios.motor.ServicioProgramacion;
import co.edu.unbosque.horarios.motor.ValidadorEntrada;
import co.edu.unbosque.horarios.motor.datos.GeneradorDatos;
import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Parametros;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.solver.OpcionesBusqueda;
import co.edu.unbosque.horarios.motor.solver.ResultadoSolver;
import co.edu.unbosque.horarios.servicio.Api.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 * Casos de uso del coordinador: ver el horario del periodo, generarlo, revisar conflictos
 * y hacer cambios manuales validados. No depende de Spring ni de la base de datos:
 * recibe los datos por {@link FuenteInstancia} y guarda los horarios en {@link AlmacenHorarios}.
 */
public class ServicioHorarios {

    public static final String PERIODO = "2026-2";
    static final String HORARIO = "PERIODO_" + PERIODO;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Conjuntos de datos de prueba que el coordinador puede cargar (Anexo A). */
    public static final List<ConjuntoDTO> CONJUNTOS = List.of(
            new ConjuntoDTO("base", "Periodo 2026-2",
                    "Los datos normales: 5.º a 7.º semestre, 22 asignaturas, 6 grupos, 12 docentes, 8 aulas y 84 clases. "
                            + "Trae un horario guardado con un cruce de docente para resolver.",
                    "Se genera un horario completo en milisegundos.", true),
            new ConjuntoDTO("docente-no-habilitado", "Docente no habilitado",
                    "Compiladores del grupo 6B queda a cargo de D07, que no está habilitada para esa asignatura (RD-08).",
                    "La revisión previa rechaza los datos y nombra la sección y el docente.", false),
            new ConjuntoDTO("docente-sin-franjas", "Docente sin franjas suficientes",
                    "El docente D06 debe dictar 11 clases, pero solo está disponible en 4 franjas.",
                    "La revisión previa lo detecta antes de buscar.", false),
            new ConjuntoDTO("grupo-nocturno", "Grupo 7A con docentes nocturnos",
                    "Las 14 clases del grupo 7A quedan con dos docentes que solo dictan de noche, y de noche hay 10 franjas.",
                    "La revisión previa avisa que el grupo necesita 14 franjas y sus docentes solo tienen 10.", false),
            new ConjuntoDTO("una-sala", "Una sola sala de cómputo",
                    "El doble de cursos (12 grupos) con una única sala con computadores.",
                    "La revisión previa avisa que 60 clases requieren computadores y solo hay 32 cupos.", false));

    private final FuenteInstancia fuente;
    private final AlmacenHorarios almacen;
    private final ServicioProgramacion programacion;

    private final Deque<Cambio> historial = new ArrayDeque<>();
    private final java.util.Random azar = new java.util.Random();
    /** Probabilidad de que una generación deje avisos de preferencias para corregir. */
    private static final double PROBABILIDAD_AVISOS = 0.5;
    /** Proporción de clases que, en ese caso, se ubican sin mirar las preferencias. */
    private static final double PROPORCION_AVISOS = 0.15;
    private String conjunto = "base";
    private String ultimoCambio;

    private record Cambio(int[] franjas, int[] aulas, String descripcion) {
    }

    public ServicioHorarios(FuenteInstancia fuente, AlmacenHorarios almacen, ServicioProgramacion programacion) {
        this.fuente = fuente;
        this.almacen = almacen;
        this.programacion = programacion;
    }

    // ================================================================ datos

    /**
     * Carga un conjunto de datos de prueba. Con el conjunto de ejemplo deja guardado un horario
     * con un cruce real de docente, como el del boceto (Anexo C).
     */
    public synchronized CatalogoDTO cargarConjunto(String id) {
        GeneradorDatos.Plan base = GeneradorDatos.planBase();
        GeneradorDatos.Plan plan = switch (id) {
            case "base" -> base;
            case "docente-no-habilitado" -> GeneradorDatos.docenteNoHabilitado(base);
            case "docente-sin-franjas" -> GeneradorDatos.docenteSinFranjas(base);
            case "grupo-nocturno" -> GeneradorDatos.grupoNocturno(base);
            case "una-sala" -> GeneradorDatos.unaSalaComputo(base);
            default -> throw new IllegalArgumentException("Conjunto de datos desconocido: " + id);
        };
        almacen.borrarTodos();
        fuente.reemplazar(GeneradorDatos.construir(plan, 1, Parametros.porDefecto()));
        historial.clear();
        ultimoCambio = null;
        conjunto = id;
        if (id.equals("base")) {
            Instancia inst = fuente.cargar();
            ResultadoSolver r = programar(inst).solver();
            if (r.encontrado()) {
                Estado e = Estado.desde(inst, r.franjaDe(), r.aulaDe());
                provocarCruceDeDocente(inst, e);
                guardar(inst, e);
            }
        }
        return catalogo();
    }

    public synchronized CatalogoDTO catalogo() {
        Instancia inst = fuente.cargar();
        List<FranjaDTO> franjas = inst.franjas().stream().map(ServicioHorarios::franja).toList();
        List<AulaDTO> aulas = inst.aulas().stream()
                .map(a -> new AulaDTO(a.id(), a.codigo(), a.capacidad(), a.caracteristicas().stream().sorted().toList())).toList();
        List<DocenteDTO> docentes = inst.docentes().stream()
                .map(d -> new DocenteDTO(d.id(), d.codigo(), d.nombre(),
                        !d.disponibles().isEmpty() && d.disponibles().stream().allMatch(f -> inst.franja(f).nocturna()),
                        d.disponibles().stream().sorted().toList(), d.preferidas().stream().sorted().toList(),
                        d.habilitadas().stream().sorted().toList(), inst.sesionesDe(d).size())).toList();
        List<AsignaturaDTO> asignaturas = inst.asignaturas().stream()
                .map(a -> new AsignaturaDTO(a.id(), a.codigo(), a.nombre(), a.semestre(), a.creditos(), a.sesionesSemanales(),
                        a.requisitosAula().stream().sorted().toList())).toList();
        List<SeccionDTO> secciones = inst.secciones().stream()
                .map(s -> new SeccionDTO(s.id(), s.codigo(), s.grupo(), s.estudiantes(), s.asignatura().id(), s.docente().id())).toList();
        List<SesionDTO> sesiones = inst.sesiones().stream()
                .map(s -> new SesionDTO(s.id(), s.seccion().id(), s.indice(), s.asignatura().sesionesSemanales(),
                        s.asignatura().nombre(), s.grupo(), s.asignatura().semestre(), s.docente().codigo(), s.nombre())).toList();
        Parametros p = inst.parametros();
        ParametrosDTO par = new ParametrosDTO(p.tamanoObjetivo(), p.tolerancia(), p.minimoEstudiantes(),
                p.sesionesMin(), p.sesionesMax(), p.diasNoConsecutivos(), p.tamanoMaximo());
        List<ValidadorEntrada.Problema> advertencias = ValidadorEntrada.mejorada().revisar(inst, reglas(inst));
        int cupos = inst.aulas().size() * inst.franjas().size();
        ResumenDTO resumen = new ResumenDTO(inst.docentes().size(), inst.aulas().size(), inst.secciones().size(),
                inst.sesiones().size(), inst.franjas().size(), cupos == 0 ? 0 : 100.0 * inst.sesiones().size() / cupos);
        return new CatalogoDTO(PERIODO, conjunto, Arrays.asList(Franja.DIAS), franjas, aulas, docentes, asignaturas,
                secciones, sesiones, inst.grupos(), par, advertencias, resumen, CONJUNTOS);
    }


    public synchronized CatalogoDTO actualizarParametros(ParametrosDTO p) {
        if (p.tamanoObjetivo() <= 0 || p.tolerancia() < 0 || p.tolerancia() > 1 || p.minimoEstudiantes() < 0
                || p.sesionesMin() < 1 || p.sesionesMax() < p.sesionesMin()) {
            throw new IllegalArgumentException("Parámetros inválidos");
        }
        fuente.guardarParametros(new Parametros(p.tamanoObjetivo(), p.tolerancia(), p.minimoEstudiantes(),
                p.sesionesMin(), p.sesionesMax(), p.diasNoConsecutivos()));
        return catalogo();
    }

    // ================================================================ horario

    public synchronized HorarioDTO horario() {
        Instancia inst = fuente.cargar();
        return almacen.obtener(HORARIO, inst)
                .map(h -> horarioDTO(inst, Estado.desde(inst, h.franjaDe(), h.aulaDe()), h.generadoEn().format(FORMATO)))
                .orElseGet(() -> new HorarioDTO(true, null, List.of(), List.of(), null, false, null));
    }

    /** «Generar horario»: revisión previa → método de generación (Strategy) → verificador. */
    public synchronized GeneracionDTO generar() {
        Instancia inst = fuente.cargar();
        int[] actual = almacen.obtener(HORARIO, inst).map(AlmacenHorarios.HorarioGuardado::franjaDe).orElse(null);
        ServicioProgramacion.Resultado r = null;
        // Cada generación usa una semilla distinta, así que propone un horario diferente.
        // De vez en cuando (la mitad de las veces) algunas clases se ubican sin mirar las preferencias:
        // el horario sigue cumpliendo todas las reglas obligatorias, pero deja avisos de preferencias
        // (franja no preferida, días seguidos) que el coordinador puede corregir moviendo la clase.
        // Si por azar sale idéntico al guardado, se intenta con otra semilla.
        double variacion = azar.nextDouble() < PROBABILIDAD_AVISOS ? PROPORCION_AVISOS : 0;
        for (int intento = 0; intento < 5; intento++) {
            r = programar(inst, azar.nextLong(), variacion);
            if (!r.solver().encontrado() || actual == null || !Arrays.equals(actual, r.solver().franjaDe())) break;
        }
        ResultadoSolver s = r.solver();
        HorarioDTO h = null;
        if (s.encontrado()) {
            Estado e = Estado.desde(inst, s.franjaDe(), s.aulaDe());
            historial.clear();
            ultimoCambio = "Horario generado en " + String.format("%.3f", s.segundos()).replace('.', ',') + " s";
            guardar(inst, e);
            h = horario();
        }
        return new GeneracionDTO(s.estado().name(), s.metodo(), s.segundos(), r.msRevision(), s.retrocesos(),
                s.podas(), s.causas(), h);
    }


    public synchronized Editor.Alternativas alternativas(int sesion) {
        Instancia inst = fuente.cargar();
        comprobarSesion(inst, sesion);
        Estado e = estadoActual(inst);
        if (!e.estaAsignada(sesion)) throw new IllegalArgumentException("La clase no está ubicada en el horario");
        return editor(inst).alternativas(e, sesion);
    }

    public synchronized ValidacionDTO validar(MovimientoDTO m) {
        Instancia inst = fuente.cargar();
        comprobarMovimiento(inst, m);
        Editor.Validacion v = editor(inst).validarMovimiento(estadoActual(inst), m.sesion(), m.franja(), m.aula());
        return new ValidacionDTO(v.valido(), v.motivos(), v.avisos(), v.choques(), v.ms(), inst.franja(m.franja()).etiqueta());
    }

    /** Guarda un cambio manual solo si cumple todas las reglas. */
    public synchronized AplicacionDTO aplicar(MovimientoDTO m) {
        Instancia inst = fuente.cargar();
        comprobarMovimiento(inst, m);
        Estado e = estadoActual(inst);
        int[] franjasAntes = e.copiaFranjas();
        int[] aulasAntes = e.copiaAulas();
        Editor.Validacion v = editor(inst).aplicar(e, m.sesion(), m.franja(), m.aula());
        if (v.valido()) {
            Sesion s = inst.sesion(m.sesion());
            String desc = s.asignatura().nombre() + " (" + s.grupo() + ") pasó al " + inst.franja(m.franja()).etiqueta()
                    + ", aula " + inst.aula(m.aula()).codigo();
            historial.push(new Cambio(franjasAntes, aulasAntes, ultimoCambio));
            ultimoCambio = desc;
            guardar(inst, e);
        }
        ValidacionDTO vd = new ValidacionDTO(v.valido(), v.motivos(), v.avisos(), v.choques(), v.ms(), inst.franja(m.franja()).etiqueta());
        return new AplicacionDTO(vd, horario());
    }

    public synchronized HorarioDTO deshacer() {
        Instancia inst = fuente.cargar();
        if (!historial.isEmpty()) {
            Cambio c = historial.pop();
            ultimoCambio = c.descripcion();
            guardar(inst, Estado.desde(inst, c.franjas(), c.aulas()));
        }
        return horario();
    }

    /** RF-16: exporta el horario guardado en CSV (separado por punto y coma, para abrirlo en Excel). */
    public synchronized String exportarCsv() {
        Instancia inst = fuente.cargar();
        Estado e = estadoActual(inst);
        StringBuilder sb = new StringBuilder("\uFEFF");
        sb.append("Periodo;Semestre;Grupo;Código;Asignatura;Clase;Docente;Día;Inicio;Fin;Aula;Estudiantes\n");
        List<Sesion> orden = new ArrayList<>(inst.sesiones());
        orden.sort(java.util.Comparator.comparing((Sesion x) -> x.grupo()).thenComparingInt(x -> e.franjaDe(x.id())));
        for (Sesion x : orden) {
            if (!e.estaAsignada(x.id())) continue;
            Franja f = inst.franja(e.franjaDe(x.id()));
            sb.append(String.join(";", PERIODO, String.valueOf(x.asignatura().semestre()), x.grupo(), x.asignatura().codigo(),
                    x.asignatura().nombre(), (x.indice() + 1) + " de " + x.asignatura().sesionesSemanales(),
                    x.docente().codigo() + " " + x.docente().nombre(), f.nombreDia(), String.format("%02d:00", f.horaInicio()),
                    String.format("%02d:00", f.horaFin()), inst.aula(e.aulaDe(x.id())).codigo(),
                    String.valueOf(x.seccion().estudiantes()))).append('\n');
        }
        return sb.toString();
    }

    // ================================================================ apoyo

    /** Con semilla fija (carga del escenario): el horario de ejemplo y su cruce son siempre los mismos. */
    private ServicioProgramacion.Resultado programar(Instancia inst) {
        return programar(inst, 1);
    }

    private ServicioProgramacion.Resultado programar(Instancia inst, long semilla) {
        return programar(inst, semilla, 0);
    }

    private ServicioProgramacion.Resultado programar(Instancia inst, long semilla, double variacion) {
        return programacion.programar(inst, reglas(inst), ValidadorEntrada.mejorada(),
                OpcionesBusqueda.porDefecto().conSemilla(semilla).conVariacion(variacion));
    }

    /** Las reglas activas dependen de los parámetros (por ejemplo, días no consecutivos obligatorios). */
    private static RegistroRestricciones reglas(Instancia inst) {
        return RegistroRestricciones.para(inst.parametros());
    }

    private Editor editor(Instancia inst) {
        return new Editor(inst, reglas(inst));
    }

    private Estado estadoActual(Instancia inst) {
        var h = almacen.obtener(HORARIO, inst)
                .orElseThrow(() -> new IllegalStateException("Todavía no hay un horario. Pulse «Generar horario»."));
        return Estado.desde(inst, h.franjaDe(), h.aulaDe());
    }

    private void guardar(Instancia inst, Estado e) {
        boolean conflictos = !editor(inst).conflictos(e).isEmpty();
        almacen.guardar(HORARIO, "Horario del periodo " + PERIODO, conflictos, e.copiaFranjas(), e.copiaAulas(), inst);
    }

    private HorarioDTO horarioDTO(Instancia inst, Estado e, String generadoEn) {
        List<Editor.Conflicto> conflictos = editor(inst).conflictos(e);
        List<AsignacionDTO> asignaciones = new ArrayList<>();
        for (Sesion s : inst.sesiones()) {
            if (e.estaAsignada(s.id())) asignaciones.add(new AsignacionDTO(s.id(), e.franjaDe(s.id()), e.aulaDe(s.id())));
        }
        return new HorarioDTO(false, generadoEn, asignaciones, conflictos,
                EvaluadorBlandas.evaluar(inst, e.copiaFranjas()), !historial.isEmpty(), ultimoCambio);
    }

    private static void comprobarSesion(Instancia inst, int sesion) {
        if (sesion < 0 || sesion >= inst.sesiones().size()) throw new IllegalArgumentException("Clase inexistente: " + sesion);
    }

    private static void comprobarMovimiento(Instancia inst, MovimientoDTO m) {
        comprobarSesion(inst, m.sesion());
        if (m.franja() < 0 || m.franja() >= inst.franjas().size()) throw new IllegalArgumentException("Franja inexistente");
        if (m.aula() < 0 || m.aula() >= inst.aulas().size()) throw new IllegalArgumentException("Aula inexistente");
    }

    private static FranjaDTO franja(Franja f) {
        return new FranjaDTO(f.id(), f.dia(), f.nombreDia(), String.format("%02d:00", f.horaInicio()),
                String.format("%02d:00", f.horaFin()), f.etiqueta());
    }

    // ================================================================ escenario del boceto

    /**
     * Deja a D01 con Bases de datos 2 (grupo 5A) e Ingeniería de software 2 (grupo 6A) en la misma
     * franja, de modo que el único incumplimiento sea ese cruce de docente.
     */
    static void provocarCruceDeDocente(Instancia inst, Estado e) {
        Editor editor = new Editor(inst, RegistroRestricciones.obligatorias());
        List<Sesion> mover = new ArrayList<>();
        List<Sesion> quedan = new ArrayList<>();
        for (Sesion s : inst.sesiones()) {
            if (s.seccion().codigo().equals("SIS-501-5A")) mover.add(s);
            if (s.seccion().codigo().equals("SIS-601-6A")) quedan.add(s);
        }
        if (mover.isEmpty() || quedan.isEmpty()) return;
        String docente = mover.get(0).docente().codigo();
        for (Franja f : inst.franjas()) {
            for (Sesion q : quedan) {
                int fq = e.franjaDe(q.id());
                int aq = e.aulaDe(q.id());
                int nuevaAula = fq == f.id() ? aq : -1;
                if (nuevaAula < 0) {
                    for (Aula a : inst.aulas()) {
                        if (editor.validarMovimiento(e, q.id(), f.id(), a.id()).valido()) {
                            nuevaAula = a.id();
                            break;
                        }
                    }
                }
                if (nuevaAula < 0) continue;
                e.asignar(q.id(), f.id(), nuevaAula);
                for (Sesion m : mover) {
                    for (Aula a : inst.aulas()) {
                        var v = editor.validarMovimiento(e, m.id(), f.id(), a.id());
                        if (!v.valido() && v.motivos().size() == 1 && v.motivos().get(0).startsWith("El docente " + docente)) {
                            e.asignar(m.id(), f.id(), a.id());
                            return;
                        }
                    }
                }
                e.asignar(q.id(), fq, aq);
            }
        }
    }
}
