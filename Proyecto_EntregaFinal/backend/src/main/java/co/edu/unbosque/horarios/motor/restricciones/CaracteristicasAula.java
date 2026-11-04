package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.Set;
import java.util.TreeSet;

/** El aula debe tener el equipamiento que requiere la asignatura. */
public class CaracteristicasAula extends RestriccionUnaria {

    @Override public String id() { return "CARACTERISTICAS-AULA"; }

    @Override public String descripcion() { return "El aula debe tener el equipamiento requerido"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula) {
        return aula.cumple(sesion.asignatura().requisitosAula());
    }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula) {
        Set<String> faltan = new TreeSet<>(sesion.asignatura().requisitosAula());
        faltan.removeAll(aula.caracteristicas());
        return "El aula " + aula.codigo() + " no tiene " + String.join(" ni ", faltan) + ".";
    }
}
