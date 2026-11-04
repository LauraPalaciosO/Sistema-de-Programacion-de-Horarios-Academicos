package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.datos.GeneradorDatos;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Parametros;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.solver.BacktrackingSolver;
import co.edu.unbosque.horarios.motor.solver.EstadoResultado;
import co.edu.unbosque.horarios.motor.solver.OpcionesBusqueda;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas del motor sin Spring: escenarios de las Tablas 1, 5, 6 y 7 del dossier. */
class MotorTest {

    private final ServicioProgramacion servicio = new ServicioProgramacion(new BacktrackingSolver());
    private final OpcionesBusqueda opciones = OpcionesBusqueda.porDefecto();

    private ServicioProgramacion.Resultado programar(Instancia inst, boolean diasNoConsecutivos) {
        return servicio.programar(inst, RegistroRestricciones.obligatorias(diasNoConsecutivos), ValidadorEntrada.mejorada(), opciones);
    }

    @Test
    void generaHorarioCompletoSinIncumplimientos() {
        for (int semilla = 1; semilla <= 10; semilla++) {
            var r = programar(GeneradorDatos.base(semilla), false);
            assertEquals(EstadoResultado.HORARIO_ENCONTRADO, r.solver().estado());
            assertTrue(r.incumplimientos().isEmpty());
            assertTrue(r.solver().segundos() < 5);
        }
    }

    @Test
    void conElDobleDeDatosSigueDebajoDeCincoSegundos() {
        Instancia doble = GeneradorDatos.construir(GeneradorDatos.escalar(GeneradorDatos.planBase(), 2, 2), 1, Parametros.porDefecto());
        var r = programar(doble, false);
        assertEquals(EstadoResultado.HORARIO_ENCONTRADO, r.solver().estado());
        assertTrue(r.solver().segundos() < 5);
    }

    @Test
    void mrvYForwardCheckingSonNecesariosConAulasOcupadas() {
        // Doble de cursos con las mismas aulas (66 % de ocupación).
        Instancia inst = GeneradorDatos.construir(GeneradorDatos.escalar(GeneradorDatos.planBase(), 2, 1), 1, Parametros.porDefecto());
        var con = servicio.programar(inst, RegistroRestricciones.obligatorias(), ValidadorEntrada.mejorada(), opciones);
        var sin = servicio.programar(inst, RegistroRestricciones.obligatorias(), ValidadorEntrada.mejorada(),
                opciones.sinTecnicas().conLimite(2_000));
        assertEquals(EstadoResultado.HORARIO_ENCONTRADO, con.solver().estado());
        assertTrue(con.solver().podas() > 0);
        assertEquals(EstadoResultado.TIEMPO_AGOTADO, sin.solver().estado());
    }

    @Test
    void detectaDocenteSinFranjasAntesDeBuscar() {
        Instancia inst = GeneradorDatos.construir(GeneradorDatos.docenteSinFranjas(GeneradorDatos.planBase()), 1, Parametros.porDefecto());
        var r = programar(inst, false);
        assertEquals(EstadoResultado.DATOS_INVALIDOS, r.solver().estado());
        assertTrue(r.solver().causas().get(0).contains("D06"));
    }

    @Test
    void detectaGrupoNocturnoConRevisionMejorada() {
        Instancia inst = GeneradorDatos.construir(GeneradorDatos.grupoNocturno(GeneradorDatos.planBase()), 1, Parametros.porDefecto());
        var r = programar(inst, false);
        assertEquals(EstadoResultado.DATOS_INVALIDOS, r.solver().estado());
        assertTrue(r.solver().causas().get(0).contains("7A"));
    }

    @Test
    void reglaDeDiasNoConsecutivosSinTocarElMotor() {
        Instancia inst = GeneradorDatos.base(1);
        var r = programar(inst, true);
        assertEquals(EstadoResultado.HORARIO_ENCONTRADO, r.solver().estado());
        assertTrue(r.incumplimientos().isEmpty());
        assertTrue(EvaluadorBlandas.seccionesEnDiasSeguidos(inst, r.solver().franjaDe()).isEmpty());

        // El editor detecta la regla nueva sin modificarse: el horario generado sin la regla la incumple.
        var sinRegla = programar(inst, false);
        Estado e = Estado.desde(inst, sinRegla.solver().franjaDe(), sinRegla.solver().aulaDe());
        boolean habiaSeguidas = !EvaluadorBlandas.seccionesEnDiasSeguidos(inst, sinRegla.solver().franjaDe()).isEmpty();
        var conflictos = new Editor(inst, RegistroRestricciones.obligatorias(true)).conflictos(e);
        assertEquals(habiaSeguidas, conflictos.stream().anyMatch(c -> c.regla().equals("DIAS-NO-CONSECUTIVOS")));
    }

