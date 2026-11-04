package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.Docente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocenteRepository extends JpaRepository<Docente, Long> {
    List<Docente> findAllByOrderByIdAsc();
}
