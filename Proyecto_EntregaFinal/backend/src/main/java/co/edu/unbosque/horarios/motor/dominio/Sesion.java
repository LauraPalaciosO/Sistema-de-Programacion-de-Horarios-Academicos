package co.edu.unbosque.horarios.motor.dominio;

/** Una clase semanal concreta de una sección (índice 0..sesionesSemanales-1). */
public record Sesion(int id, Seccion seccion, int indice) {

    public Docente docente() {
        return seccion.docente();
    }

    public String grupo() {
        return seccion.grupo();
    }

    public Asignatura asignatura() {
        return seccion.asignatura();
    }

    /** Ej.: "Bases de datos 2 (clase 1 de 3)". */
    public String nombre() {
        return asignatura().nombre() + " (clase " + (indice + 1) + " de " + asignatura().sesionesSemanales() + ")";
    }
}
