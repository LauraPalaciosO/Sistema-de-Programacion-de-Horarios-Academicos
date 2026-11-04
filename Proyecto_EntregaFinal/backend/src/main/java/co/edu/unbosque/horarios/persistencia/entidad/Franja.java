package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalTime;

@Entity
public class Franja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private DiaSemana dia;

    private LocalTime inicio;

    private LocalTime fin;

    protected Franja() {
    }

    public Franja(DiaSemana dia, LocalTime inicio, LocalTime fin) {
        this.dia = dia;
        this.inicio = inicio;
        this.fin = fin;
    }

    public Long getId() { return id; }
    public DiaSemana getDia() { return dia; }
    public LocalTime getInicio() { return inicio; }
    public LocalTime getFin() { return fin; }
}
