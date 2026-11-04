package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ParametrosInstitucionales {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int tamanoObjetivo = 40;

    private double tolerancia = 0.10;

    private int minimoEstudiantes = 10;

    private int sesionesMin = 1;

    private int sesionesMax = 4;

    /** Prueba de cambio: la preferencia de días no consecutivos se vuelve obligatoria. */
    private boolean diasNoConsecutivos = false;

    public ParametrosInstitucionales() {
    }

    public int tamanoMaximo() {
        return (int) Math.floor(tamanoObjetivo * (1 + tolerancia) + 1e-9);
    }

    public Long getId() { return id; }
    public int getTamanoObjetivo() { return tamanoObjetivo; }
    public void setTamanoObjetivo(int v) { this.tamanoObjetivo = v; }
    public double getTolerancia() { return tolerancia; }
    public void setTolerancia(double v) { this.tolerancia = v; }
    public int getMinimoEstudiantes() { return minimoEstudiantes; }
    public void setMinimoEstudiantes(int v) { this.minimoEstudiantes = v; }
    public int getSesionesMin() { return sesionesMin; }
    public void setSesionesMin(int v) { this.sesionesMin = v; }
    public int getSesionesMax() { return sesionesMax; }
    public void setSesionesMax(int v) { this.sesionesMax = v; }
    public boolean isDiasNoConsecutivos() { return diasNoConsecutivos; }
    public void setDiasNoConsecutivos(boolean v) { this.diasNoConsecutivos = v; }
}
