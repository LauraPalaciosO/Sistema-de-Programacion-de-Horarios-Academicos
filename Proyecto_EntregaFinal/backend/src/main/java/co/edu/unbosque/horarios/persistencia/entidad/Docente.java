package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Docente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;

    /** Franjas en que el docente puede dictar (regla obligatoria). */
    @ManyToMany
    @JoinTable(name = "docente_disponibilidad")
    private Set<Franja> disponibilidad = new HashSet<>();

    /** Franjas que el docente prefiere (preferencia). */
    @ManyToMany
    @JoinTable(name = "docente_preferencia")
    private Set<Franja> preferidas = new HashSet<>();

    /** RD-08 / RF-05: asignaturas que el docente puede orientar («habilitado para» en el modelo E-R). */
    @ManyToMany
    @JoinTable(name = "docente_habilitacion")
    private Set<Asignatura> asignaturasHabilitadas = new HashSet<>();

    protected Docente() {
    }

    public Docente(String codigo, String nombre) {
        this.codigo = codigo;
        this.nombre = nombre;
    }

    public boolean estaDisponible(Franja f) {
        return disponibilidad.contains(f);
    }

    public boolean puedeDictar(Asignatura a) {
        return asignaturasHabilitadas.contains(a);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public Set<Franja> getDisponibilidad() { return disponibilidad; }
    public Set<Franja> getPreferidas() { return preferidas; }
    public Set<Asignatura> getAsignaturasHabilitadas() { return asignaturasHabilitadas; }
}
