package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Ubicación de una clase (sección + índice de sesión) en una franja y un aula. */
@Entity
public class Asignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Horario horario;

    @ManyToOne(optional = false)
    private Seccion seccion;

    private int indiceSesion;

    @ManyToOne(optional = false)
    private Franja franja;

    @ManyToOne(optional = false)
    private Aula aula;

    protected Asignacion() {
    }

    public Asignacion(Seccion seccion, int indiceSesion, Franja franja, Aula aula) {
        this.seccion = seccion;
        this.indiceSesion = indiceSesion;
        this.franja = franja;
        this.aula = aula;
    }

    void setHorario(Horario horario) { this.horario = horario; }

    public Long getId() { return id; }
    public Horario getHorario() { return horario; }
    public Seccion getSeccion() { return seccion; }
    public int getIndiceSesion() { return indiceSesion; }
    public Franja getFranja() { return franja; }
    public Aula getAula() { return aula; }
}
