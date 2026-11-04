package co.edu.unbosque.horarios.motor.dominio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Asignación (posiblemente parcial) de sesiones a franja y aula.
 * La usan tanto el motor de generación como el editor de cambios manuales.
 * Admite estados con conflictos (por ejemplo, un horario editado a mano).
 */
public final class Estado {

    private final Instancia inst;
    private final int[] franjaDe;
    private final int[] aulaDe;
    private final List<List<Integer>> porFranja;
    private int asignadas;

    public Estado(Instancia inst) {
        this.inst = inst;
        int n = inst.sesiones().size();
        this.franjaDe = new int[n];
        this.aulaDe = new int[n];
        Arrays.fill(franjaDe, -1);
        Arrays.fill(aulaDe, -1);
        this.porFranja = new ArrayList<>();
        for (int i = 0; i < inst.franjas().size(); i++) porFranja.add(new ArrayList<>(8));
    }

    public static Estado desde(Instancia inst, int[] franjas, int[] aulas) {
        Estado e = new Estado(inst);
        for (int s = 0; s < franjas.length && s < inst.sesiones().size(); s++) {
            if (franjas[s] >= 0 && aulas[s] >= 0) e.asignar(s, franjas[s], aulas[s]);
        }
        return e;
    }

    public Instancia instancia() { return inst; }

    public void asignar(int sesion, int franja, int aula) {
        if (franjaDe[sesion] >= 0) desasignar(sesion);
        franjaDe[sesion] = franja;
        aulaDe[sesion] = aula;
        porFranja.get(franja).add(sesion);
        asignadas++;
    }

    public void desasignar(int sesion) {
        int f = franjaDe[sesion];
        if (f < 0) return;
        porFranja.get(f).remove(Integer.valueOf(sesion));
        franjaDe[sesion] = -1;
        aulaDe[sesion] = -1;
        asignadas--;
    }

    public boolean estaAsignada(int sesion) { return franjaDe[sesion] >= 0; }
    public int franjaDe(int sesion) { return franjaDe[sesion]; }
    public int aulaDe(int sesion) { return aulaDe[sesion]; }
    public int asignadas() { return asignadas; }
    public boolean completo() { return asignadas == franjaDe.length; }

    /** Ids de las sesiones ubicadas en una franja (vista de solo lectura, sin copiar). */
    public List<Integer> idsEn(int franja) {
        return java.util.Collections.unmodifiableList(porFranja.get(franja));
    }

    /** Sesiones ubicadas en una franja. */
    public List<Sesion> en(int franja) {
        List<Integer> ids = porFranja.get(franja);
        List<Sesion> r = new ArrayList<>(ids.size());
        for (int id : ids) r.add(inst.sesion(id));
        return r;
    }

    /** Sesiones ubicadas en cualquier franja de un día. */
    public List<Sesion> enDia(int dia) {
        List<Sesion> r = new ArrayList<>();
        for (Franja f : inst.franjasDelDia(dia)) {
            for (int id : porFranja.get(f.id())) r.add(inst.sesion(id));
        }
        return r;
    }

    public int[] copiaFranjas() { return franjaDe.clone(); }
    public int[] copiaAulas() { return aulaDe.clone(); }

    public Estado copia() {
        return desde(inst, franjaDe, aulaDe);
    }
}
