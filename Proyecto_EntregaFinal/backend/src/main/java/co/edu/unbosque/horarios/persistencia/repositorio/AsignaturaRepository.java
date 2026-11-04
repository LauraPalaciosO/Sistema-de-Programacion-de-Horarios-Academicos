package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.Asignatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsignaturaRepository extends JpaRepository<Asignatura, Long> {
    List<Asignatura> findAllByOrderByIdAsc();
}
