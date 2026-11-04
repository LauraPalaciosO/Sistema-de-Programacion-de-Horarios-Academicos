package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.solver.EstadoResultado;
import co.edu.unbosque.horarios.motor.solver.OpcionesBusqueda;
import co.edu.unbosque.horarios.motor.solver.ResultadoSolver;
import co.edu.unbosque.horarios.motor.solver.Solver;

import java.util.List;

/**
 * Coordina la programación: revisión previa, método de generación (Strategy),
 * verificación independiente y evaluación de preferencias.
 * No sabe cómo funciona el método que recibe.
 */
public final class ServicioProgramacion {

    public record Resultado(ResultadoSolver solver, List<ValidadorEntrada.Problema> revision, double msRevision,
                            List<String> incumplimientos, EvaluadorBlandas.Metricas metricas) {
    }

    private final Solver solver;
    private final int reintentos;

    public ServicioProgramacion(Solver solver) {
        this(solver, 2);
    }

    /**
     * @param reintentos si la búsqueda agota el tiempo, cuántas veces más se intenta con otro orden
     *                   de búsqueda (otra semilla). El tiempo total se reparte entre los intentos.
     */
    public ServicioProgramacion(Solver solver, int reintentos) {
        this.solver = solver;
        this.reintentos = Math.max(0, reintentos);
    }

    public Solver solver() {
        return solver;
    }

    public Resultado programar(Instancia inst, RegistroRestricciones reglas, ValidadorEntrada validador,
                               OpcionesBusqueda opciones) {
        long t0 = System.nanoTime();
        List<ValidadorEntrada.Problema> revision = validador.revisar(inst, reglas);
        long nanosRevision = System.nanoTime() - t0;
        double msRevision = nanosRevision / 1e6;
        if (ValidadorEntrada.hayBloqueantes(revision)) {
            List<String> causas = revision.stream().filter(ValidadorEntrada.Problema::bloqueante)
                    .map(ValidadorEntrada.Problema::mensaje).toList();
            return new Resultado(ResultadoSolver.datosInvalidos(nanosRevision, causas), revision, msRevision, List.of(), null);
        }
        ResultadoSolver r = resolverConReintentos(inst, reglas, opciones);
        if (!r.encontrado()) return new Resultado(r, revision, msRevision, List.of(), null);
        boolean dnc = reglas.contiene("DIAS-NO-CONSECUTIVOS");
        List<String> incumplimientos = Verificador.verificar(inst, r.franjaDe(), r.aulaDe(), dnc);
        return new Resultado(r, revision, msRevision, incumplimientos, EvaluadorBlandas.evaluar(inst, r.franjaDe()));
    }

    /**
     * Reintentos con otro orden de búsqueda (propuesta de la sección G): un mal orden inicial puede
     * hacer que el retroceso se quede atascado; otra semilla cambia el desempate de MRV y de los valores.
     * Solo se reintenta si se agotó el tiempo: «sin solución» es definitivo.
     */
    private ResultadoSolver resolverConReintentos(Instancia inst, RegistroRestricciones reglas, OpcionesBusqueda opciones) {
        int intentos = 1 + reintentos;
        long porIntento = Math.max(500, opciones.limiteMs() / intentos);
        long nanos = 0;
        ResultadoSolver r = null;
        for (int k = 0; k < intentos; k++) {
            r = solver.resolver(inst, reglas, opciones.conLimite(porIntento).conSemilla(opciones.semillaOrden() + 7919L * k));
            nanos += r.nanos();
            if (r.estado() != EstadoResultado.TIEMPO_AGOTADO) break;
        }
        return new ResultadoSolver(r.metodo(), r.estado(), r.franjaDe(), r.aulaDe(), nanos, r.retrocesos(), r.nodos(),
                r.podas(), r.causas());
    }
}
