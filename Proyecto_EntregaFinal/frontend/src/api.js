// Cliente de la API REST del backend (Spring Boot).

async function pedir(url, opciones = {}) {
  const r = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...opciones
  })
  const texto = await r.text()
  const datos = texto ? JSON.parse(texto) : null
  if (!r.ok) throw new Error(datos?.mensaje || `Error ${r.status}`)
  return datos
}

const post = (url, cuerpo) => pedir(url, { method: 'POST', body: cuerpo ? JSON.stringify(cuerpo) : undefined })

export const api = {
  // datos del periodo
  catalogo: () => pedir('/api/catalogo'),
  guardarParametros: (p) => pedir('/api/parametros', { method: 'PUT', body: JSON.stringify(p) }),
  cargarDatos: (conjunto) => post(`/api/datos/cargar?conjunto=${encodeURIComponent(conjunto)}`),

  // horario
  horario: () => pedir('/api/horario'),
  generar: () => post('/api/horario/generar'),
  alternativas: (sesion) => pedir(`/api/horario/alternativas/${sesion}`),
  validar: (mov) => post('/api/horario/validar', mov),
  aplicar: (mov) => post('/api/horario/aplicar', mov),
  deshacer: () => post('/api/horario/deshacer')
}
