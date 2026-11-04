package co.edu.unbosque.horarios.motor.restricciones;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

/**
 * Regla que depende solo de la sesión, la franja y el aula (no de las demás clases).
 * El motor la aplica una sola vez, al preparar las opciones iniciales.
 */
public abstract class RestriccionUnaria implements Restriccion {

    public abstract boolean permite(Sesion sesion, Franja franja, Aula aula);

    /** Motivo en lenguaje común cuando la regla no se cumple. */
    public abstract String motivo(Sesion sesion, Franja franja, Aula aula);
}
