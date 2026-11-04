package co.edu.unbosque.horarios.motor.dominio;

/** Una sección es un curso de una asignatura para un grupo, con su docente fijo. */
public record Seccion(int id, String codigo, String grupo, int estudiantes, Asignatura asignatura, Docente docente) {
}
