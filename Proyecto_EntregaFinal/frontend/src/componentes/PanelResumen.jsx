import { useState } from 'react'
import { avisoCorto, num } from '../util.js'
import { IconoAlerta, IconoCerrar, IconoCheck } from './Iconos.jsx'

/** Indicadores del horario. Cada uno se puede abrir para ver los casos concretos. */
const INDICADORES = [
  { id: 'CONFLICTOS', nombre: 'Conflictos (reglas obligatorias)', ayuda: 'Clases que incumplen una regla obligatoria. Deben resolverse.' },
  {
    id: 'AVISOS',
    nombre: 'Avisos de preferencias',
    ayuda: 'RB-01 y RB-02. Clases que cumplen todas las reglas obligatorias, pero quedaron fuera de la franja preferida del docente o en días seguidos. Se pueden mejorar moviéndolas.'
  },
  { id: 'RB-02', nombre: 'Fuera de la franja preferida del docente', ayuda: 'RB-02. Clases en una franja que el docente no prefiere.' },
  { id: 'RB-01', nombre: 'Asignaturas en días seguidos', ayuda: 'RB-01. Asignaturas de 2 o 3 clases con clases en días consecutivos.' },
  { id: 'RB-04', nombre: 'Asignaturas muy separadas en la semana', ayuda: 'RB-04. Más de tres días entre una clase y la siguiente.' },
  { id: 'RB-05', nombre: 'Huecos en la jornada de los docentes', ayuda: 'RB-03 y RB-05. Franjas libres entre dos clases del mismo docente en un día.' },
  { id: 'RB-06', nombre: 'Días muy cargados para un grupo', ayuda: 'RB-06. Días con 4 o más clases (8 horas) para un mismo grupo.' }
]

export default function PanelResumen({ cat, idx, horario, abierto, onAbrir, onResaltar, onMover, onVer }) {
  const m = horario.metricas
  const hallazgos = m?.hallazgos ?? []
  const deCriterio = (id) => hallazgos.filter((h) => (id === 'AVISOS' ? h.criterio === 'RB-01' || h.criterio === 'RB-02' : h.criterio === id))
  const cuenta = (id) => (id === 'CONFLICTOS' ? horario.conflictos.length : deCriterio(id).length)
  const [fijo, setFijo] = useState(null)

  if (abierto) {
    const ind = INDICADORES.find((i) => i.id === abierto)
    const items =
      abierto === 'CONFLICTOS'
        ? horario.conflictos.map((c) => {
            const t = avisoCorto(c, idx)
            return { texto: `${t.quien} ${t.que}`, detalle: c.mensaje, sesiones: c.sesiones, mover: c.sesiones[0] }
          })
        : deCriterio(abierto)
            .map((h) => ({ texto: h.texto, sesiones: h.sesiones, mover: h.criterio === 'RB-05' || h.criterio === 'RB-06' ? null : h.sesiones[0], h }))
    return (
      <div className="tarjeta panel">
        <div className="cabecera">
          <h2 className="titulo-seccion">{ind.nombre}</h2>
          <button className="boton secundario icono" onClick={() => { onResaltar([]); onAbrir(null) }} title="Volver al resumen">
            <IconoCerrar />
          </button>
        </div>
        <p className="nota-pequena" style={{ marginTop: 0 }}>
          {ind.ayuda} Pase el mouse sobre un caso para verlo en el horario. La lista se actualiza con cada cambio.
        </p>
        {items.length === 0 ? (
          <div className="vacio-lista">
            <IconoCheck width={18} height={18} /> No hay casos. Este criterio se cumple por completo.
          </div>
        ) : (
          <ul className="lista-hallazgos">
            {items.map((it, i) => (
              <li
                key={`${it.texto}-${i}`}
                style={{ animationDelay: `${Math.min(i * 30, 300)}ms` }}
                className={fijo === i ? 'fijo' : ''}
                onMouseEnter={() => onResaltar(it.sesiones)}
                onMouseLeave={() => fijo == null && onResaltar([])}
              >
                <span className="texto">{it.texto}</span>
                <span className="acciones">
                  {it.mover != null ? (
                    <button className="boton pequeno" onClick={() => onMover(it.mover)}>
                      {abierto === 'CONFLICTOS' ? 'Resolver' : 'Mover clase'}
                    </button>
                  ) : (
                    <button
                      className="boton pequeno secundario"
                      onClick={() => {
                        setFijo(i)
                        onResaltar(it.sesiones)
                        onVer(it.h.docente ? { modo: 'docente', valor: it.h.docente } : { modo: 'grupo', valor: it.h.grupo })
                      }}
                    >
                      Ver en el horario
                    </button>
                  )}
                </span>
              </li>
            ))}
          </ul>
        )}
      </div>
    )
  }

  const puntaje = m?.puntaje ?? 0
  const nConflictos = horario.conflictos.length
  return (
    <div className="tarjeta resumen">
      <h2 className="titulo-seccion">Calidad del horario</h2>
      <div className="puntaje">
        <div className="cifra">
          {num(puntaje, 1)}
          <span>/ 100</span>
        </div>
        <div className="barra-puntaje">
          <span style={{ width: `${puntaje}%` }} />
        </div>
        <div className="nota-pequena">
          Puntaje de preferencias (RB-01 a RB-06). Un horario es válido si no tiene conflictos; el puntaje solo mide qué tan
          cómodo es.
        </div>
      </div>
      <ul className="indicadores">
        {INDICADORES.map((ind, i) => {
          const n = cuenta(ind.id)
          const rojo = ind.id === 'CONFLICTOS' && n > 0
          if (ind.id === 'RB-01' || ind.id === 'RB-02') return null
          return (
            <li key={ind.id} style={{ animationDelay: `${i * 40}ms` }}>
              <button className={`indicador ${rojo ? 'rojo' : n === 0 ? 'ok' : ind.id === 'AVISOS' ? 'amarillo' : ''}`} onClick={() => onAbrir(ind.id)} title={ind.ayuda}>
                <span className="icono">{n === 0 ? <IconoCheck width={14} height={14} /> : rojo ? <IconoAlerta width={14} height={14} /> : n}</span>
                <span className="nombre">{ind.nombre}</span>
                <span className="valor">{ind.id === 'RB-02' ? `${num(m?.porcentajePreferidas ?? 0, 0)} % cumple` : n === 0 ? 'Ninguno' : `${n} ${n === 1 ? 'caso' : 'casos'}`}</span>
              </button>
            </li>
          )
        })}
      </ul>
      {horario.ultimoCambio && (
        <div className="ultimo-cambio">
          <b>Último cambio:</b> {horario.ultimoCambio}
        </div>
      )}
      <div className="nota-pequena" style={{ marginTop: 8 }}>
        {nConflictos === 0 ? 'Sin conflictos. ' : ''}Guardado el {horario.generadoEn}.
      </div>
    </div>
  )
}
