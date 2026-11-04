package co.edu.unbosque.horarios.persistencia.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Sección: un curso de una asignatura para un grupo, con su docente fijo (decisión del MVP). */
@Entity
public class Seccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String codigo;

    private String grupo;

    private int estudiantes;

    @ManyToOne(optional = false)
    private Asignatura asignatura;

    @ManyToOne(optional = false)
    private Docente docente;

    protected Seccion() {
    }

    public Seccion(String codigo, String grupo, int estudiantes, Asignatura asignatura, Docente docente) {
        this.codigo = codigo;
        this.grupo = grupo;
        this.estudiantes = estudiantes;
        this.asignatura = asignatura;
        this.docente = docente;
    }

    public Long getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getGrupo() { return grupo; }
    public int getEstudiantes() { return estudiantes; }
    public Asignatura getAsignatura() { return asignatura; }
    public Docente getDocente() { return docente; }
}
