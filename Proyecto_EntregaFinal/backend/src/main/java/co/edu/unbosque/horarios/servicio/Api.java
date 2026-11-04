package co.edu.unbosque.horarios.servicio;

import co.edu.unbosque.horarios.motor.Editor;
import co.edu.unbosque.horarios.motor.EvaluadorBlandas;
import co.edu.unbosque.horarios.motor.ValidadorEntrada;

import java.util.List;

/** Objetos que viajan por la API REST (JSON). */
public final class Api {

    private Api() {
    }

    // ---------------------------------------------------------------- catálogo

    public record FranjaDTO(int id, int dia, String nombreDia, String inicio, String fin, String etiqueta) {
    }

    public record AulaDTO(int id, String codigo, int capacidad, List<String> caracteristicas) {
    }

    public record DocenteDTO(int id, String codigo, String nombre, boolean nocturno, List<Integer> disponibles,
                             List<Integer> preferidas, List<Integer> habilitadas, int sesiones) {
    }

    public record AsignaturaDTO(int id, String codigo, String nombre, int semestre, int creditos,
                                int sesionesSemanales, List<String> requisitos) {
    }

    public record SeccionDTO(int id, String codigo, String grupo, int estudiantes, int asignatura, int docente) {
    }

    /** Una clase semanal concreta (sección + número de clase). */
    public record SesionDTO(int id, int seccion, int indice, int de, String asignatura, String grupo, int semestre,
                            String docente, String nombre) {
    }

    public record ParametrosDTO(int tamanoObjetivo, double tolerancia, int minimoEstudiantes,
                                int sesionesMin, int sesionesMax, boolean diasNoConsecutivos, int tamanoMaximo) {
    }

    public record ResumenDTO(int docentes, int aulas, int secciones, int clases, int franjas, double ocupacion) {
    }

    /**
     * Escenario de prueba que el coordinador puede cargar.
     *
     * @param esperado qué debería pasar al pulsar «Generar horario» con estos datos
     */
    public record ConjuntoDTO(String id, String nombre, String descripcion, String esperado, boolean tieneSolucion) {
    }

    public record CatalogoDTO(String periodo, String conjunto, List<String> dias, List<FranjaDTO> franjas,
                              List<AulaDTO> aulas, List<DocenteDTO> docentes, List<AsignaturaDTO> asignaturas,
                              List<SeccionDTO> secciones, List<SesionDTO> sesiones, List<String> grupos,
                              ParametrosDTO parametros, List<ValidadorEntrada.Problema> advertencias,
                              ResumenDTO resumen, List<ConjuntoDTO> conjuntos) {
    }


    // ---------------------------------------------------------------- horario

    public record AsignacionDTO(int sesion, int franja, int aula) {
    }

    /**
     * Horario guardado del periodo.
     *
     * @param vacio verdadero si todavía no se ha generado ningún horario
     */
    public record HorarioDTO(boolean vacio, String generadoEn, List<AsignacionDTO> asignaciones,
                             List<Editor.Conflicto> conflictos, EvaluadorBlandas.Metricas metricas,
                             boolean puedeDeshacer, String ultimoCambio) {
    }

    /** Resultado de pulsar «Generar horario». */
    public record GeneracionDTO(String estado, String metodo, double segundos, double msRevision, int retrocesos,
                                long podas, List<String> causas, HorarioDTO horario) {
    }

    public record MovimientoDTO(int sesion, int franja, int aula) {
    }

    public record ValidacionDTO(boolean valido, List<String> motivos, List<String> avisos, List<Integer> choques, double ms,
                                String destino) {
    }

    public record AplicacionDTO(ValidacionDTO validacion, HorarioDTO horario) {
    }
}
