package co.edu.unbosque.horarios.motor.dominio;

import java.util.Set;

/**
 * @param disponibles ids de las franjas en que el docente puede dictar (RD-09, obligatoria)
 * @param preferidas  ids de las franjas que el docente prefiere (RB-02, preferencia)
 * @param habilitadas ids de las asignaturas que el docente puede orientar (RD-08)
 */
public record Docente(int id, String codigo, String nombre, Set<Integer> disponibles, Set<Integer> preferidas,
                      Set<Integer> habilitadas) {

    public boolean estaDisponible(Franja f) {
        return disponibles.contains(f.id());
    }

    public boolean prefiere(Franja f) {
        return preferidas.contains(f.id());
    }

    /** RD-08: el docente solo puede orientar asignaturas para las que está habilitado. */
    public boolean puedeDictar(Asignatura a) {
        return habilitadas.contains(a.id());
    }
}
