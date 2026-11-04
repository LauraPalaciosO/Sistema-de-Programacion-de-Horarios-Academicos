import { createContext, useCallback, useContext, useState } from 'react'
import { createPortal } from 'react-dom'
import { IconoAlerta, IconoCheck } from './Iconos.jsx'

export function Cargando({ texto = 'Cargando…' }) {
  return (
    <div className="cargando">
      <div className="spinner" />
      {texto}
    </div>
  )
}

export function ErrorRed({ error }) {
  return (
    <div className="error-red">
      <b>No se pudo hablar con el backend.</b> {error?.message}
      <div className="nota-pequena" style={{ marginTop: 6 }}>
        Verifique que Spring Boot esté corriendo en http://localhost:8080.
      </div>
    </div>
  )
}

export function Titulo({ children, derecha }) {
  return (
    <div className="titulo-fila">
      <h2 className="titulo-seccion">{children}</h2>
      {derecha}
    </div>
  )
}

export function Metrica({ valor, nombre, tono }) {
  return (
    <div className="metrica">
      <div className={`valor ${tono || ''}`}>{valor}</div>
      <div className="nombre">{nombre}</div>
    </div>
  )
}

/** Diálogo de confirmación. */
export function Dialogo({ titulo, children, onCancelar, acciones }) {
  return createPortal(
    <div className="fondo-dialogo" onClick={onCancelar}>
      <div className="dialogo" onClick={(e) => e.stopPropagation()}>
        <h3>{titulo}</h3>
        {children}
        <div className="botones">{acciones}</div>
      </div>
    </div>,
    document.body
  )
}

// ---------------- avisos flotantes (toasts)

const ContextoAvisos = createContext(() => {})

export function ProveedorAvisos({ children }) {
  const [avisos, setAvisos] = useState([])
  const avisar = useCallback((texto, tipo = 'ok') => {
    const id = Math.random()
    setAvisos((a) => [...a, { id, texto, tipo }])
    setTimeout(() => setAvisos((a) => a.filter((x) => x.id !== id)), 3800)
  }, [])
  return (
    <ContextoAvisos.Provider value={avisar}>
      {children}
      <div className="toasts">
        {avisos.map((a) => (
          <div key={a.id} className={`toast ${a.tipo}`}>
            <span className="ic">{a.tipo === 'error' ? <IconoAlerta width={12} height={12} /> : <IconoCheck width={12} height={12} />}</span>
            {a.texto}
          </div>
        ))}
      </div>
    </ContextoAvisos.Provider>
  )
}

export const useAvisos = () => useContext(ContextoAvisos)