    @Test
    void unaAsignaturaNuncaTieneDosClasesElMismoDia() {
        Instancia inst = GeneradorDatos.base(1);
        var r = programar(inst, false);
        assertEquals(0, r.metricas().asignaturasRepetidasDia());
        assertTrue(r.incumplimientos().isEmpty());

        // Mover a mano una clase al mismo día que otra de su asignatura se rechaza.
        Estado e = Estado.desde(inst, r.solver().franjaDe(), r.solver().aulaDe());
        Editor editor = new Editor(inst, RegistroRestricciones.obligatorias());
        Sesion a = inst.sesiones().stream().filter(s -> s.asignatura().sesionesSemanales() >= 2).findFirst().orElseThrow();
        Sesion b = inst.sesionesDe(a.seccion()).stream().filter(s -> s.id() != a.id()).findFirst().orElseThrow();
        int dia = inst.franja(e.franjaDe(b.id())).dia();
        for (var f : inst.franjasDelDia(dia)) {
            for (var aula : inst.aulas()) {
                var v = editor.validarMovimiento(e, a.id(), f.id(), aula.id());
                assertFalse(v.valido());
                assertTrue(v.motivos().stream().anyMatch(m -> m.contains("ya tiene otra clase el")));
            }
        }
        // Y las alternativas nunca proponen ese día.
        for (var alt : editor.alternativas(e, a.id()).validas()) {
            assertTrue(inst.franja(alt.franja()).dia() != dia);
        }
    }

    @Test
    void elEditorDetectaUnCruceYSoloProponeAlternativasValidas() {
        Instancia inst = GeneradorDatos.base(1);
        var r = programar(inst, false);
        Estado e = Estado.desde(inst, r.solver().franjaDe(), r.solver().aulaDe());
        Editor editor = new Editor(inst, RegistroRestricciones.obligatorias());
        assertTrue(editor.conflictos(e).isEmpty());

        // Dos clases del mismo docente en la misma franja → conflicto de docente.
        Sesion a = inst.sesionesDe(inst.docentes().get(0)).get(0);
        Sesion b = inst.sesionesDe(inst.docentes().get(0)).stream().filter(s -> !s.grupo().equals(a.grupo())).findFirst().orElseThrow();
        e.asignar(a.id(), e.franjaDe(b.id()), e.aulaDe(a.id()));
        assertTrue(editor.conflictos(e).stream().anyMatch(c -> c.regla().equals("DOCENTE-LIBRE")));

        var alternativas = editor.alternativas(e, a.id());
        assertFalse(alternativas.validas().isEmpty());
        for (var alt : alternativas.validas()) {
            assertTrue(editor.validarMovimiento(e, a.id(), alt.franja(), alt.aula()).valido());
        }
        assertFalse(editor.validarMovimiento(e, a.id(), e.franjaDe(b.id()), e.aulaDe(a.id())).valido());
    }

    @Test
    void elAlcanceEsQuintoASeptimoCon22Asignaturas() {
        Instancia inst = GeneradorDatos.base(1);
        assertEquals(22, inst.asignaturas().size());
        assertEquals(51, inst.asignaturas().stream().mapToInt(a -> a.creditos()).sum());
        assertEquals(44, inst.secciones().size());
        assertEquals(84, inst.sesiones().size());
        assertTrue(inst.asignaturas().stream().allMatch(a -> a.semestre() >= 5 && a.semestre() <= 7));
    }

    @Test
    void rechazaDocenteNoHabilitadoAntesDeBuscar() {
        Instancia inst = GeneradorDatos.construir(GeneradorDatos.docenteNoHabilitado(GeneradorDatos.planBase()), 1, Parametros.porDefecto());
        var r = programar(inst, false);
        assertEquals(EstadoResultado.DATOS_INVALIDOS, r.solver().estado());
        assertTrue(r.solver().causas().get(0).contains("no está habilitado"));
    }

    @Test
    void elTopeDeSeccionEsObligatorio() {
        Instancia inst = GeneradorDatos.base(1).conParametros(new Parametros(35, 0.10, 10, 1, 4, false));
        var r = programar(inst, false);
        assertEquals(EstadoResultado.DATOS_INVALIDOS, r.solver().estado());
        assertEquals(9, r.solver().causas().stream().filter(c -> c.contains("supera el tope")).count());
    }

    @Test
    void lasPreferenciasDanUnPuntajeYCasosConcretos() {
        Instancia inst = GeneradorDatos.base(1);
        var m = programar(inst, false).metricas();
        assertTrue(m.puntaje() > 0 && m.puntaje() <= 100);
        assertEquals(m.asignaturasDiasSeguidos(), m.hallazgos().stream().filter(h -> h.criterio().equals("RB-01")).count());
        for (var h : m.hallazgos()) assertFalse(h.sesiones().isEmpty());
    }
}
