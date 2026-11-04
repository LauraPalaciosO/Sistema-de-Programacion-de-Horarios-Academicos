package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.HashSet;
import java.util.Set;

@Entity
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private int capacidad;

    @ElementCollection
    @CollectionTable(name = "aula_caracteristica")
    @Column(name = "caracteristica")
    private Set<String> caracteristicas = new HashSet<>();

    protected Aula() {
    }

    public Aula(String codigo, int capacidad, Set<String> caracteristicas) {
        this.codigo = codigo;
        this.capacidad = capacidad;
        this.caracteristicas = new HashSet<>(caracteristicas);
    }

    public boolean cumple(Set<String> requisitos) {
        return caracteristicas.containsAll(requisitos);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public int getCapacidad() { return capacidad; }
    public Set<String> getCaracteristicas() { return caracteristicas; }
}
