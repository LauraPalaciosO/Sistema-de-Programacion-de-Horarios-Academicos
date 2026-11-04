package co.edu.unbosque.horarios.web;

import co.edu.unbosque.horarios.motor.Editor;
import co.edu.unbosque.horarios.servicio.Api.AplicacionDTO;
import co.edu.unbosque.horarios.servicio.Api.CatalogoDTO;
import co.edu.unbosque.horarios.servicio.Api.GeneracionDTO;
import co.edu.unbosque.horarios.servicio.Api.HorarioDTO;
import co.edu.unbosque.horarios.servicio.Api.MovimientoDTO;
import co.edu.unbosque.horarios.servicio.Api.ParametrosDTO;
import co.edu.unbosque.horarios.servicio.Api.ValidacionDTO;
import co.edu.unbosque.horarios.servicio.ServicioHorarios;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/** API REST que consume el frontend en React. */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class HorariosController {

    private final ServicioHorarios servicio;

    public HorariosController(ServicioHorarios servicio) {
        this.servicio = servicio;
    }

    // ---- datos del periodo
    @GetMapping("/catalogo")
    public CatalogoDTO catalogo() {
        return servicio.catalogo();
    }

    @PutMapping("/parametros")
    public CatalogoDTO parametros(@RequestBody ParametrosDTO parametros) {
        return servicio.actualizarParametros(parametros);
    }

    @PostMapping("/datos/cargar")
    public CatalogoDTO cargarDatos(@RequestParam(defaultValue = "base") String conjunto) {
        return servicio.cargarConjunto(conjunto);
    }

    // ---- horario
    @GetMapping("/horario")
    public HorarioDTO horario() {
        return servicio.horario();
    }

    @PostMapping("/horario/generar")
    public GeneracionDTO generar() {
        return servicio.generar();
    }

    @GetMapping("/horario/alternativas/{sesion}")
    public Editor.Alternativas alternativas(@PathVariable int sesion) {
        return servicio.alternativas(sesion);
    }

    @PostMapping("/horario/validar")
    public ValidacionDTO validar(@RequestBody MovimientoDTO movimiento) {
        return servicio.validar(movimiento);
    }

    @PostMapping("/horario/aplicar")
    public AplicacionDTO aplicar(@RequestBody MovimientoDTO movimiento) {
        return servicio.aplicar(movimiento);
    }

    /** RF-16: descarga el horario guardado en CSV. */
    @GetMapping("/horario/exportar.csv")
    public ResponseEntity<byte[]> exportarCsv() {
        byte[] csv = servicio.exportarCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"horario-" + ServicioHorarios.PERIODO + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }

    @PostMapping("/horario/deshacer")
    public HorarioDTO deshacer() {
        return servicio.deshacer();
    }
}
