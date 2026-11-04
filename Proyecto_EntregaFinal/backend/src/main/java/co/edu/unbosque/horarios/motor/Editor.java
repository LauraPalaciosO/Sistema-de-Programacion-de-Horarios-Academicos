package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.restricciones.RestriccionRelacional;
import co.edu.unbosque.horarios.motor.restricciones.RestriccionUnaria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Editor de cambios manuales. Usa las mismas reglas que el motor (RegistroRestricciones),
 * así que una regla nueva se respeta aquí sin modificar esta clase.
 */
public final class Editor {

    /**
     * @param regla    id de la regla incumplida (DOCENTE-LIBRE, AULA-LIBRE...)
     * @param recurso  recurso involucrado (docente D01, grupo 5A...)
     * @param sesiones clases involucradas; la primera es la que se propone mover
     */
    public record Conflicto(String regla, String recurso, int franja, List<Integer> sesiones, String mensaje) {
    }

    public record Alternativa(int franja, int aula, boolean preferida) {
    }

    /**
     * @param revisadas franjas revisadas (todas menos la actual)
     * @param validas   franjas donde la clase cumple todas las reglas, en orden cronológico
     * @param sugeridas las mejores opciones válidas, para mostrarlas primero
     */
    public record Alternativas(int sesion, int revisadas, List<Alternativa> validas, List<Alternativa> sugeridas, double ms) {
    }

    /**
     * @param motivos reglas obligatorias que el cambio incumpliría (lo impiden)
     * @param avisos  preferencias que el cambio rompería (no lo impiden)
     * @param choques clases ya ubicadas con las que chocaría el cambio (para mostrarlas en la grilla)
     */
    public record Validacion(boolean valido, List<String> motivos, List<String> avisos, List<Integer> choques, double ms) {
    }

    private final Instancia inst;
    private final RegistroRestricciones reglas;

    public Editor(Instancia inst, RegistroRestricciones reglas) {
        this.inst = inst;
        this.reglas = reglas;
    }

    /**
     * Detecta los conflictos del horario recorriendo las reglas activas: el editor no conoce
     * reglas concretas, así que una regla nueva (como DiasNoConsecutivos) se detecta sola.
     * En cada conflicto, la primera clase es la que se propone mover.
     */
    public List<Conflicto> conflictos(Estado e) {
        List<Conflicto> r = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (Sesion s : inst.sesiones()) {
            int fid = e.franjaDe(s.id());
            if (fid < 0) continue;
            Franja f = inst.franja(fid);
            Aula a = inst.aula(e.aulaDe(s.id()));
            for (RestriccionUnaria u : reglas.unarias()) {
                if (!u.permite(s, f, a)) {
                    r.add(new Conflicto(u.id(), u.id(), fid, List.of(s.id()),
                            s.asignatura().nombre() + " (grupo " + s.grupo() + "): " + u.motivo(s, f, a)));
                }
            }
            for (RestriccionRelacional rel : reglas.relacionales()) {
                if (rel.permite(s, f, a, e)) continue;
                for (Sesion otra : rel.involucradas(s, f, a, e)) {
                    Sesion mover = s.id() < otra.id() ? s : otra;
                    Sesion queda = mover == s ? otra : s;
                    if (!vistos.add(rel.id() + ":" + mover.id() + ":" + queda.id())) continue;
                    Franja fm = inst.franja(e.franjaDe(mover.id()));
                    r.add(new Conflicto(rel.id(), rel.recurso(mover), fm.id(), List.of(mover.id(), queda.id()),
                            rel.describirConflicto(mover, queda, fm, e)));
                }
            }
        }
        return r;
    }

