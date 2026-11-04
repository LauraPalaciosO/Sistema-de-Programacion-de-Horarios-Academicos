package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.List;

/**
 * Prueba de cambio: la preferencia de días no consecutivos pasa a ser obligatoria
 * (asignaturas de 2 o 3 clases semanales). El mismo día ya lo prohíbe {@link UnaClasePorDia}.
 */
public class DiasNoConsecutivos extends RestriccionRelacional {

    @Override public String id() { return "DIAS-NO-CONSECUTIVOS"; }

    @Override public String descripcion() { return "Una asignatura de 2 o 3 clases semanales no puede tener clases en días consecutivos"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        int n = sesion.asignatura().sesionesSemanales();
        if (n != 2 && n != 3) return true;
        for (int dia : new int[]{franja.dia() - 1, franja.dia() + 1}) {
            for (Sesion otra : estado.enDia(dia)) {
                if (otra.seccion().id() == sesion.seccion().id() && otra.id() != sesion.id()) return false;
            }
        }
        return true;
    }

    @Override
    public int[] afecta(Franja franja, Instancia instancia) {
        return instancia.franjas().stream().filter(f -> Math.abs(f.dia() - franja.dia()) == 1).mapToInt(Franja::id).toArray();
    }

    @Override
    public List<Sesion> relacionadas(Sesion sesion, List<Sesion> pendientes) {
        List<Sesion> r = new ArrayList<>();
        for (Sesion t : pendientes) if (t.seccion().id() == sesion.seccion().id()) r.add(t);
        return r;
    }

    @Override public String recurso(Sesion sesion) { return "sección " + sesion.seccion().codigo(); }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        return sesion.asignatura().nombre() + " ya tiene clase el día anterior o el siguiente.";
    }

    @Override
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        Franja otra = estado.instancia().franja(estado.franjaDe(b.id()));
        return a.asignatura().nombre() + " (grupo " + a.grupo() + ") tiene clases el " + franja.nombreDia().toLowerCase()
                + " y el " + otra.nombreDia().toLowerCase() + ". La Facultad no permite clases en días consecutivos.";
    }
}
