package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

/** Respetar la disponibilidad de cada docente. */
public class DisponibilidadDocente extends RestriccionUnaria {

    @Override public String id() { return "DISPONIBILIDAD-DOCENTE"; }

    @Override public String descripcion() { return "Respetar la disponibilidad de cada docente"; }

    @Override
    public boolean permite(Sesion sesion, Franja franja, Aula aula) {
        return sesion.docente().estaDisponible(franja);
    }

    @Override
    public String motivo(Sesion sesion, Franja franja, Aula aula) {
        return "El docente " + sesion.docente().codigo() + " no está disponible en esa franja.";
    }
}
