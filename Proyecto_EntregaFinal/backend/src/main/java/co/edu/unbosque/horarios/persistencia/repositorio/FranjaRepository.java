package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.Franja;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FranjaRepository extends JpaRepository<Franja, Long> {
    List<Franja> findAllByOrderByIdAsc();
}
