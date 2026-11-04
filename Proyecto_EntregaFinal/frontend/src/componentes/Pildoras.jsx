/** Selector rápido en forma de píldoras; un punto rojo marca las opciones con conflictos. */
export default function Pildoras({ opciones, valor, onChange, conConflicto = new Set(), titulo }) {
  return (
    <div className="pildoras">
      {opciones.map((o) => (
        <button
          key={o.id}
          className={`pildora ${valor === o.id ? 'activa' : ''}`}
          onClick={() => onChange(o.id)}
          title={titulo ? titulo(o) : o.nombre}
        >
          {o.nombre}
          {conConflicto.has(o.id) && <span className="punto-rojo" />}
        </button>
      ))}
    </div>
  )
}
