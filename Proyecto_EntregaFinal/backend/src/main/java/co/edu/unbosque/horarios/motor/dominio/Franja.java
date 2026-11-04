package co.edu.unbosque.horarios.motor.dominio;

/**
 * Franja de dos horas. {@code id} es el índice dentro de la instancia (0..n-1),
 * {@code dia} va de 0 (lunes) a 5 (sábado) y las horas son enteras (7 = 07:00).
 */
public record Franja(int id, int dia, int horaInicio, int horaFin) {

    public static final String[] DIAS = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado"};

    public String nombreDia() {
        return DIAS[dia];
    }

    public String rango() {
        return String.format("%02d:00 a %02d:00", horaInicio, horaFin);
    }

    /** Ej.: "lunes de 07:00 a 09:00". */
    public String etiqueta() {
        return nombreDia().toLowerCase() + " de " + rango();
    }

    public boolean nocturna() {
        return horaInicio >= 17;
    }
}
