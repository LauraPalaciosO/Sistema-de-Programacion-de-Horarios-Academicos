package co.edu.unbosque.horarios;

import co.edu.unbosque.horarios.motor.ServicioProgramacion;
import co.edu.unbosque.horarios.motor.solver.BacktrackingSolver;
import co.edu.unbosque.horarios.motor.solver.Solver;
import co.edu.unbosque.horarios.servicio.AlmacenHorarios;
import co.edu.unbosque.horarios.servicio.FuenteInstancia;
import co.edu.unbosque.horarios.servicio.ServicioHorarios;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla las piezas. Aquí se elige el método de generación (patrón Strategy):
 * para usar otro solucionador (por ejemplo, CP-SAT) basta con cambiar el bean {@link Solver};
 * ServicioProgramacion, el editor y la interfaz no cambian.
 */
@Configuration
public class ConfiguracionAplicacion {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionAplicacion.class);

    @Bean
    public Solver solver() {
        return new BacktrackingSolver();
    }

    @Bean
    public ServicioProgramacion servicioProgramacion(Solver solver) {
        return new ServicioProgramacion(solver);
    }

    @Bean
    public ServicioHorarios servicioHorarios(FuenteInstancia fuente, AlmacenHorarios almacen, ServicioProgramacion programacion) {
        return new ServicioHorarios(fuente, almacen, programacion);
    }

    /** Al arrancar carga en H2 los datos de ejemplo del periodo y su horario guardado. */
    @Bean
    public CommandLineRunner inicializarDatos(ServicioHorarios servicio) {
        return args -> {
            servicio.cargarConjunto("base");
            log.info("Datos de ejemplo cargados en H2. Abra http://localhost:8080 en el navegador");
        };
    }
}
