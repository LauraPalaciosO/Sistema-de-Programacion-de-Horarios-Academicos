package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.List;

/**
 * Una asignatura (sección) no puede tener dos de sus clases semanales el mismo día.
 * Se agregó como clase nueva y se activó en {@link RegistroRestricciones}: el motor, el editor
 * y la detección de conflictos la aplican sin modificarse (principio abierto/cerrado).
 */
public class UnaClasePorDia extends RestriccionRelacional {

    @Override public String id() { return "UNA-CLASE-POR-DIA"; }

    @Override public String descripcion() { return "Una asignatura no puede tener dos clases el mismo día"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        for (Sesion otra : estado.enDia(franja.dia())) {
            if (otra.seccion().id() == sesion.seccion().id() && otra.id() != sesion.id()) return false;
        }
        return true;
    }

    /** Ubicar una clase bloquea todo el día para las demás clases de su sección. */
    @Override
    public int[] afecta(Franja franja, Instancia instancia) {
        return instancia.franjasDelDia(franja.dia()).stream().mapToInt(Franja::id).toArray();
    }

    /** Solo se relaciona con las clases de la misma sección. */
    @Override
    public List<Sesion> relacionadas(Sesion sesion, List<Sesion> pendientes) {
        List<Sesion> r = new ArrayList<>();
        for (Sesion t : pendientes) if (t.seccion().id() == sesion.seccion().id()) r.add(t);
        return r;
    }

    @Override public String recurso(Sesion sesion) { return "sección " + sesion.seccion().codigo(); }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        return sesion.asignatura().nombre() + " ya tiene otra clase el " + franja.nombreDia().toLowerCase() + ".";
    }

    @Override
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        return a.asignatura().nombre() + " (grupo " + a.grupo() + ") tiene dos clases el " + franja.nombreDia().toLowerCase()
                + ". Una asignatura no puede tener dos clases el mismo día.";
    }
}
