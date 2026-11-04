// Utilidades de formato (números con coma decimal) e índices del catálogo.

export const num = (v, dec = 2) =>
  Number(v).toLocaleString('es-CO', { minimumFractionDigits: dec, maximumFractionDigits: dec })

export const seg = (v) => `${num(v, v < 0.1 ? 3 : 2)} s`

export const ms = (v) => `${num(v, v < 1 ? 3 : 1)} ms`

export const pct = (v, dec = 1) => `${num(v, dec)} %`

/** "Lunes de 07:00 a 09:00" */
export const etiquetaFranja = (f) => (f ? `${f.nombreDia} de ${f.inicio} a ${f.fin}` : '')

/** Índices del catálogo para no recorrer listas a cada rato. */
export function indexar(cat) {
  const sesion = new Map(cat.sesiones.map((s) => [s.id, s]))
  const seccion = new Map(cat.secciones.map((s) => [s.id, s]))
  const franja = new Map(cat.franjas.map((f) => [f.id, f]))
  const aula = new Map(cat.aulas.map((a) => [a.id, a]))
  const asignatura = new Map(cat.asignaturas.map((a) => [a.id, a]))
  const horas = []
  for (const f of cat.franjas) {
    if (!horas.some((h) => h.inicio === f.inicio)) horas.push({ inicio: f.inicio, fin: f.fin })
  }
  horas.sort((a, b) => a.inicio.localeCompare(b.inicio))
  const franjaEn = new Map(cat.franjas.map((f) => [`${f.dia}|${f.inicio}`, f]))
  return { sesion, seccion, franja, aula, asignatura, horas, franjaEn }
}

/** Requisitos de aula (computadores, sillas móviles) de la asignatura de una clase. */
export function requisitosDe(idx, sesionId) {
  const s = idx.sesion.get(sesionId)
  const sec = idx.seccion.get(s.seccion)
  return idx.asignatura.get(sec.asignatura).requisitos
}

/** Texto corto para el aviso lateral de un conflicto. */
export function avisoCorto(c, idx) {
  const f = idx.franja.get(c.franja)
  const cuando = f ? `el ${f.nombreDia.toLowerCase()} de ${f.inicio} a ${f.fin}` : ''
  switch (c.regla) {
    case 'DOCENTE-LIBRE':
      return { quien: `El ${c.recurso}`, que: `tiene dos clases ${cuando}.` }
    case 'AULA-LIBRE':
      return { quien: `Un aula`, que: `tiene dos clases ${cuando}.` }
    case 'GRUPO-LIBRE':
      return { quien: `El ${c.recurso}`, que: `tiene dos asignaturas ${cuando}.` }
    case 'UNA-CLASE-POR-DIA':
      return { quien: idx.sesion.get(c.sesiones[0]).asignatura, que: `(${idx.sesion.get(c.sesiones[0]).grupo}) tiene dos clases el mismo día.` }
    case 'DIAS-NO-CONSECUTIVOS':
      return { quien: idx.sesion.get(c.sesiones[0]).asignatura, que: `(${idx.sesion.get(c.sesiones[0]).grupo}) tiene clases en días seguidos.` }
    default:
      return { quien: '', que: c.mensaje }
  }
}
