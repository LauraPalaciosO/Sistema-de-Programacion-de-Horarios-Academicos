package co.edu.unbosque.horarios.servicio;

import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Parametros;

/**
 * Puerto de datos (inversión de dependencias): el servicio no sabe si los datos
 * vienen de la base de datos H2, de un archivo o de memoria.
 */
public interface FuenteInstancia {

    /** Lee docentes, aulas, asignaturas, secciones, franjas y parámetros. */
    Instancia cargar();

    void guardarParametros(Parametros parametros);

    /** Reemplaza todos los datos del periodo por los de la instancia dada. */
    void reemplazar(Instancia instancia);
}
