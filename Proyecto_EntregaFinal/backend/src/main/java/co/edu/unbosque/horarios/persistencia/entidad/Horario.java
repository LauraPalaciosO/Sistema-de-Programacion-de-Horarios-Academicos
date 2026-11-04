package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifica el horario dentro de la aplicación (GENERADO, CONFLICTO...). */
    @Column(unique = true, nullable = false)
    private String clave;

    private String descripcion;

    private String periodo;

    private LocalDateTime generadoEn;

    @Enumerated(EnumType.STRING)
    private EstadoHorario estado;

    @OneToMany(mappedBy = "horario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Asignacion> asignaciones = new ArrayList<>();

    protected Horario() {
    }

    public Horario(String clave, String descripcion, String periodo) {
        this.clave = clave;
        this.descripcion = descripcion;
        this.periodo = periodo;
    }

    public void reemplazarAsignaciones(List<Asignacion> nuevas) {
        asignaciones.clear();
        for (Asignacion a : nuevas) {
            a.setHorario(this);
            asignaciones.add(a);
        }
    }

    public Long getId() { return id; }
    public String getClave() { return clave; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getPeriodo() { return periodo; }
    public LocalDateTime getGeneradoEn() { return generadoEn; }
    public void setGeneradoEn(LocalDateTime generadoEn) { this.generadoEn = generadoEn; }
    public EstadoHorario getEstado() { return estado; }
    public void setEstado(EstadoHorario estado) { this.estado = estado; }
    public List<Asignacion> getAsignaciones() { return asignaciones; }
}
