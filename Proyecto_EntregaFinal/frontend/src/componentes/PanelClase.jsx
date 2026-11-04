import { useEffect, useRef, useState } from 'react'
import { api } from '../api.js'
import { etiquetaFranja, ms, requisitosDe } from '../util.js'
import { IconoAlerta, IconoCerrar, IconoCheck } from './Iconos.jsx'

/**
 * Detalle de una clase (Figura 7 del dossier) con los cambios propuestos en la Tabla 3:
 * franjas válidas primero, la semana visible en la grilla, validación inmediata al elegir,
 * un solo botón «Aplicar cambio», nombres de docentes y deshacer.
 * El editor del backend usa las mismas reglas que el motor de generación.
 */
export default function PanelClase({ cat, idx, horario, sesion, conflicto, avisos = [], eleccion, onElegir, onAlternativas, onChoques, onVerChoques, onCerrar, onGuardado }) {
  const asig = new Map(horario.asignaciones.map((a) => [a.sesion, a]))
  const actual = asig.get(sesion)
  const s = idx.sesion.get(sesion)
  const [alt, setAlt] = useState(null)
  const [validacion, setValidacion] = useState(null)
  const [validando, setValidando] = useState(false)
  const [aplicando, setAplicando] = useState(false)
  const resultado = useRef(null)

  useEffect(() => {
    setAlt(null)
    api.alternativas(sesion).then((a) => {
      setAlt(a)
      onAlternativas(a)
    })
  }, [sesion])

  // Validación inmediata: al elegir una franja el sistema la revisa sin pedir otro clic.
  useEffect(() => {
    if (!eleccion) {
      setValidacion(null)
      return
    }
    let vigente = true
    setValidando(true)
    api
      .validar({ sesion, franja: eleccion.franja, aula: eleccion.aula })
      .then((v) => vigente && setValidacion(v))
      .finally(() => vigente && setValidando(false))
    return () => {
      vigente = false
    }
  }, [sesion, eleccion?.franja, eleccion?.aula])

  useEffect(() => {
    if (validacion) resultado.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    onChoques?.(validacion && !validacion.valido ? validacion.choques ?? [] : [])
  }, [validacion])

  const aplicar = async () => {
    setAplicando(true)
    try {
      const r = await api.aplicar({ sesion, franja: eleccion.franja, aula: eleccion.aula })
      if (r.validacion.valido) onGuardado(r.horario, sesion, r.validacion.destino)
      else setValidacion(r.validacion)
    } finally {
      setAplicando(false)
    }
  }

  const docente = (codigo) => cat.docentes.find((d) => d.codigo === codigo)
  const tarjeta = (id, rol, clase) => {
    const c = idx.sesion.get(id)
    const a = asig.get(id)
    const d = docente(c.docente)
    return (
      <div className={`clase-detalle ${clase}`} key={id}>
        <div className="linea">
          {etiquetaFranja(idx.franja.get(a.franja))} · {rol}
        </div>
        <div className="nombre">{c.asignatura}</div>
        <div className="pie">
          Grupo {c.grupo} · {d ? d.nombre : c.docente} · Aula {idx.aula.get(a.aula).codigo}
        </div>
      </div>
    )
  }

  const req = requisitosDe(idx, sesion)
  const paso = !eleccion ? 1 : validacion?.valido ? 3 : 2
  const PASOS = ['Elija una franja', 'El sistema la revisa', 'Aplique el cambio']

  return (
    <div className="tarjeta panel">
      <div className="cabecera">
        <h2 className="titulo-seccion">{conflicto ? 'Resolver conflicto' : avisos.length ? 'Mejorar clase' : 'Mover clase'}</h2>
        <button className="boton secundario icono" onClick={onCerrar} title="Cerrar (volver al horario)">
          <IconoCerrar />
        </button>
      </div>

      <ol className="mini-pasos">
        {PASOS.map((t, i) => (
          <li key={t} className={i + 1 < paso ? 'hecho' : i + 1 === paso ? 'actual' : ''}>
            <span>{i + 1 < paso ? <IconoCheck width={11} height={11} /> : i + 1}</span>
            {t}
          </li>
        ))}
      </ol>

      {conflicto ? (
        <>
          <p className="causa">
            <b>Causa:</b> {conflicto.mensaje}
          </p>
          {tarjeta(conflicto.sesiones[0], 'se va a mover', 'mover')}
          {conflicto.sesiones[1] != null && tarjeta(conflicto.sesiones[1], 'se mantiene', '')}
        </>
      ) : (
        <>
          {avisos.length > 0 && (
            <div className="causa amarilla">
              <b>Aviso de preferencia:</b> {avisos.map((a) => (a.endsWith('.') ? a : a + '.')).join(' ')} El horario es válido;
              moverla solo lo mejora.
            </div>
          )}
          {tarjeta(sesion, 'ubicación actual', '')}
        </>
      )}
      {req.length > 0 && <p className="nota-pequena" style={{ margin: '8px 0 0' }}>Necesita un aula con {req.join(' y ')}.</p>}

      <div className="etiqueta-mayus" style={{ marginTop: 16 }}>
        Franjas sugeridas · cumplen todas las reglas
      </div>
      {!alt ? (
        <div className="chips">
          {[0, 1, 2, 3].map((i) => (
            <span key={i} className="chip esqueleto" />
          ))}
        </div>
      ) : alt.validas.length === 0 ? (
        <p className="nota-pequena">No hay ninguna franja libre que cumpla todas las reglas para esta clase.</p>
      ) : (
        <>
          <div className="chips">
            {alt.sugeridas.map((a, i) => (
              <button
                key={a.franja}
                style={{ animationDelay: `${i * 50}ms` }}
                className={`chip ${eleccion?.franja === a.franja ? 'elegido' : ''}`}
                onClick={() => onElegir({ franja: a.franja, aula: a.aula })}
                title={a.preferida ? 'Franja preferida del docente' : 'Cumple las reglas, pero no es preferida del docente'}
              >
                {etiquetaFranja(idx.franja.get(a.franja))} · {idx.aula.get(a.aula).codigo}
                {a.preferida && <span className="tag-pref">preferida</span>}
              </button>
            ))}
          </div>
          <div className="nota-pequena">
            Revisó las {alt.revisadas} franjas en {ms(alt.ms)}: {alt.validas.length} sirven. «Preferida» indica que el docente
            prefiere esa franja. También puede pulsar una franja verde en la grilla.
          </div>
        </>
      )}

      <div className="etiqueta-mayus" style={{ margin: '16px 0 6px' }}>
        O elija cualquier franja
      </div>
      <select
        className="selector ancho"
        value={eleccion?.franja ?? ''}
        onChange={(e) => {
          const f = Number(e.target.value)
          const v = alt?.validas.find((x) => x.franja === f)
          onElegir({ franja: f, aula: v ? v.aula : actual.aula })
        }}
      >
        <option value="" disabled>
          Seleccione una franja…
        </option>
        {cat.franjas
          .filter((f) => f.id !== actual.franja)
          .map((f) => (
            <option key={f.id} value={f.id}>
              {etiquetaFranja(f)}
              {alt && alt.validas.some((v) => v.franja === f.id) ? ' (disponible)' : ''}
            </option>
          ))}
      </select>

      <div ref={resultado}>
        {eleccion && validando && !validacion && (
          <div className="resultado-validacion revisando">
            <span className="spinner pequeno" /> Revisando las reglas…
          </div>
        )}
        {validacion && !validacion.valido && (
          <div className="resultado-validacion rechazo" key={`r${eleccion?.franja}`}>
            <div className="r-titulo">
              <IconoAlerta width={15} height={15} /> No se puede: {validacion.destino}
            </div>
            <ul>
              {validacion.motivos.map((m, i) => (
                <li key={i}>{m}</li>
              ))}
            </ul>
            {validacion.choques?.length > 0 && (
              <div className="choques">
                <div className="c-titulo">Chocaría con {validacion.choques.length === 1 ? 'esta clase' : 'estas clases'}:</div>
                {validacion.choques.map((id) => {
                  const c = idx.sesion.get(id)
                  const a = asig.get(id)
                  return (
                    <div className="choque" key={id}>
                      <b>{c.asignatura}</b>
                      <span>
                        Grupo {c.grupo} · {c.docente} · Aula {idx.aula.get(a.aula).codigo} · {etiquetaFranja(idx.franja.get(a.franja))}
                      </span>
                    </div>
                  )
                })}
                <button className="boton pequeno secundario" onClick={() => onVerChoques(validacion.choques)}>
                  Ver dónde choca en el horario
                </button>
              </div>
            )}
            <div className="pie">Revisado en {ms(validacion.ms)}. Elija otra franja o una de las sugeridas.</div>
          </div>
        )}
        {validacion && validacion.valido && (
          <div className="resultado-validacion acepta" key={`a${eleccion?.franja}`}>
            <div className="r-titulo">
              <IconoCheck width={15} height={15} /> Se puede: {validacion.destino}, aula {idx.aula.get(eleccion.aula).codigo}
            </div>
            <div>Cumple todas las reglas obligatorias.</div>
            {validacion.avisos?.length > 0 && (
              <div className="avisos-preferencia">
                <b>Ojo con las preferencias</b> (puede aplicarlo igual):
                <ul>
                  {validacion.avisos.map((a, i) => (
                    <li key={i}>{a}</li>
                  ))}
                </ul>
              </div>
            )}
            <div className="pie">Revisado en {ms(validacion.ms)}.</div>
          </div>
        )}
      </div>

      <div className="botones acciones-panel">
        <button className="boton verde" onClick={aplicar} disabled={!validacion?.valido || aplicando}>
          <IconoCheck width={15} height={15} /> {aplicando ? 'Aplicando…' : 'Aplicar cambio'}
        </button>
        <button className="boton secundario" onClick={onCerrar}>
          Cancelar
        </button>
      </div>
    </div>
  )
}
