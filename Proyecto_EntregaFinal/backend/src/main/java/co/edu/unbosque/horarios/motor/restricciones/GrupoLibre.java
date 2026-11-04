package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.List;

/** Un grupo (cohorte) no puede tener dos asignaturas a la vez. */
public class GrupoLibre extends RestriccionRelacional {

    @Override public String id() { return "GRUPO-LIBRE"; }

    @Override public String descripcion() { return "Un grupo no puede tener dos asignaturas a la vez"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        for (int otra : estado.idsEn(franja.id())) {
            if (otra != sesion.id() && estado.instancia().sesion(otra).grupo().equals(sesion.grupo())) return false;
        }
        return true;
    }

    @Override
    public List<Sesion> relacionadas(Sesion sesion, List<Sesion> pendientes) {
        List<Sesion> r = new ArrayList<>();
        for (Sesion t : pendientes) if (t.grupo().equals(sesion.grupo())) r.add(t);
        return r;
    }

    @Override public String recurso(Sesion sesion) { return "grupo " + sesion.grupo(); }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        return "El grupo " + sesion.grupo() + " ya tiene otra asignatura en esa franja.";
    }

    @Override public String explicacion() { return "Un grupo no puede tener dos sesiones simultáneas"; }

    @Override
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        return "El grupo " + a.grupo() + " tiene " + a.asignatura().nombre() + " y " + b.asignatura().nombre() + " el "
                + franja.etiqueta() + ". Un grupo no puede tener dos asignaturas a la vez.";
    }
}
