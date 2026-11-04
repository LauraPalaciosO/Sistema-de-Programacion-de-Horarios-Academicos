package co.edu.unbosque.horarios.motor.restricciones;

/** Regla obligatoria del M1. El motor y el editor solo conocen esta abstracción. */
public interface Restriccion {

    /** Identificador corto, por ejemplo "DOCENTE-LIBRE". */
    String id();

    /** La regla tal como aparece en el M1. */
    String descripcion();
}