    /** Calcula en qué franjas (y con qué aula) se puede ubicar la clase sin romper ninguna regla. */
    public Alternativas alternativas(Estado original, int sesion) {
        long t0 = System.nanoTime();
        Estado e = original.copia();
        Sesion s = inst.sesion(sesion);
        int actualF = e.franjaDe(sesion);
        int actualA = e.aulaDe(sesion);
        e.desasignar(sesion);

        List<Aula> aulasOrdenadas = new ArrayList<>(inst.aulas());
        aulasOrdenadas.sort(Comparator
                .comparing((Aula a) -> a.id() != actualA)
                .thenComparingInt(a -> Math.abs(a.capacidad() - s.seccion().estudiantes())));

        List<Alternativa> validas = new ArrayList<>();
        int revisadas = 0;
        for (Franja f : inst.franjas()) {
            if (f.id() == actualF) continue;
            revisadas++;
            for (Aula a : aulasOrdenadas) {
                if (cumpleTodas(s, f, a, e)) {
                    validas.add(new Alternativa(f.id(), a.id(), s.docente().prefiere(f)));
                    break;
                }
            }
        }
        // Sugeridas: franjas preferidas por el docente y sin otra clase de la misma asignatura ese día.
        List<Alternativa> orden = new ArrayList<>(validas);
        orden.sort(Comparator.comparingInt((Alternativa a) -> costo(s, a, e)).thenComparingInt(Alternativa::franja));
        List<Alternativa> sugeridas = new ArrayList<>(orden.subList(0, Math.min(5, orden.size())));
        sugeridas.sort(Comparator.comparingInt(Alternativa::franja));
        double ms = (System.nanoTime() - t0) / 1e6;
        return new Alternativas(sesion, revisadas, validas, sugeridas, ms);
    }

    private int costo(Sesion s, Alternativa alt, Estado e) {
        int c = alt.preferida() ? 0 : 10;
        int dia = inst.franja(alt.franja()).dia();
        for (Sesion otra : inst.sesionesDe(s.seccion())) {
            if (otra.id() == s.id() || !e.estaAsignada(otra.id())) continue;
            int d = Math.abs(inst.franja(e.franjaDe(otra.id())).dia() - dia);
            // Días seguidos pesa más que la franja no preferida: así las sugeridas corrigen el aviso RB-01.
            if (d == 0) c += 20;
            else if (d == 1 && (s.asignatura().sesionesSemanales() == 2 || s.asignatura().sesionesSemanales() == 3)) c += 12;
        }
        return c;
    }

    /** Valida un cambio manual con las mismas reglas que usa el motor. */
    public Validacion validarMovimiento(Estado original, int sesion, int franja, int aula) {
        long t0 = System.nanoTime();
        Estado e = original.copia();
        Sesion s = inst.sesion(sesion);
        e.desasignar(sesion);
        Franja f = inst.franja(franja);
        Aula a = inst.aula(aula);
        List<String> motivos = new ArrayList<>();
        Set<Integer> choques = new java.util.LinkedHashSet<>();
        for (RestriccionUnaria u : reglas.unarias()) {
            if (!u.permite(s, f, a)) motivos.add(u.motivo(s, f, a));
        }
        for (RestriccionRelacional r : reglas.relacionales()) {
            if (r.permite(s, f, a, e)) continue;
            List<Sesion> otras = r.involucradas(s, f, a, e);
            motivos.add(conDetalle(r.motivo(s, f, a, e), otras, e));
            for (Sesion o : otras) choques.add(o.id());
        }
        List<String> avisos = motivos.isEmpty()
                ? EvaluadorBlandas.avisos(inst, e, s, f, reglas.contiene("DIAS-NO-CONSECUTIVOS"))
                : List.of();
        return new Validacion(motivos.isEmpty(), motivos, avisos, List.copyOf(choques), (System.nanoTime() - t0) / 1e6);
    }

    /** Agrega al motivo la clase concreta con la que se choca: asignatura, grupo, aula y día. */
    private String conDetalle(String motivo, List<Sesion> otras, Estado e) {
        if (otras.isEmpty()) return motivo;
        List<String> partes = new ArrayList<>();
        for (Sesion o : otras) {
            Franja fo = inst.franja(e.franjaDe(o.id()));
            partes.add(o.asignatura().nombre() + " del grupo " + o.grupo() + ", aula " + inst.aula(e.aulaDe(o.id())).codigo()
                    + ", " + fo.nombreDia().toLowerCase() + " " + String.format("%02d:00", fo.horaInicio()));
        }
        String base = motivo.endsWith(".") ? motivo.substring(0, motivo.length() - 1) : motivo;
        return base + ": " + String.join("; ", partes) + ".";
    }

    /** Aplica el cambio si es válido. Devuelve la validación realizada. */
    public Validacion aplicar(Estado e, int sesion, int franja, int aula) {
        Validacion v = validarMovimiento(e, sesion, franja, aula);
        if (v.valido()) e.asignar(sesion, franja, aula);
        return v;
    }

    private boolean cumpleTodas(Sesion s, Franja f, Aula a, Estado e) {
        for (RestriccionUnaria u : reglas.unarias()) if (!u.permite(s, f, a)) return false;
        for (RestriccionRelacional r : reglas.relacionales()) if (!r.permite(s, f, a, e)) return false;
        return true;
    }
}
