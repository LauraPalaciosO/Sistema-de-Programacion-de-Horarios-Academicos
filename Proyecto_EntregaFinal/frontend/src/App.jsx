import { useEffect, useMemo, useState } from 'react'
import { api } from './api.js'
import { indexar } from './util.js'
import { Cargando, ErrorRed, ProveedorAvisos } from './componentes/Comunes.jsx'
import Horario from './vistas/Horario.jsx'
import Datos from './vistas/Datos.jsx'

export default function App() {
  const [pestana, setPestana] = useState(() => (location.hash === '#datos' ? 'datos' : 'horario'))
  const [cat, setCat] = useState(null)
  const [horario, setHorario] = useState(null)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    Promise.all([api.catalogo(), api.horario()])
      .then(([c, h]) => {
        setCat(c)
        setHorario(h)
      })
      .catch(setError)
  }, [])

  useEffect(() => {
    history.replaceState(null, '', `#${pestana}`)
  }, [pestana])

  const idx = useMemo(() => (cat ? indexar(cat) : null), [cat])

  /** Al cambiar datos o reglas, los conflictos del horario pueden cambiar: se vuelve a leer. */
  const actualizarCatalogo = async (c, datosNuevos = false) => {
    setCat(c)
    setHorario(await api.horario())
    if (datosNuevos) setRecarga((r) => r + 1)
  }

  const conflictos = horario?.conflictos.length ?? 0
  const avisos = (horario?.metricas?.hallazgos ?? []).filter((h) => h.criterio === 'RB-01' || h.criterio === 'RB-02').length

  return (
    <ProveedorAvisos>
      <div className="pagina">
        <header className="encabezado">
          <div className="marca">
            <img className="logo" src="/logo-unbosque-blanco.png" alt="Universidad El Bosque" />
            <div className="separador" />
            <div>
              <div className="sobre">Facultad de Ingeniería · Planificación académica</div>
              <h1>Horario académico · Ingeniería de Sistemas</h1>
            </div>
          </div>
          <div className="derecha">
            <div className="periodo">
              Periodo <b>{cat?.periodo ?? '…'}</b> · Datos de prueba, no institucionales
            </div>
            {horario && (
              <span className={`estado-chip ${horario.vacio ? 'neutro' : conflictos ? 'alerta' : avisos ? 'aviso' : ''}`}>
                <span className="punto" />
                {horario.vacio
                  ? 'Sin horario generado'
                  : conflictos
                    ? `${conflictos} conflicto${conflictos > 1 ? 's' : ''} por resolver`
                    : avisos
                      ? `Sin conflictos · ${avisos} aviso${avisos > 1 ? 's' : ''} de preferencias`
                      : 'Horario sin conflictos'}
              </span>
            )}
          </div>
        </header>

        <nav className="pestanas">
          <button className={`pestana ${pestana === 'horario' ? 'activa' : ''}`} onClick={() => setPestana('horario')}>
            Horario
          </button>
          <button className={`pestana ${pestana === 'datos' ? 'activa' : ''}`} onClick={() => setPestana('datos')}>
            Datos
          </button>
        </nav>

        {error && <ErrorRed error={error} />}
        {!error && (!cat || !horario) && <Cargando texto="Cargando el periodo…" />}
        {cat && horario && (
          <main key={pestana} className="entrar">
            {pestana === 'horario' ? (
              <Horario
                key={recarga}
                cat={cat}
                idx={idx}
                horario={horario}
                setHorario={setHorario}
                irADatos={() => setPestana('datos')}
              />
            ) : (
              <Datos cat={cat} idx={idx} onCatalogo={actualizarCatalogo} />
            )}
          </main>
        )}
      </div>
    </ProveedorAvisos>
  )
}
