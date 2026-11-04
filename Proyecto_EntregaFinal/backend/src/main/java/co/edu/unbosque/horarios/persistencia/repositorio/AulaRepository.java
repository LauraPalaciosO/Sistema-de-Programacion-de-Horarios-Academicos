package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.Aula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AulaRepository extends JpaRepository<Aula, Long> {
    List<Aula> findAllByOrderByIdAsc();
}
