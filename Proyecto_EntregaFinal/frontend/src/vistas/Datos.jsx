import { useState } from 'react'
import { api } from '../api.js'
import { Dialogo, Titulo, useAvisos } from '../componentes/Comunes.jsx'
import { IconoAlerta, IconoCheck } from '../componentes/Iconos.jsx'

/**
 * Versión de vista previa sin la tarjeta de parámetros institucionales: se compila con
 * VITE_SIN_PARAMETROS=1 (ver frontend/.env.sin-parametros). La versión normal la muestra (RF-02, RD-12, MVP-18).
 */
const SIN_PARAMETROS = import.meta.env.VITE_SIN_PARAMETROS === '1'

const TABLAS = [
  { id: 'secciones', nombre: 'Secciones' },
  { id: 'docentes', nombre: 'Docentes' },
  { id: 'aulas', nombre: 'Aulas' },
  { id: 'asignaturas', nombre: 'Asignaturas' }
]

/** Datos del periodo: parámetros institucionales, consulta de lo guardado y escenarios de prueba. */
export default function Datos({ cat, idx, onCatalogo }) {
  const avisar = useAvisos()
  const [p, setP] = useState(cat.parametros)
  const [tabla, setTabla] = useState('secciones')
  const [escenario, setEscenario] = useState(null)
  const [ocupado, setOcupado] = useState(false)

  const guardarParametros = async () => {
    try {
      const c = await api.guardarParametros({
        ...p,
        tamanoObjetivo: Number(p.tamanoObjetivo),
        tolerancia: Number(p.tolerancia),
        minimoEstudiantes: Number(p.minimoEstudiantes),
        sesionesMin: Number(p.sesionesMin),
        sesionesMax: Number(p.sesionesMax)
      })
      onCatalogo(c)
      setP(c.parametros)
      const bloquean = c.advertencias.filter((a) => a.bloqueante).length
      avisar(bloquean ? `Parámetros guardados. Hay ${bloquean} problema(s) que impiden generar.` : 'Parámetros guardados')
    } catch (e) {
      avisar(e.message, 'error')
    }
  }

  const cargar = async () => {
    const id = escenario.id
    setEscenario(null)
    setOcupado(true)
    try {
      const c = await api.cargarDatos(id)
      onCatalogo(c, true)
      setP(c.parametros)
      avisar(`Escenario cargado: ${cat.conjuntos.find((x) => x.id === id)?.nombre}`)
    } catch (e) {
      avisar(e.message, 'error')
    } finally {
      setOcupado(false)
    }
  }

  const bloqueantes = cat.advertencias.filter((a) => a.bloqueante)
  const revisar = cat.advertencias.filter((a) => !a.bloqueante)
  const campo = (k, etiqueta, ayuda, paso = 1) => (
    <label>
      {etiqueta}
      <input className="campo" type="number" step={paso} value={p[k]} onChange={(e) => setP({ ...p, [k]: e.target.value })} />
      {ayuda && <span className="ayuda">{ayuda}</span>}
    </label>
  )
  const habilitadas = (d) =>
    cat.asignaturas
      .filter((a) => d.habilitadas?.includes(a.id))
      .map((a) => a.nombre)
      .join(', ')

  return (
    <div className="rejilla-datos">
      {!SIN_PARAMETROS && (
        <section className="tarjeta">
          <Titulo>Parámetros institucionales</Titulo>
          <p className="descripcion">Valores que usa el sistema al revisar los datos y al generar el horario (RF-02, RD-12).</p>
          <div className="formulario">
            {campo('tamanoObjetivo', 'Tamaño objetivo de sección', 'Estudiantes por sección')}
            {campo('tolerancia', 'Tolerancia', 'Ej.: 0,10 = 10 %', 0.01)}
            {campo('minimoEstudiantes', 'Mínimo de estudiantes', 'Menos se marca para revisión')}
            <div className="dos-campos">
              {campo('sesionesMin', 'Clases mín.', 'por semana')}
              {campo('sesionesMax', 'Clases máx.', 'por semana')}
            </div>
          </div>
          <label className="casilla">
            <input
              type="checkbox"
              checked={!!p.diasNoConsecutivos}
              onChange={(e) => setP({ ...p, diasNoConsecutivos: e.target.checked })}
            />
            <span>
              <b>Exigir días no consecutivos.</b> Las asignaturas de 2 o 3 clases no podrán tener clases en días seguidos
              (hoy es una preferencia).
            </span>
          </label>
          <div className="botones" style={{ marginTop: 14 }}>
            <button className="boton" onClick={guardarParametros}>
              Guardar parámetros
            </button>
            <span className="nota-pequena">
              Tope de sección: <b>{cat.parametros.tamanoMaximo}</b> estudiantes (tamaño objetivo + tolerancia)
            </span>
          </div>
        </section>
      )}

      <section className={`tarjeta ${SIN_PARAMETROS ? 'ancho-completo' : ''}`}>
        <Titulo>Estado de los datos</Titulo>
        <p className="descripcion">Lo que la revisión previa encuentra con los datos y parámetros actuales.</p>
        {bloqueantes.length === 0 && revisar.length === 0 && (
          <div className="vacio-lista">
            <IconoCheck width={18} height={18} /> Los datos están completos y son coherentes. Se puede generar el horario.
          </div>
        )}
        {bloqueantes.length > 0 && (
          <div className="aviso rojo" style={{ marginBottom: 10 }}>
            <div className="titulo">
              <IconoAlerta width={14} height={14} /> Impiden generar el horario ({bloqueantes.length})
            </div>
            <ul className="lista-avisos">
              {bloqueantes.slice(0, 10).map((a, i) => (
                <li key={i}>{a.mensaje}.</li>
              ))}
            </ul>
            {bloqueantes.length > 10 && <p className="nota-pequena">Y {bloqueantes.length - 10} más.</p>}
          </div>
        )}
        {revisar.length > 0 && (
          <div className="aviso ambar">
            <div className="titulo">Para revisión administrativa ({revisar.length})</div>
            <ul className="lista-avisos">
              {revisar.map((a, i) => (
                <li key={i}>{a.mensaje}.</li>
              ))}
            </ul>
          </div>
        )}
      </section>

      <section className="tarjeta ancho-completo">
        <Titulo>Datos del periodo {cat.periodo}</Titulo>
        <p className="descripcion">
          Consulta de lo que está guardado en la base de datos: {cat.resumen.secciones} secciones de {cat.asignaturas.length}{' '}
          asignaturas, {cat.resumen.docentes} docentes y {cat.resumen.aulas} aulas. Datos de prueba, no institucionales.
        </p>
        <div className="segmentado" style={{ marginBottom: 12 }}>
          {TABLAS.map((t) => (
            <button key={t.id} className={tabla === t.id ? 'activo' : ''} onClick={() => setTabla(t.id)}>
              {t.nombre}
            </button>
          ))}
        </div>
        <div className="tabla-contenedor" key={tabla}>
          {tabla === 'secciones' && (
            <table className="tabla entrar">
              <thead>
                <tr><th>Código</th><th>Grupo</th><th>Asignatura</th><th>Docente</th><th>Estudiantes</th><th>Clases por semana</th></tr>
              </thead>
              <tbody>
                {cat.secciones.map((s) => {
                  const a = idx.asignatura.get(s.asignatura)
                  const d = cat.docentes[s.docente]
                  const tope = s.estudiantes > cat.parametros.tamanoMaximo
                  return (
                    <tr key={s.id} className={tope ? 'fila-alerta' : ''}>
                      <td className="mono">{s.codigo}</td><td>{s.grupo}</td><td>{a.nombre}</td>
                      <td>{d.codigo} · {d.nombre}</td>
                      <td className="num">{s.estudiantes}{tope && <span className="tag-error">supera el tope</span>}</td>
                      <td className="num">{a.sesionesSemanales}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          )}
          {tabla === 'docentes' && (
            <table className="tabla entrar">
              <thead>
                <tr><th>Código</th><th>Nombre</th><th>Habilitado para</th><th>Clases</th><th>Franjas disponibles</th><th>Jornada</th></tr>
              </thead>
              <tbody>
                {cat.docentes.map((d) => (
                  <tr key={d.id}>
                    <td className="mono">{d.codigo}</td><td>{d.nombre}</td><td className="habilitado">{habilitadas(d)}</td>
                    <td className="num">{d.sesiones}</td><td className="num">{d.disponibles.length} de {cat.franjas.length}</td>
                    <td>{d.nocturno ? 'Solo desde las 5:00 p. m.' : 'Diurna y nocturna'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {tabla === 'aulas' && (
            <table className="tabla entrar">
              <thead>
                <tr><th>Código</th><th>Capacidad</th><th>Características</th></tr>
              </thead>
              <tbody>
                {cat.aulas.map((a) => (
                  <tr key={a.id}>
                    <td className="mono">{a.codigo}</td><td className="num">{a.capacidad}</td>
                    <td>{a.caracteristicas.length ? a.caracteristicas.join(', ') : 'Ninguna especial'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          {tabla === 'asignaturas' && (
            <table className="tabla entrar">
              <thead>
                <tr><th>Código</th><th>Nombre</th><th>Semestre</th><th>Créditos</th><th>Clases por semana</th><th>Requisitos de aula</th></tr>
              </thead>
              <tbody>
                {cat.asignaturas.map((a) => (
                  <tr key={a.id}>
                    <td className="mono">{a.codigo}</td><td>{a.nombre}</td><td className="num">{a.semestre}.º</td>
                    <td className="num">{a.creditos}</td><td className="num">{a.sesionesSemanales}</td>
                    <td>{a.requisitos.length ? a.requisitos.join(', ') : 'Ninguno'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </section>

      <section className="tarjeta ancho-completo">
        <Titulo>Escenarios de prueba</Titulo>
        <p className="descripcion">
          Para la exposición y las pruebas: cada escenario reemplaza los datos del periodo y el horario guardado por un caso
          preparado. Después vaya a <b>Horario</b> y pulse <b>Generar horario</b> para ver qué hace el sistema.
        </p>
        <div className="escenarios">
          {cat.conjuntos.map((c, i) => {
            const enUso = c.id === cat.conjunto
            return (
              <div className={`escenario ${enUso ? 'en-uso' : ''}`} key={c.id} style={{ animationDelay: `${i * 50}ms` }}>
                <div className="e-cabecera">
                  <h3>{c.nombre}</h3>
                  <span className={`etiqueta ${c.tieneSolucion ? 'verde' : 'rojo'}`}>{c.tieneSolucion ? 'Tiene solución' : 'Sin solución'}</span>
                </div>
                <p>{c.descripcion}</p>
                <p className="esperado">
                  <b>Al generar:</b> {c.esperado}
                </p>
                <button className={`boton pequeno ${enUso ? 'secundario' : ''}`} onClick={() => setEscenario(c)} disabled={ocupado}>
                  {enUso ? 'En uso · volver a cargar' : 'Usar este escenario'}
                </button>
              </div>
            )
          })}
        </div>
      </section>

      {escenario && (
        <Dialogo
          titulo={`¿Usar «${escenario.nombre}»?`}
          onCancelar={() => setEscenario(null)}
          acciones={
            <>
              <button className="boton secundario" onClick={() => setEscenario(null)}>Cancelar</button>
              <button className="boton" onClick={cargar}>Usar este escenario</button>
            </>
          }
        >
          <p>{escenario.descripcion}</p>
          <p>Se reemplazarán los datos del periodo y el horario guardado (incluidos los cambios manuales).</p>
        </Dialogo>
      )}
    </div>
  )
}
