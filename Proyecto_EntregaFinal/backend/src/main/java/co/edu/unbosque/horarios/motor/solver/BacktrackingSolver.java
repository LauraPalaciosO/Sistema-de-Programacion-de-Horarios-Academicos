package co.edu.unbosque.horarios.motor.solver;

import co.edu.unbosque.horarios.motor.dominio.Aula;
import co.edu.unbosque.horarios.motor.dominio.Estado;
import co.edu.unbosque.horarios.motor.dominio.Franja;
import co.edu.unbosque.horarios.motor.dominio.Instancia;
import co.edu.unbosque.horarios.motor.dominio.Sesion;
import co.edu.unbosque.horarios.motor.restricciones.RegistroRestricciones;
import co.edu.unbosque.horarios.motor.restricciones.RestriccionRelacional;
import co.edu.unbosque.horarios.motor.restricciones.RestriccionUnaria;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Estrategia concreta: búsqueda con retroceso (backtracking) para el problema de
 * satisfacción de restricciones (CSP) del horario. Variables = clases semanales;
 * dominio de cada clase = pares (franja, aula).
 *
 * <ol>
 *   <li><b>Preparación de opciones iniciales:</b> las reglas unarias (aula adecuada, cupo,
 *       disponibilidad del docente) se aplican una sola vez y reducen cada dominio.</li>
 *   <li><b>MRV</b> (minimum remaining values): se elige primero la clase con menos opciones.</li>
 *   <li><b>Orden de valores:</b> primero las franjas preferidas por el docente y que no repiten
 *       día ni quedan en días seguidos (preferencias blandas).</li>
 *   <li><b>Forward checking (poda temprana):</b> al ubicar una clase, cada regla relacional
 *       descarta de las clases pendientes las opciones que dejaron de ser posibles. Si alguna
 *       clase se queda sin opciones, se retrocede de inmediato sin seguir bajando.</li>
 *   <li><b>Retroceso:</b> si ninguna opción sirve, se deshace la última decisión.</li>
 * </ol>
 *
 * Solo conoce las abstracciones {@link RestriccionUnaria} y {@link RestriccionRelacional}.
 */
public class BacktrackingSolver implements Solver {

    @Override
    public String nombre() {
        return "Búsqueda con retroceso + MRV + forward checking";
    }

    @Override
    public ResultadoSolver resolver(Instancia instancia, RegistroRestricciones reglas, OpcionesBusqueda opciones) {
        return new Busqueda(instancia, reglas, opciones, nombre()).ejecutar();
    }

    private static final class TiempoAgotado extends RuntimeException {
        TiempoAgotado() {
            super(null, null, false, false);
        }
    }

    /**
     * Estado de una ejecución. El dominio de cada clase se guarda, por franja, como una máscara
     * de bits de aulas: {@code dominio[clase][franja]} tiene el bit {@code a} encendido si el
     * aula {@code a} sigue siendo una opción.
     */
    private static final class Busqueda {
        private final Instancia inst;
        private final RegistroRestricciones reglas;
        private final OpcionesBusqueda op;
        private final String metodo;
        private final int n;
        private final int nf;
        private final long[][] dominio;
        private final int[] tamano;
        private final boolean[] asignada;
        private final Estado estado;
        private final Random rnd;
        /** Desempate de MRV: con otra semilla cambia el orden en que se eligen clases empatadas. */
        private final double[] desempate;
        /** Clases que se ubican sin mirar las preferencias (opción «variacion»). */
        private final boolean[] sinPreferencias;
        /** Cuántas veces cada regla (y recurso) dejó a una clase sin opciones: sirve para explicar fallas. */
        private final Map<String, Integer> culpables = new HashMap<>();
        private long limite;
        private int retrocesos;
        private long nodos;
        private long totalPodas;
        private int ubicadas;

        Busqueda(Instancia inst, RegistroRestricciones reglas, OpcionesBusqueda op, String metodo) {
            if (inst.aulas().size() > 64) throw new IllegalArgumentException("El prototipo admite hasta 64 aulas");
            this.inst = inst;
            this.reglas = reglas;
            this.op = op;
            this.metodo = metodo;
            this.n = inst.sesiones().size();
            this.nf = inst.franjas().size();
            this.dominio = new long[n][nf];
            this.tamano = new int[n];
            this.asignada = new boolean[n];
            this.estado = new Estado(inst);
            this.rnd = new Random(op.semillaOrden());
            this.desempate = new double[n];
            for (int i = 0; i < n; i++) desempate[i] = rnd.nextDouble();
            this.sinPreferencias = new boolean[n];
            for (int i = 0; i < n; i++) sinPreferencias[i] = rnd.nextDouble() < op.variacion();
        }

