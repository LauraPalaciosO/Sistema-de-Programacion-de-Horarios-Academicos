package co.edu.unbosque.horarios.motor.dominio;

/**
 * Parámetros institucionales (Figura 1: ParametrosInstitucionales).
 *
 * @param diasNoConsecutivos si es verdadero, la preferencia de no programar una asignatura en días
 *                           consecutivos se vuelve obligatoria (prueba de cambio, Tabla 5)
 */
public record Parametros(int tamanoObjetivo, double tolerancia, int minimoEstudiantes, int sesionesMin, int sesionesMax,
                         boolean diasNoConsecutivos) {

    public static Parametros porDefecto() {
        return new Parametros(40, 0.10, 10, 1, 4, false);
    }

    public int tamanoMaximo() {
        return (int) Math.floor(tamanoObjetivo * (1 + tolerancia) + 1e-9);
    }
}
