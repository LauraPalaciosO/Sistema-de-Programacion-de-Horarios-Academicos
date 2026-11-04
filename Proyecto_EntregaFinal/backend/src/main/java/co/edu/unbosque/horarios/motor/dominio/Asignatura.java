package co.edu.unbosque.horarios.motor.dominio;

import java.util.Set;

public record Asignatura(int id, String codigo, String nombre, int semestre, int creditos,
                         int sesionesSemanales, Set<String> requisitosAula) {

    /** Regla de los datos de prueba: 4 créditos → 3 clases; 3 o 2 → 2; 1 → 1. */
    public static int sesionesPorCreditos(int creditos) {
        if (creditos >= 4) return 3;
        if (creditos >= 2) return 2;
        return 1;
    }
}
