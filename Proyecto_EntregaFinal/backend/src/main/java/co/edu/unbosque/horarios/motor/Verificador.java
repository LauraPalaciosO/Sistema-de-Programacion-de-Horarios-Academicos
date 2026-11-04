package co.edu.unbosque.horarios.motor;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Verificador independiente: revisa un horario completo contra las reglas obligatorias
 * leyendo directamente los datos, sin usar las clases de restricciones del motor.
 */
public final class Verificador {

    private Verificador() {
    }

    public static List<String> verificar(Instancia inst, int[] franjaDe, int[] aulaDe, boolean diasNoConsecutivos) {
        List<String> r = new ArrayList<>();
        int n = inst.sesiones().size();
        if (franjaDe.length != n || aulaDe.length != n) {
            r.add("El horario no tiene todas las clases ubicadas");
            return r;
        }
        Map<String, Integer> ocupado = new HashMap<>();
        for (Sesion s : inst.sesiones()) {
            int f = franjaDe[s.id()];
            int a = aulaDe[s.id()];
            if (f < 0 || a < 0) {
                r.add("Clase sin ubicar: " + s.nombre() + " del grupo " + s.grupo());
                continue;
            }
            Franja fr = inst.franja(f);
            Aula au = inst.aula(a);
            String donde = " (" + fr.etiqueta() + ")";
            Integer previo;
            if ((previo = ocupado.put("A" + a + "@" + f, s.id())) != null)
                r.add("Aula " + au.codigo() + " con dos clases" + donde);
            if ((previo = ocupado.put("D" + s.docente().id() + "@" + f, s.id())) != null)
                r.add("Docente " + s.docente().codigo() + " con dos clases" + donde);
            if ((previo = ocupado.put("G" + s.grupo() + "@" + f, s.id())) != null)
                r.add("Grupo " + s.grupo() + " con dos asignaturas" + donde);
            if (!au.caracteristicas().containsAll(s.asignatura().requisitosAula()))
                r.add("Aula " + au.codigo() + " sin el equipamiento de " + s.asignatura().nombre());
            if (au.capacidad() < s.seccion().estudiantes())
                r.add("Aula " + au.codigo() + " sin cupo para la sección " + s.seccion().codigo());
            if (!s.docente().disponibles().contains(f))
                r.add("Docente " + s.docente().codigo() + " fuera de su disponibilidad" + donde);
        }
        Map<String, Integer> diaSeccion = new HashMap<>();
        for (Sesion s : inst.sesiones()) {
            int f = franjaDe[s.id()];
            if (f < 0) continue;
            if (diaSeccion.put(s.seccion().id() + "@" + inst.franja(f).dia(), s.id()) != null)
                r.add("Sección " + s.seccion().codigo() + " con dos clases el " + inst.franja(f).nombreDia().toLowerCase());
        }
        if (diasNoConsecutivos) {
            for (int sec : EvaluadorBlandas.seccionesEnDiasSeguidos(inst, franjaDe)) {
                r.add("Sección " + inst.secciones().get(sec).codigo() + " con clases en días consecutivos");
            }
        }
        return r;
    }
}
