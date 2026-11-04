package co.edu.unbosque.horarios.motor.solver;

import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;

/**
 * Patrón Strategy (Tabla 4): todos los métodos de generación reciben los mismos datos
 * y devuelven un resultado verificable. ServicioProgramacion recibe el método que debe usar.
 */
public interface Solver {

    String nombre();

    ResultadoSolver resolver(Instancia instancia, RegistroRestricciones reglas, OpcionesBusqueda opciones);
}
