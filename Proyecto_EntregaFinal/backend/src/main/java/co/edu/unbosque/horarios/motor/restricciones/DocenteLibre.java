package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.List;

/** Un docente no puede tener dos clases a la vez. */
public class DocenteLibre extends RestriccionRelacional {

    @Override public String id() { return "DOCENTE-LIBRE"; }

    @Override public String descripcion() { return "Un docente no puede tener dos clases a la vez"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        int docente = sesion.docente().id();
        for (int otra : estado.idsEn(franja.id())) {
            if (otra != sesion.id() && estado.instancia().sesion(otra).docente().id() == docente) return false;
        }
        return true;
    }

    @Override
    public List<Sesion> relacionadas(Sesion sesion, List<Sesion> pendientes) {
        List<Sesion> r = new ArrayList<>();
        for (Sesion t : pendientes) if (t.docente().id() == sesion.docente().id()) r.add(t);
        return r;
    }

    @Override public String recurso(Sesion sesion) { return "docente " + sesion.docente().codigo(); }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        return "El docente " + sesion.docente().codigo() + " ya dicta otra clase en esa franja.";
    }

    @Override public String explicacion() { return "Un docente no puede tener dos sesiones simultáneas"; }

    @Override
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        return "El docente " + a.docente().codigo() + " está asignado a " + a.asignatura().nombre() + " (grupo " + a.grupo()
                + ") y a " + b.asignatura().nombre() + " (grupo " + b.grupo() + ") el " + franja.etiqueta()
                + ". Un docente no puede dictar dos clases al mismo tiempo.";
    }
}
