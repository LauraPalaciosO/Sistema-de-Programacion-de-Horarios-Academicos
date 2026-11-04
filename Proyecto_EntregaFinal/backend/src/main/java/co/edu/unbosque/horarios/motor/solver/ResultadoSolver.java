package co.edu.unbosque.horarios.motor.solver;

import java.util.List;

/**
 * Resultado común a todos los métodos de generación (contrato del patrón Strategy).
 *
 * @param franjaDe   franja asignada a cada clase (vacío si no hay horario)
 * @param aulaDe     aula asignada a cada clase (vacío si no hay horario)
 * @param retrocesos decisiones deshechas durante la búsqueda
 * @param podas      opciones descartadas por forward checking
 * @param causas     explicación en lenguaje común cuando no hay horario
 */
public record ResultadoSolver(String metodo, EstadoResultado estado, int[] franjaDe, int[] aulaDe,
                              long nanos, int retrocesos, long nodos, long podas, List<String> causas) {

    public double segundos() {
        return nanos / 1e9;
    }

    public boolean encontrado() {
        return estado == EstadoResultado.HORARIO_ENCONTRADO;
    }

    public static ResultadoSolver datosInvalidos(long nanos, List<String> causas) {
        return new ResultadoSolver("Revisión previa", EstadoResultado.DATOS_INVALIDOS, new int[0], new int[0],
                nanos, 0, 0, 0, causas);
    }
}
