package co.edu.unbosque.horarios.persistencia.repositorio;

import co.edu.unbosque.horarios.persistencia.entidad.Horario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HorarioRepository extends JpaRepository<Horario, Long> {
    Optional<Horario> findByClave(String clave);
}
