package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.List;

/**
 * Regla que relaciona una sesión con las ya ubicadas (cruces, días consecutivos...).
 * Cada regla declara qué franjas y qué sesiones puede afectar, para que el motor
 * descarte opciones sin conocer la regla (principio abierto/cerrado).
 */
public abstract class RestriccionRelacional implements Restriccion {

    public abstract boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado);

    /**
     * Franjas de otras sesiones que pueden quedar inválidas al ocupar {@code franja}.
     * Por defecto solo la misma franja (cruces). Una regla con mayor alcance lo declara aquí,
     * y el motor poda esas franjas sin conocer la regla.
     */
    public int[] afecta(Franja franja, Instancia instancia) {
        return new int[]{franja.id()};
    }

    /**
     * Sesiones pendientes que esta regla puede afectar al asignar {@code sesion}.
     * Por defecto todas; una regla que solo relaciona ciertos recursos lo restringe
     * para que la poda no revise sesiones que no comparten nada con la asignada.
     */
    public List<Sesion> relacionadas(Sesion sesion, List<Sesion> pendientes) {
        return pendientes;
    }

    /** Recurso que la regla protege para esta sesión (para explicar fallas). */
    public abstract String recurso(Sesion sesion);

    /** Motivo en lenguaje común cuando la regla no se cumple. */
    public abstract String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado);

    /** Explicación general usada cuando la regla bloquea la búsqueda. */
    public String explicacion() {
        return descripcion();
    }

    /**
     * Clases ya ubicadas que, junto con {@code sesion}, hacen que la regla no se cumpla.
     * Es genérico: revisa solo las franjas que la regla declara en {@link #afecta}, así que
     * una regla nueva obtiene la detección de conflictos sin escribir nada más.
     */
    public List<Sesion> involucradas(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        List<Sesion> r = new ArrayList<>();
        Estado copia = estado.copia();
        for (int fid : afecta(franja, estado.instancia())) {
            for (Sesion t : estado.en(fid)) {
                if (t.id() == sesion.id()) continue;
                int ft = copia.franjaDe(t.id());
                int at = copia.aulaDe(t.id());
                copia.desasignar(t.id());
                if (permite(sesion, franja, aula, copia)) r.add(t);
                copia.asignar(t.id(), ft, at);
            }
        }
        return r;
    }

    /** Mensaje en lenguaje común para un conflicto entre dos clases ya ubicadas. */
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        return a.asignatura().nombre() + " (grupo " + a.grupo() + ") y " + b.asignatura().nombre() + " (grupo "
                + b.grupo() + ") el " + franja.etiqueta() + ". " + descripcion() + ".";
    }
}
