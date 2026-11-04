package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

/** El aula debe tener cupo para todos los estudiantes de la sección. */
public class CapacidadAula extends RestriccionUnaria {

    @Override public String id() { return "CAPACIDAD-AULA"; }

    @Override public String descripcion() { return "El aula debe tener cupo para la sección"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula) {
        return aula.capacidad() >= sesion.seccion().estudiantes();
    }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula) {
        return "El aula " + aula.codigo() + " tiene " + aula.capacidad() + " puestos y la sección tiene "
                + sesion.seccion().estudiantes() + " estudiantes.";
    }
}
