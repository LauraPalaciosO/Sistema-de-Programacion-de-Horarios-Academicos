package co.edu.unbosque.horarios.motor.solver;

/** Todo resultado termina en uno de cuatro estados. */
public enum EstadoResultado {
    HORARIO_ENCONTRADO,
    SIN_SOLUCION,
    TIEMPO_AGOTADO,
    DATOS_INVALIDOS
}
