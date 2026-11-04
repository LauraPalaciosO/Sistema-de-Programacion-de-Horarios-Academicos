package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.ParametrosInstitucionales;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametrosRepository extends JpaRepository<ParametrosInstitucionales, Long> {
    Optional<ParametrosInstitucionales> findFirstByOrderByIdAsc();
}
