import { useEffect, useMemo, useState } from 'react'
import { api } from '../api.js'
import { num, seg } from '../util.js'
import GrillaHorario from '../componentes/GrillaHorario.jsx'
import PanelClase from '../componentes/PanelClase.jsx'
import PanelResumen from '../componentes/PanelResumen.jsx'
import Guia from '../componentes/Guia.jsx'
import Pildoras from '../componentes/Pildoras.jsx'
import { Dialogo, useAvisos } from '../componentes/Comunes.jsx'
import { IconoAlerta, IconoDeshacer, IconoGenerar } from '../componentes/Iconos.jsx'

const MODOS = [
  { id: 'todo', nombre: 'Todo el horario' },
  { id: 'grupo', nombre: 'Un grupo' },
  { id: 'docente', nombre: 'Un docente' },
  { id: 'aula', nombre: 'Un aula' }
]

const PASOS_GENERACION = ['Revisando los datos del periodo…', 'Aplicando las reglas obligatorias…', 'Buscando el horario…']

/** Pantalla principal del coordinador: ver, generar, corregir y exportar el horario del periodo. */
export default function Horario({ cat, idx, horario, setHorario, irADatos }) {
  const avisar = useAvisos()
  const [vista, setVista] = useState({ modo: 'todo', valor: null })
  const [seleccion, setSeleccion] = useState(null) // { sesion, conflicto }
  const [eleccion, setEleccion] = useState(null) // { franja, aula }
  const [alternativas, setAlternativas] = useState(null)
  const [choques, setChoques] = useState([])
  const [resaltadas, setResaltadas] = useState([])
  const [indicador, setIndicador] = useState(null)
  const [movida, setMovida] = useState(null)
  const [generando, setGenerando] = useState(false)
  const [pasoGen, setPasoGen] = useState(0)
  const [confirmar, setConfirmar] = useState(false)
  const [fallo, setFallo] = useState(null)
  const [version, setVersion] = useState(0)

  const enConflicto = useMemo(() => new Set(horario.conflictos.flatMap((c) => c.sesiones)), [horario])
  const validas = useMemo(() => new Map((alternativas?.validas ?? []).map((a) => [a.franja, a])), [alternativas])
  const resaltadasSet = useMemo(() => new Set([...resaltadas, ...choques]), [resaltadas, choques])
  // Avisos de preferencias (RB-01, RB-02): clases válidas que se pueden mejorar. Se marcan en amarillo.
  const avisosPorSesion = useMemo(() => {
    const m = new Map()
    for (const h of horario.metricas?.hallazgos ?? []) {
      if (h.criterio !== 'RB-01' && h.criterio !== 'RB-02') continue
      for (const id of h.criterio === 'RB-01' ? h.sesiones.slice(0, 1) : h.sesiones) {
        if (!m.has(id)) m.set(id, [])
        m.get(id).push(h.texto)
      }
    }
    return m
  }, [horario])
  const conAviso = useMemo(() => new Set(avisosPorSesion.keys()), [avisosPorSesion])

  useEffect(() => {
    if (!generando) return
    const t = setInterval(() => setPasoGen((p) => Math.min(p + 1, PASOS_GENERACION.length - 1)), 450)
    return () => clearInterval(t)
  }, [generando])

  const opcionesDe = (modo) =>
    modo === 'grupo'
      ? cat.grupos.map((g) => ({ id: g, nombre: g }))
      : modo === 'docente'
        ? cat.docentes.map((d) => ({ id: d.codigo, nombre: d.codigo, titulo: d.nombre }))
        : modo === 'aula'
          ? cat.aulas.map((a) => ({ id: a.codigo, nombre: a.codigo }))
          : []

  // Dónde hay conflictos, para marcar las píldoras con un punto rojo.
  const conConflicto = useMemo(() => {
    const r = new Set()
    for (const c of horario.conflictos) {
      for (const id of c.sesiones) {
        const s = idx.sesion.get(id)
        const a = horario.asignaciones.find((x) => x.sesion === id)
        if (vista.modo === 'grupo') r.add(s.grupo)
        else if (vista.modo === 'docente') r.add(s.docente)
        else if (vista.modo === 'aula' && a) r.add(idx.aula.get(a.aula).codigo)
      }
    }
    return r
  }, [horario, vista.modo, idx])

  const cambiarModo = (modo) => {
    if (modo === 'todo') return setVista({ modo, valor: null })
    let valor = opcionesDe(modo)[0]?.id
    const ref = seleccion?.sesion ?? horario.conflictos[0]?.sesiones[0]
    if (ref != null) {
      const s = idx.sesion.get(ref)
      const a = horario.asignaciones.find((x) => x.sesion === ref)
      valor = modo === 'grupo' ? s.grupo : modo === 'docente' ? s.docente : idx.aula.get(a.aula).codigo
    }
    setVista({ modo, valor })
  }

  const abrir = (sesion) => {
    const conflicto = horario.conflictos.find((c) => c.sesiones.includes(sesion)) ?? null
    const mover = conflicto ? conflicto.sesiones[0] : sesion
    setSeleccion({ sesion: mover, conflicto })
    setEleccion(null)
    setAlternativas(null)
    setChoques([])
    setResaltadas(conflicto ? conflicto.sesiones.filter((x) => x !== mover) : [])
    setMovida(null)
    if (vista.modo === 'grupo') setVista({ modo: 'grupo', valor: idx.sesion.get(mover).grupo })
    if (vista.modo === 'docente') setVista({ modo: 'docente', valor: idx.sesion.get(mover).docente })
  }

  const cerrar = () => {
    setSeleccion(null)
    setEleccion(null)
    setAlternativas(null)
    setChoques([])
    setResaltadas([])
  }

  const generar = async () => {
    setConfirmar(false)
    cerrar()
    setIndicador(null)
    setGenerando(true)
    setPasoGen(0)
    const inicio = Date.now()
    try {
      const r = await api.generar()
      await new Promise((ok) => setTimeout(ok, Math.max(0, 1100 - (Date.now() - inicio))))
      if (r.horario) {
        setHorario(r.horario)
        setVersion((v) => v + 1)
        setMovida(null)
        const nAvisos = (r.horario.metricas?.hallazgos ?? []).filter((h) => h.criterio === 'RB-01' || h.criterio === 'RB-02').length
        avisar(
          `Horario generado en ${seg(r.segundos)}: ${r.horario.asignaciones.length} clases, sin conflictos` +
            (nAvisos ? `, ${nAvisos} aviso${nAvisos > 1 ? 's' : ''} de preferencias para mejorar` : '')
        )
      } else {
        setFallo(r)
      }
    } catch (e) {
      avisar(e.message, 'error')
    } finally {
      setGenerando(false)
    }
  }

  const deshacer = async () => {
    cerrar()
    const h = await api.deshacer()
    setHorario(h)
    setMovida(null)
    avisar('Se deshizo el último cambio')
  }

  const guardado = (h, sesion, destino) => {
    setHorario(h)
    cerrar()
    setMovida(sesion)
    avisar(`Cambio guardado: ${idx.sesion.get(sesion).asignatura} pasó al ${destino}`)
  }

  const nombreVista = () => {
    if (vista.modo === 'todo') return `Horario del periodo ${cat.periodo}`
    if (vista.modo === 'grupo') return `Horario del periodo · grupo ${vista.valor}`
    if (vista.modo === 'docente') {
      const d = cat.docentes.find((x) => x.codigo === vista.valor)
      return `Horario del periodo · ${d ? d.nombre : vista.valor}`
    }
    return `Horario del periodo · aula ${vista.valor}`
  }

  const descripcion = horario.vacio
    ? 'Todavía no hay un horario guardado para este periodo.'
    : seleccion
      ? 'Las franjas verdes cumplen todas las reglas para la clase que se mueve. Pulse una para elegirla.'
      : vista.modo === 'todo'
        ? `Las ${horario.asignaciones.length} clases de los ${cat.grupos.length} grupos. Es un único horario: los filtros de arriba solo cambian qué clases se ven.`
        : 'Mismo horario, filtrado. Pase el mouse sobre una clase para ver sus datos y púlsela para moverla.'

  return (
    <>
      <Guia
        cat={cat}
        idx={idx}
        horario={horario}
        onGenerar={() => (horario.vacio ? generar() : setConfirmar(true))}
        onRevisar={abrir}
        onDatos={irADatos}
        onAvisos={() => {
          cerrar()
          setIndicador('AVISOS')
        }}
      />

      <div className="barra no-imprimir">
        <div className="izquierda">
          <span className="etiqueta-mayus">Mostrar</span>
          <div className="segmentado">
            {MODOS.map((mo) => (
              <button key={mo.id} className={vista.modo === mo.id ? 'activo' : ''} onClick={() => cambiarModo(mo.id)}>
                {mo.nombre}
              </button>
            ))}
          </div>
        </div>
        <div className="botones">
          <button className="boton secundario" onClick={deshacer} disabled={!horario.puedeDeshacer || generando}>
            <IconoDeshacer /> Deshacer
          </button>
          <a className={`boton secundario ${horario.vacio ? 'deshabilitado' : ''}`} href="/api/horario/exportar.csv">
            Exportar CSV
          </a>
          <button className="boton secundario" onClick={() => window.print()} disabled={horario.vacio}>
            Imprimir / PDF
          </button>
          <button className="boton naranja" onClick={() => (horario.vacio ? generar() : setConfirmar(true))} disabled={generando}>
            <IconoGenerar /> Generar horario
          </button>
        </div>
      </div>

      <div className="dos-columnas">
        <section className="tarjeta area-horario">
          <h2 className="titulo-seccion">{nombreVista()}</h2>
          {vista.modo !== 'todo' && !horario.vacio && (
            <Pildoras
              opciones={opcionesDe(vista.modo)}
              valor={vista.valor}
              onChange={(v) => setVista({ ...vista, valor: v })}
              conConflicto={conConflicto}
              titulo={(o) => o.titulo ?? o.nombre}
            />
          )}
          <p className="descripcion">{descripcion}</p>
          <div className="grilla-marco">
            {horario.vacio ? (
              <div className="vacio">
                <span className="icono-grande">
                  <IconoGenerar width={22} height={22} />
                </span>
                <h3>Aún no hay horario para el periodo {cat.periodo}</h3>
                <p>
                  El sistema revisa los datos, aplica las reglas obligatorias y busca un horario completo. Si no existe, le
                  dirá por qué.
                </p>
                <button className="boton" onClick={generar} disabled={generando}>
                  <IconoGenerar /> Generar horario
                </button>
              </div>
            ) : (
              <GrillaHorario
                key={version}
                cat={cat}
                idx={idx}
                asignaciones={horario.asignaciones}
                vista={vista}
                enConflicto={enConflicto}
                conAviso={conAviso}
                seleccionada={seleccion?.sesion}
                movida={movida}
                validas={seleccion ? validas : undefined}
                destino={eleccion?.franja}
                resaltadas={resaltadasSet}
                onClase={abrir}
                onCelda={(f) => setEleccion({ franja: f, aula: validas.get(f).aula })}
              />
            )}
            {generando && (
              <div className="generando">
                <div className="spinner" />
                <div className="texto">Generando el horario</div>
                <div className="paso">{PASOS_GENERACION[pasoGen]}</div>
                <div className="barra-progreso">
                  <span />
                </div>
              </div>
            )}
          </div>
        </section>

        <aside className="columna-lateral no-imprimir">
          {seleccion ? (
            <PanelClase
              key={seleccion.sesion}
              cat={cat}
              idx={idx}
              horario={horario}
              sesion={seleccion.sesion}
              conflicto={seleccion.conflicto}
              avisos={avisosPorSesion.get(seleccion.sesion) ?? []}
              eleccion={eleccion}
              onElegir={setEleccion}
              onAlternativas={setAlternativas}
              onChoques={setChoques}
              onVerChoques={() => setVista({ modo: 'todo', valor: null })}
              onCerrar={cerrar}
              onGuardado={guardado}
            />
          ) : horario.vacio ? (
            <div className="aviso ambar">
              <div className="titulo">Sin horario</div>
              <p style={{ margin: 0 }}>
                Pulse <b>Generar horario</b>. Si los datos no permiten un horario, el sistema le mostrará la causa.
              </p>
            </div>
          ) : (
            <PanelResumen
              cat={cat}
              idx={idx}
              horario={horario}
              abierto={indicador}
              onAbrir={setIndicador}
              onResaltar={setResaltadas}
              onMover={abrir}
              onVer={setVista}
            />
          )}
        </aside>
      </div>

      {confirmar && (
        <Dialogo
          titulo="¿Generar un horario nuevo?"
          onCancelar={() => setConfirmar(false)}
          acciones={
            <>
              <button className="boton secundario" onClick={() => setConfirmar(false)}>
                Cancelar
              </button>
              <button className="boton" onClick={generar}>
                <IconoGenerar /> Generar
              </button>
            </>
          }
        >
          <p>El horario actual, con sus cambios manuales, se reemplazará por uno nuevo calculado desde cero.</p>
        </Dialogo>
      )}

      {fallo && (
        <Dialogo
          titulo={
            <>
              <IconoAlerta style={{ color: 'var(--rojo)' }} /> No se puede generar el horario
            </>
          }
          onCancelar={() => setFallo(null)}
          acciones={
            <>
              <button className="boton secundario" onClick={() => setFallo(null)}>
                Cerrar
              </button>
              <button
                className="boton"
                onClick={() => {
                  setFallo(null)
                  irADatos()
                }}
              >
                Revisar datos
              </button>
            </>
          }
        >
          <p>
            {fallo.estado === 'DATOS_INVALIDOS'
              ? `La revisión previa detectó en ${num(fallo.msRevision, 1)} ms que con estos datos no existe un horario que cumpla todas las reglas:`
              : fallo.estado === 'TIEMPO_AGOTADO'
                ? `La búsqueda no encontró un horario en ${seg(fallo.segundos)}. Las reglas que más la bloquearon:`
                : 'No existe un horario que cumpla todas las reglas. Lo que más bloqueó la búsqueda:'}
          </p>
          {fallo.causas.slice(0, 6).map((c, i) => (
            <div className="codigo" key={i}>
              {c}
            </div>
          ))}
          {fallo.causas.length > 6 && <p className="nota-pequena">Y {fallo.causas.length - 6} más.</p>}
          <p className="nota-pequena" style={{ marginTop: 12 }}>
            El horario guardado no se modificó.
          </p>
        </Dialogo>
      )}
    </>
  )
}
