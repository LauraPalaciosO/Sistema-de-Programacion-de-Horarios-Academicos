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
public class Asignatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String nombre;

    private int semestre;

    private int creditos;

    private int sesionesSemanales;

    @ElementCollection
    @CollectionTable(name = "asignatura_requisito")
    @Column(name = "requisito")
    private Set<String> requisitosAula = new HashSet<>();

    protected Asignatura() {
    }

    public Asignatura(String codigo, String nombre, int semestre, int creditos, int sesionesSemanales, Set<String> requisitosAula) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.semestre = semestre;
        this.creditos = creditos;
        this.sesionesSemanales = sesionesSemanales;
        this.requisitosAula = new HashSet<>(requisitosAula);
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getSemestre() { return semestre; }
    public int getCreditos() { return creditos; }
    public int getSesionesSemanales() { return sesionesSemanales; }
    public Set<String> getRequisitosAula() { return requisitosAula; }
}
