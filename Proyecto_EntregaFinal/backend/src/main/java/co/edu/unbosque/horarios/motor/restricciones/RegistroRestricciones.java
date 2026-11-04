package co.edu.unbosque.horarios.motor.restricciones;

import java.util.ArrayList;
import java.util.List;

/**
 * Único lugar que sabe qué reglas están activas. El motor y el editor reciben
 * este registro y trabajan con las abstracciones, nunca con reglas concretas.
 */
public final class RegistroRestricciones {

    private final List<RestriccionUnaria> unarias = new ArrayList<>();
    private final List<RestriccionRelacional> relacionales = new ArrayList<>();

    private RegistroRestricciones() {
    }

    /** Reglas obligatorias del MVP. */
    public static RegistroRestricciones obligatorias() {
        return obligatorias(false);
    }

    /**
     * @param diasNoConsecutivos prueba de cambio: la preferencia de días no consecutivos
     *                           se vuelve obligatoria.
     */
    public static RegistroRestricciones obligatorias(boolean diasNoConsecutivos) {
        RegistroRestricciones r = new RegistroRestricciones();
        r.unarias.add(new CaracteristicasAula());
        r.unarias.add(new CapacidadAula());
        r.unarias.add(new DisponibilidadDocente());
        r.relacionales.add(new AulaLibre());
        r.relacionales.add(new DocenteLibre());
        r.relacionales.add(new GrupoLibre());
        r.relacionales.add(new UnaClasePorDia());
        // Prueba de cambio: dos líneas para activar la nueva regla.
        if (diasNoConsecutivos)
            r.relacionales.add(new DiasNoConsecutivos());
        return r;
    }

    /** Reglas activas según los parámetros institucionales. */
    public static RegistroRestricciones para(co.edu.unbosque.horarios.motor.dominio.Parametros parametros) {
        return obligatorias(parametros.diasNoConsecutivos());
    }

    public List<RestriccionUnaria> unarias() { return unarias; }

    public List<RestriccionRelacional> relacionales() { return relacionales; }

    /** Todas las reglas activas, en el orden en que se aplican. */
    public List<Restriccion> todas() {
        List<Restriccion> r = new ArrayList<>(unarias);
        r.addAll(relacionales);
        return r;
    }

    public boolean contiene(String id) {
        return unarias.stream().anyMatch(u -> u.id().equals(id)) || relacionales.stream().anyMatch(u -> u.id().equals(id));
    }
}
