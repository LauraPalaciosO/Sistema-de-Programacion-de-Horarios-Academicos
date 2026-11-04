package co.edu.unbosque.horarios.motor.solver;

/**
 * Parámetros de la búsqueda. Las variantes del retroceso no justifican estrategias
 * separadas; bastan estos parámetros (contraste con la hipótesis de la Fase 1).
 *
 * @param menosOpcionesPrimero heurística MRV: elegir primero la clase con menos opciones
 * @param forwardChecking      poda temprana: al ubicar una clase, descartar de antemano las
 *                             opciones que dejan de ser posibles para las clases pendientes
 * @param limiteMs             tiempo máximo de búsqueda
 * @param semillaOrden         semilla para desempatar el orden de los valores
 * @param variacion            proporción (0 a 1) de clases que se ubican sin tener en cuenta las
 *                             preferencias (RB-01, RB-02). Solo afecta el orden de las opciones:
 *                             las reglas obligatorias se cumplen siempre. Con 0 el motor busca la
 *                             mejor calidad; con más, deja avisos de preferencias para corregir a mano.
 */
public record OpcionesBusqueda(boolean menosOpcionesPrimero, boolean forwardChecking, long limiteMs, long semillaOrden,
                               double variacion) {

    public static OpcionesBusqueda porDefecto() {
        return new OpcionesBusqueda(true, true, 5_000, 1, 0);
    }

    public OpcionesBusqueda conLimite(long ms) {
        return new OpcionesBusqueda(menosOpcionesPrimero, forwardChecking, ms, semillaOrden, variacion);
    }

    public OpcionesBusqueda conSemilla(long semilla) {
        return new OpcionesBusqueda(menosOpcionesPrimero, forwardChecking, limiteMs, semilla, variacion);
    }

    public OpcionesBusqueda conVariacion(double proporcion) {
        return new OpcionesBusqueda(menosOpcionesPrimero, forwardChecking, limiteMs, semillaOrden,
                Math.max(0, Math.min(1, proporcion)));
    }

    /** Búsqueda con retroceso simple, sin MRV ni forward checking (para comparar). */
    public OpcionesBusqueda sinTecnicas() {
        return new OpcionesBusqueda(false, false, limiteMs, semillaOrden, variacion);
    }
}
