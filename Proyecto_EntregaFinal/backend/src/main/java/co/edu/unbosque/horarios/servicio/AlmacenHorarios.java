package co.edu.unbosque.horarios.servicio;

import co.edu.unbosque.horarios.motor.dominio.Instancia;

import java.time.LocalDateTime;
import java.util.Optional;

/** Puerto de persistencia de horarios (inversión de dependencias). */
public interface AlmacenHorarios {

    record HorarioGuardado(String clave, String descripcion, LocalDateTime generadoEn, int[] franjaDe, int[] aulaDe) {
    }

    void guardar(String clave, String descripcion, boolean conConflictos, int[] franjaDe, int[] aulaDe, Instancia instancia);

    Optional<HorarioGuardado> obtener(String clave, Instancia instancia);

    void borrarTodos();
}
