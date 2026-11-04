package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

/** Un aula no puede tener dos clases a la vez. */
public class AulaLibre extends RestriccionRelacional {

    @Override public String id() { return "AULA-LIBRE"; }

    @Override public String descripcion() { return "Un aula no puede tener dos clases a la vez"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        for (int otra : estado.idsEn(franja.id())) {
            if (otra != sesion.id() && estado.aulaDe(otra) == aula.id()) return false;
        }
        return true;
    }

    @Override public String recurso(Sesion sesion) { return "aulas"; }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula, Estado estado) {
        return "El aula " + aula.codigo() + " ya está ocupada por otra clase en esa franja.";
    }

    @Override public String explicacion() { return "Un aula no puede tener dos sesiones simultáneas"; }

    @Override
    public String describirConflicto(Sesion a, Sesion b, Franja franja, Estado estado) {
        String aula = estado.instancia().aula(estado.aulaDe(a.id())).codigo();
        return "El aula " + aula + " tiene dos clases el " + franja.etiqueta() + ": " + a.asignatura().nombre() + " (grupo "
                + a.grupo() + ") y " + b.asignatura().nombre() + " (grupo " + b.grupo() + "). Un aula no puede tener dos clases a la vez.";
    }
}