        ResultadoSolver ejecutar() {
            long t0 = System.nanoTime();
            limite = t0 + op.limiteMs() * 1_000_000L;
            prepararOpcionesIniciales();
            EstadoResultado res;
            try {
                boolean sinOpciones = false;
                for (int s = 0; s < n; s++) {
                    if (tamano[s] == 0) {
                        sinOpciones = true;
                        culpables.merge("La clase " + inst.sesion(s).nombre() + " del grupo " + inst.sesion(s).grupo()
                                + " no tiene ninguna franja y aula posibles", 1, Integer::sum);
                    }
                }
                res = !sinOpciones && buscar() ? EstadoResultado.HORARIO_ENCONTRADO : EstadoResultado.SIN_SOLUCION;
            } catch (TiempoAgotado e) {
                res = EstadoResultado.TIEMPO_AGOTADO;
            }
            boolean ok = res == EstadoResultado.HORARIO_ENCONTRADO;
            return new ResultadoSolver(metodo, res,
                    ok ? estado.copiaFranjas() : new int[0], ok ? estado.copiaAulas() : new int[0],
                    System.nanoTime() - t0, retrocesos, nodos, totalPodas, ok ? List.of() : causas());
        }

        /** Paso 1: las reglas unarias se aplican una sola vez y dejan el dominio inicial de cada clase. */
        private void prepararOpcionesIniciales() {
            for (Sesion s : inst.sesiones()) {
                for (Franja f : inst.franjas()) {
                    long m = 0;
                    for (Aula a : inst.aulas()) {
                        if (cumpleUnarias(s, f, a)) m |= 1L << a.id();
                    }
                    dominio[s.id()][f.id()] = m;
                    tamano[s.id()] += Long.bitCount(m);
                }
            }
        }

        private boolean cumpleUnarias(Sesion s, Franja f, Aula a) {
            for (RestriccionUnaria u : reglas.unarias()) {
                if (!u.permite(s, f, a)) return false;
            }
            return true;
        }

        /** Búsqueda recursiva con retroceso. */
        private boolean buscar() {
            if (ubicadas == n) return true;
            nodos++;
            if ((nodos & 63) == 0 && System.nanoTime() > limite) throw new TiempoAgotado();

            int s = seleccionarClase();
            Sesion clase = inst.sesion(s);
            for (int[] valor : ordenarValores(clase)) {
                if (System.nanoTime() > limite) throw new TiempoAgotado();
                Franja franja = inst.franja(valor[0]);
                Aula aula = inst.aula(valor[1]);
                if (!consistente(clase, franja, aula)) continue;

                estado.asignar(s, franja.id(), aula.id());
                asignada[s] = true;
                ubicadas++;
                List<int[]> podas = op.forwardChecking() ? propagar(clase, franja) : List.of();
                if (podas != null) {
                    if (buscar()) return true;
                    restaurar(podas);
                    retrocesos++;
                }
                estado.desasignar(s);
                asignada[s] = false;
                ubicadas--;
            }
            return false;
        }

        /** Paso 2 (MRV): la clase pendiente con menos opciones; sin esa técnica, la siguiente en orden. */
        private int seleccionarClase() {
            int mejor = -1;
            for (int s = 0; s < n; s++) {
                if (asignada[s]) continue;
                if (!op.menosOpcionesPrimero()) return s;
                if (mejor < 0 || tamano[s] < tamano[mejor]
                        || (tamano[s] == tamano[mejor] && desempate[s] < desempate[mejor])) mejor = s;
            }
            return mejor;
        }

