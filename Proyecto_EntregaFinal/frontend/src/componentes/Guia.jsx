import { IconoAlerta, IconoBaseDatos, IconoCheck, IconoFlecha, IconoGenerar } from './Iconos.jsx'

/**
 * Progreso del periodo: le dice al coordinador en qué paso está y qué hacer a continuación.
 * Datos cargados → Horario generado → Conflictos resueltos → Listo.
 */
export default function Guia({ cat, horario, idx, onGenerar, onRevisar, onDatos, onAvisos }) {
  const conflictos = horario.conflictos.length
  const avisos = (horario.metricas?.hallazgos ?? []).filter((h) => h.criterio === 'RB-01' || h.criterio === 'RB-02').length
  const pasos = [
    { titulo: 'Datos cargados', detalle: `${cat.resumen.secciones} secciones · ${cat.resumen.clases} clases`, hecho: true, icono: <IconoBaseDatos width={14} height={14} /> },
    { titulo: 'Horario generado', detalle: horario.vacio ? 'Pendiente' : `Guardado el ${horario.generadoEn}`, hecho: !horario.vacio, icono: <IconoGenerar width={14} height={14} /> },
    {
      titulo: 'Conflictos resueltos',
      detalle: horario.vacio ? 'Pendiente' : conflictos ? `${conflictos} por resolver` : 'Ninguno',
      hecho: !horario.vacio && conflictos === 0,
      alerta: !horario.vacio && conflictos > 0,
      icono: <IconoAlerta width={14} height={14} />
    },
    { titulo: 'Horario listo', detalle: !horario.vacio && conflictos === 0 ? 'Cumple todas las reglas' : 'Aún no', hecho: !horario.vacio && conflictos === 0, icono: <IconoCheck width={14} height={14} /> }
  ]
  const actual = pasos.findIndex((p) => !p.hecho)

  let siguiente
  if (horario.vacio) {
    siguiente = (
      <>
        <span>
          <b>Siguiente paso:</b> genere el horario del periodo.
        </span>
        <button className="boton pequeno" onClick={onGenerar}>
          <IconoGenerar width={14} height={14} /> Generar horario
        </button>
      </>
    )
  } else if (conflictos > 0) {
    const c = horario.conflictos[0]
    const s = idx.sesion.get(c.sesiones[0])
    siguiente = (
      <>
        <span>
          <b>Siguiente paso:</b> resuelva el conflicto de <b>{s.asignatura}</b> ({s.grupo}). Pulse la clase en rojo o «Revisar».
        </span>
        <button className="boton pequeno rojo" onClick={() => onRevisar(c.sesiones[0])}>
          Revisar <IconoFlecha width={14} height={14} />
        </button>
      </>
    )
  } else if (avisos > 0) {
    siguiente = (
      <>
        <span>
          <b>Sin conflictos.</b> Hay {avisos} {avisos === 1 ? 'aviso' : 'avisos'} de preferencias (marcados en amarillo): el
          horario es válido, pero puede mejorarlo moviendo esas clases.
        </span>
        <button className="boton pequeno aviso" onClick={onAvisos}>
          Ver avisos <IconoFlecha width={14} height={14} />
        </button>
      </>
    )
  } else {
    siguiente = (
      <>
        <span>
          <b>Todo en orden.</b> Puede pulsar cualquier clase para moverla, o cambiar las reglas y los datos.
        </span>
        <button className="boton pequeno secundario" onClick={onDatos}>
          Datos <IconoFlecha width={14} height={14} />
        </button>
      </>
    )
  }

  return (
    <div className="guia">
      <ol className="pasos-periodo">
        {pasos.map((p, i) => (
          <li key={p.titulo} className={`${p.hecho ? 'hecho' : ''} ${i === actual ? 'actual' : ''} ${p.alerta ? 'alerta' : ''}`}>
            <span className="bolita">{p.hecho ? <IconoCheck width={13} height={13} /> : i + 1}</span>
            <span className="textos">
              <span className="t">{p.titulo}</span>
              <span className="d">{p.detalle}</span>
            </span>
          </li>
        ))}
      </ol>
      <div className={`siguiente ${horario.vacio ? '' : conflictos ? 'rojo' : avisos ? 'amarillo' : 'verde'}`}>{siguiente}</div>
    </div>
  )
}
