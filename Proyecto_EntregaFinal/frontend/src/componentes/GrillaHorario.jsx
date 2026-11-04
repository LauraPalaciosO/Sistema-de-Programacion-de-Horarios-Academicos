import { useState } from 'react'
import { createPortal } from 'react-dom'

/**
 * Grilla semanal del horario del periodo: filas = franjas horarias, columnas = días.
 * Es un solo horario: en «Todo el horario» se ven todas las clases (color por semestre) y los
 * filtros solo dejan ver las de un grupo, un docente o un aula.
 * Mientras se mueve una clase, las franjas válidas se pintan de verde y se pueden pulsar.
 */
export default function GrillaHorario({
  cat, idx, asignaciones, vista, enConflicto = new Set(), conAviso = new Set(), seleccionada, movida,
  validas = new Map(), destino, resaltadas = new Set(), onClase, onCelda
}) {
  const [hover, setHover] = useState(null)
  const todo = vista.modo === 'todo'

  const coincide = (a) => {
    const s = idx.sesion.get(a.sesion)
    if (!s) return false
    if (todo) return true
    if (vista.modo === 'grupo') return s.grupo === vista.valor
    if (vista.modo === 'docente') return s.docente === vista.valor
    return idx.aula.get(a.aula)?.codigo === vista.valor
  }
  const detalle = (s, aula) => {
    if (vista.modo === 'grupo') return `${s.docente} · ${aula}`
    if (vista.modo === 'docente') return `${s.grupo} · ${aula}`
    return `${s.grupo} · ${s.docente}`
  }

  const porFranja = new Map()
  for (const a of asignaciones) {
    if (!coincide(a)) continue
    if (!porFranja.has(a.franja)) porFranja.set(a.franja, [])
    porFranja.get(a.franja).push(a)
  }
  for (const lista of porFranja.values()) {
    lista.sort((x, y) => idx.sesion.get(x.sesion).grupo.localeCompare(idx.sesion.get(y.sesion).grupo))
  }

  let orden = 0
  return (
    <>
      <div className="grilla-contenedor">
        <table className={`grilla ${todo ? 'general' : ''}`}>
          <thead>
            <tr>
              <th>Franja</th>
              {cat.dias.map((d) => (
                <th key={d}>{d}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {idx.horas.map((h) => (
              <tr key={h.inicio}>
                <td className="hora">
                  {h.inicio}
                  <br />
                  {h.fin}
                </td>
                {cat.dias.map((_, dia) => {
                  const f = idx.franjaEn.get(`${dia}|${h.inicio}`)
                  if (!f) return <td key={dia} className="bloqueada" title="No hay clases el sábado en la tarde" />
                  const clases = porFranja.get(f.id) || []
                  const valida = validas.get(f.id)
                  const clase = [valida ? 'valida' : '', destino === f.id ? 'destino' : ''].join(' ')
                  return (
                    <td key={dia} className={clase} onClick={valida && onCelda ? () => onCelda(f.id) : undefined}>
                      {valida && (clases.length ? (
                        <div className={`marca-valida compacta ${valida.preferida ? 'pref' : ''}`} title={valida.preferida ? 'Franja preferida del docente' : ''}>
                          {destino === f.id ? 'Elegida' : 'Disponible'} · {idx.aula.get(valida.aula)?.codigo}
                        </div>
                      ) : (
                        <div className="marca-valida">
                          <span>{destino === f.id ? 'Elegida' : 'Disponible'}</span>
                          <span className="aula">
                            {idx.aula.get(valida.aula)?.codigo}
                            {valida.preferida && <span className="tag-pref">preferida</span>}
                          </span>
                        </div>
                      ))}
                      {clases.map((a) => {
                        const s = idx.sesion.get(a.sesion)
                        const aula = idx.aula.get(a.aula)?.codigo
                        const estilo = [
                          todo ? `mini sem${s.semestre}` : '',
                          enConflicto.has(a.sesion) ? 'conflicto' : conAviso.has(a.sesion) ? 'aviso' : '',
                          seleccionada === a.sesion ? 'seleccionada' : '',
                          movida === a.sesion ? 'movida' : '',
                          resaltadas.has(a.sesion) ? 'resaltada' : '',
                          seleccionada != null && seleccionada !== a.sesion && !resaltadas.has(a.sesion) ? 'atenuada' : ''
                        ].join(' ')
                        return (
                          <div
                            key={a.sesion}
                            className={`clase ${estilo}`}
                            style={{ animationDelay: `${Math.min(orden++ * 12, 500)}ms` }}
                            onClick={(e) => {
                              e.stopPropagation()
                              setHover(null)
                              onClase?.(a.sesion)
                            }}
                            onMouseEnter={(e) => setHover({ a, rect: e.currentTarget.getBoundingClientRect() })}
                            onMouseLeave={() => setHover(null)}
                          >
                            {todo ? (
                              <>
                                <span className="g">{s.grupo}</span>
                                <span className="n">{s.asignatura}</span>
                              </>
                            ) : (
                              <>
                                <div className="nombre">{s.asignatura}</div>
                                <div className="detalle">{detalle(s, aula)}</div>
                              </>
                            )}
                          </div>
                        )
                      })}
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <Leyenda seleccionando={seleccionada != null} todo={todo} />
      {hover && <Ficha cat={cat} idx={idx} hover={hover} conflicto={enConflicto.has(hover.a.sesion)} aviso={conAviso.has(hover.a.sesion)} />}
    </>
  )
}

function Leyenda({ seleccionando, todo }) {
  let items
  if (seleccionando) {
    items = [
      ['muestra seleccionada', 'Clase que se mueve'],
      ['muestra disponible', 'Franja donde cabe sin romper reglas'],
      ['muestra elegida', 'Franja elegida'],
      ['muestra resaltada', 'Clase con la que choca']
    ]
  } else if (todo) {
    items = [
      ['muestra sem5', '5.º semestre'],
      ['muestra sem6', '6.º semestre'],
      ['muestra sem7', '7.º semestre'],
      ['muestra conflicto', 'Incumple una regla'],
      ['muestra aviso', 'Aviso de preferencia'],
      ['muestra resaltada', 'Resaltada desde el resumen']
    ]
  } else {
    items = [
      ['muestra normal', 'Clase'],
      ['muestra conflicto', 'Incumple una regla'],
      ['muestra aviso', 'Aviso de preferencia'],
      ['muestra movida', 'Recién movida'],
      ['muestra resaltada', 'Resaltada desde el resumen']
    ]
  }
  return (
    <div className="leyenda" key={`${seleccionando}${todo}`}>
      {items.map(([c, t]) => (
        <span key={t}>
          <i className={c} /> {t}
        </span>
      ))}
      <span>
        <i className="muestra rayada" /> Sin clases
      </span>
    </div>
  )
}

/** Ficha flotante con los datos completos de la clase. */
function Ficha({ cat, idx, hover, conflicto, aviso }) {
  const { a, rect } = hover
  const s = idx.sesion.get(a.sesion)
  const sec = idx.seccion.get(s.seccion)
  const asig = idx.asignatura.get(sec.asignatura)
  const doc = cat.docentes.find((d) => d.codigo === s.docente)
  const aula = idx.aula.get(a.aula)
  const f = idx.franja.get(a.franja)
  const arriba = rect.top > 240
  const estilo = {
    left: Math.min(Math.max(rect.left + rect.width / 2, 150), window.innerWidth - 150),
    top: arriba ? rect.top - 10 : rect.bottom + 10
  }
  return createPortal(
    <div className={`ficha ${arriba ? 'arriba' : 'abajo'}`} style={estilo}>
      <div className="f-titulo">{s.asignatura}</div>
      <div className="f-sub">
        {f.nombreDia} {f.inicio} a {f.fin} · clase {s.indice + 1} de {s.de}
      </div>
      <dl>
        <dt>Docente</dt>
        <dd>{doc ? `${doc.nombre} (${doc.codigo})` : s.docente}</dd>
        <dt>Grupo</dt>
        <dd>
          {s.grupo} · {s.semestre}.º semestre · {sec.estudiantes} estudiantes
        </dd>
        <dt>Aula</dt>
        <dd>
          {aula.codigo} · {aula.capacidad} puestos{aula.caracteristicas.length ? ` · ${aula.caracteristicas.join(', ')}` : ''}
        </dd>
        {asig.requisitos.length > 0 && (
          <>
            <dt>Requiere</dt>
            <dd>{asig.requisitos.join(', ')}</dd>
          </>
        )}
      </dl>
      <div className={`f-pie ${conflicto ? 'rojo' : aviso ? 'amarillo' : ''}`}>
        {conflicto ? 'Incumple una regla. Pulse para resolver' : aviso ? 'Aviso de preferencia. Pulse para mejorarla' : 'Pulse para moverla'}
      </div>
    </div>,
    document.body
  )
}