        /**
         * Paso 3: ordena las opciones de una clase según las preferencias blandas: franja preferida
         * por el docente, no repetir día ni días seguidos, y el aula que mejor se ajusta al cupo.
         */
        private List<int[]> ordenarValores(Sesion s) {
            int[] diasUsados = new int[7];
            for (Sesion otra : inst.sesionesDe(s.seccion())) {
                int fo = estado.franjaDe(otra.id());
                if (fo >= 0 && otra.id() != s.id()) diasUsados[inst.franja(fo).dia()]++;
            }
            List<int[]> valores = new ArrayList<>();
            List<Double> costos = new ArrayList<>();
            long[] d = dominio[s.id()];
            for (int f = 0; f < nf; f++) {
                long bits = d[f];
                if (bits == 0) continue;
                Franja fr = inst.franja(f);
                int dia = fr.dia();
                double base = s.docente().prefiere(fr) ? 0 : 10;
                if (sinPreferencias[s.id()]) {
                    // Esta clase ignora las preferencias: cualquier opción válida vale lo mismo.
                    long b = bits;
                    while (b != 0) {
                        int a = Long.numberOfTrailingZeros(b);
                        b &= b - 1;
                        valores.add(new int[]{f, a, valores.size()});
                        costos.add(rnd.nextDouble() * 20);
                    }
                    continue;
                }
                base += 8 * diasUsados[dia];
                base += 3 * ((dia > 0 ? diasUsados[dia - 1] : 0) + diasUsados[dia + 1]);
                while (bits != 0) {
                    int a = Long.numberOfTrailingZeros(bits);
                    bits &= bits - 1;
                    double holgura = (inst.aula(a).capacidad() - s.seccion().estudiantes()) / 20.0;
                    valores.add(new int[]{f, a, valores.size()});
                    // Desempate aleatorio entre opciones igual de buenas: con otra semilla sale otro horario.
                    // Es menor que las penalizaciones de las preferencias (3, 8 y 10), así que no las sacrifica.
                    costos.add(base + holgura + rnd.nextDouble() * 2.5);
                }
            }
            valores.sort(Comparator.comparingDouble(v -> costos.get(v[2])));
            return valores;
        }

        /** Comprueba las reglas relacionales contra las clases ya ubicadas. */
        private boolean consistente(Sesion s, Franja f, Aula a) {
            for (RestriccionRelacional r : reglas.relacionales()) {
                if (!r.permite(s, f, a, estado)) {
                    if (!op.forwardChecking()) culpables.merge(r.explicacion() + " (" + r.recurso(s) + ")", 1, Integer::sum);
                    return false;
                }
            }
            return true;
        }

        /**
         * Paso 4: forward checking por regla (poda temprana). Cada regla poda solo las franjas que
         * declara en {@code afecta()} de las clases que declara en {@code relacionadas()}.
         * Devuelve las podas hechas (para deshacerlas al retroceder) o {@code null} si alguna clase
         * pendiente se quedó sin opciones.
         */
        private List<int[]> propagar(Sesion s, Franja f) {
            List<int[]> podas = new ArrayList<>();
            List<Sesion> pendientes = new ArrayList<>(n - ubicadas);
            for (Sesion t : inst.sesiones()) if (!asignada[t.id()]) pendientes.add(t);
            for (RestriccionRelacional r : reglas.relacionales()) {
                int[] franjas = r.afecta(f, inst);
                for (Sesion t : r.relacionadas(s, pendientes)) {
                    long[] dt = dominio[t.id()];
                    for (int fid : franjas) {
                        long bits = dt[fid];
                        if (bits == 0) continue;
                        Franja ft = inst.franja(fid);
                        while (bits != 0) {
                            int aid = Long.numberOfTrailingZeros(bits);
                            bits &= bits - 1;
                            if (!r.permite(t, ft, inst.aula(aid), estado)) {
                                dt[fid] &= ~(1L << aid);
                                tamano[t.id()]--;
                                podas.add(new int[]{t.id(), fid, aid});
                            }
                        }
                    }
                    if (tamano[t.id()] == 0) {
                        culpables.merge(r.explicacion() + " (" + r.recurso(t) + ")", 1, Integer::sum);
                        restaurar(podas);
                        return null;
                    }
                }
            }
            totalPodas += podas.size();
            return podas;
        }

        /** Deshace las podas al retroceder. */
        private void restaurar(List<int[]> podas) {
            for (int[] p : podas) {
                dominio[p[0]][p[1]] |= 1L << p[2];
                tamano[p[0]]++;
            }
        }

        private List<String> causas() {
            return culpables.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(e -> e.getKey() + ": bloqueó " + e.getValue() + " intentos")
                    .toList();
        }
    }
}
