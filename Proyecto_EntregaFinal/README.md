# Horario académico · Ingeniería de Sistemas

Prototipo del sistema de generación y edición de horarios (Entregable 1 y Fase 2 – Dossier de Pre-diseño,
Universidad El Bosque).

- **Backend:** Java 17+ · Spring Boot 3.5 · Spring Data JPA · base de datos embebida **H2** (en memoria).
- **Frontend:** React 19 + Vite, consumiendo una **API REST**. Ya viene compilado dentro del backend.
- **Identidad visual:** colores del Manual de identidad visual de la Universidad El Bosque (verde institucional
  #3E4729, Pantone 5743 C, y naranja #F5841F, Pantone 144 C) y logo en blanco sólido sobre el verde, como indica
  el manual (`frontend/public/logo-unbosque-blanco.png`; también hay una versión a color).
- **Datos de prueba:** se cargan solos al arrancar. Alcance del MVP: 5.º a 7.º semestre, 22 asignaturas
  (51 créditos), 6 grupos, 12 docentes, 8 aulas, 44 secciones, 84 clases y 32 franjas.

---

## 1. Cómo ejecutarlo (exposición)

Solo necesita **Java 17 o superior** (`java -version`). No hace falta instalar Node ni la base de datos.

```bash
cd Proyecto_EntregaFinal/backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Luego abra **http://localhost:8080**.

> `mvnw` usa el Maven instalado; si no hay, lo descarga una sola vez. La primera ejecución descarga las
> dependencias de Spring Boot (requiere internet una vez).

**Desde IntelliJ IDEA:** *File → Open* → carpeta `Proyecto_EntregaFinal/backend` (se abre como proyecto Maven) → ejecutar
`HorariosApplication`.

**Como JAR:** `./mvnw package` y luego `java -jar target/horarios-1.0.0.jar`.

### Consola de la base de datos (opcional, no aparece en la interfaz)

http://localhost:8080/h2-console · JDBC URL `jdbc:h2:mem:horarios` · usuario `sa` · contraseña vacía.
Cada arranque empieza con los datos de ejemplo limpios.

---

### Parámetros institucionales en la interfaz

Esta versión se compila sin la tarjeta de parámetros institucionales (`frontend/.env` tiene
`VITE_SIN_PARAMETROS=1`); los parámetros siguen en la base de datos y en la API (`PUT /api/parametros`).
Para mostrar la tarjeta: borre esa línea de `frontend/.env` y ejecute `npm run build`.

---

## 2. Modificar el frontend

Requiere Node 18+.

```bash
# terminal 1
cd backend && ./mvnw spring-boot:run
# terminal 2
cd frontend && npm install && npm run dev     # http://localhost:5173 (con recarga en caliente)
```

Cuando termine: `npm run build` deja la versión compilada en `backend/src/main/resources/static`,
y Spring Boot vuelve a servirla en el puerto 8080.

---

## 3. Cómo se usa (flujo del coordinador)

La aplicación tiene dos pantallas: **Horario** y **Datos**.

**Horario**
1. Arriba, el **progreso del periodo** dice cuál es el siguiente paso. Al abrir hay un **cruce real de docente**:
   D01 con *Bases de datos 2* (5A) e *Ingeniería de software 2* (6A) a la misma hora.
2. **Mostrar**: *Todo el horario* (las 84 clases, con color por semestre) o filtrar por un grupo, un docente o un aula
   (RF-15). Es siempre el mismo horario.
3. **Revisar**, o pulsar una clase, abre el panel con la causa, las franjas sugeridas (con la etiqueta «preferida»
   si el docente prefiere esa franja) y las franjas válidas pintadas de verde en la grilla. Al elegir una franja se
   valida al instante. Si choca, el panel dice **con qué clase exacta** (asignatura, grupo, aula y hora), y
   *Ver dónde choca* la resalta en el horario completo. **Aplicar cambio** guarda y **Deshacer** revierte.
4. **Calidad del horario** (derecha): puntaje de preferencias de 0 a 100 (RB-01 a RB-06, RF-10). Cada indicador se
   abre para ver los casos concretos, resaltarlos en la grilla y moverlos. Se recalcula con cada cambio.
5. **Exportar CSV** e **Imprimir / PDF** (RF-16). **Generar horario** calcula uno nuevo desde cero: cada vez usa una
   semilla distinta para desempatar opciones igual de buenas, así que propone un horario diferente (siempre válido).
   El horario de ejemplo que se carga al arrancar sí es siempre el mismo, para que la demo del cruce sea repetible.
   La mitad de las veces, algunas clases (15 %) se ubican sin mirar las preferencias: el horario sigue cumpliendo
   todas las reglas obligatorias, pero deja **avisos de preferencias** (franja no preferida del docente, días
   seguidos), marcados en amarillo, que se corrigen con *Ver avisos* → *Mover clase*. Nunca se generan conflictos de
   reglas obligatorias: el Entregable 1 lo prohíbe (MVP-11, RNF-03); ese caso se muestra con el cruce del ejemplo.

**Datos**
- **Parámetros institucionales** (RF-02): tamaño objetivo, tolerancia, mínimo de estudiantes, rango de clases por
  semana y la opción de exigir días no consecutivos (la «prueba de cambio» del dossier). Pruebe a poner **35**:
  la revisión previa bloquea la generación y nombra las **9 secciones** que superan el tope (RD-11 es obligatoria).
- **Estado de los datos**: lo que la revisión previa encuentra. Separa lo que impide generar de lo que es solo para
  revisión administrativa (por ejemplo, secciones con menos de 10 estudiantes, RF-14).
- **Datos del periodo**: consulta de secciones, docentes (con las asignaturas para las que están habilitados, RD-08),
  aulas y asignaturas guardadas en la base de datos.
- **Escenarios de prueba**: cada tarjeta explica qué contiene y qué debe pasar al generar. *Usar este escenario*
  reemplaza los datos y el horario por ese caso: periodo normal, docente no habilitado (RD-08), docente sin franjas,
  grupo 7A nocturno y una sola sala de cómputo.

---

## 4. Reglas: dónde se valida cada una

| Regla (Entregable 1) | Tipo | Motor (generar) | Editor (cambios) | Revisión previa | Verificador |
|---|---|---|---|---|---|
| RD-01/02/03 Aula, docente y grupo sin dos clases a la vez | Obligatoria | `AulaLibre`, `DocenteLibre`, `GrupoLibre` | mismas clases | recursos, docentes, grupos | sí |
| RD-04/05 Franjas institucionales de dos horas | Obligatoria | las opciones solo usan franjas configuradas | ídem | — | sí |
| RD-06/07 Cupo y equipamiento del aula | Obligatoria | `CapacidadAula`, `CaracteristicasAula` | mismas clases | secciones sin aula posible; cupos por característica | sí |
| RD-08 Docente habilitado para la asignatura | Obligatoria | — (dato de entrada) | — | `DOCENTE_NO_HABILITADO` | — |
| RD-09 Disponibilidad del docente | Obligatoria | `DisponibilidadDocente` | misma clase | docente con más clases que franjas | sí |
| RD-10 Número de clases semanales (mín.–máx.) | Obligatoria | cada sección se divide en sus clases | — | `FRECUENCIA_INVALIDA` | sí |
| RD-11 Tope de sección (objetivo + tolerancia) | Obligatoria | — (dato de entrada) | — | `SECCION_SUPERA_TOPE` | — |
| Una asignatura no tiene dos clases el mismo día | Obligatoria | `UnaClasePorDia` | misma clase | docente con menos días que clases | sí |
| RB-01 Sin clases en días seguidos (2 o 3 clases) | Preferencia; obligatoria si se activa | orden de valores / `DiasNoConsecutivos` | aviso / rechazo | — | si está activa |
| RB-02 Franjas preferidas del docente | Preferencia | orden de valores | aviso | — | — |
| RB-03/05 Huecos en la jornada del docente | Preferencia | — | — | — | puntaje |
| RB-04 Asignatura muy separada en la semana | Preferencia | — | — | — | puntaje |
| RB-06 Semana equilibrada del grupo | Preferencia | — | — | — | puntaje |
| RF-14 Secciones con menos de 10 estudiantes | Advertencia | — | — | `SECCION_BAJO_MINIMO` | — |

Las preferencias no impiden un horario. El motor las usa para elegir primero las mejores opciones, el editor avisa
si un cambio manual las rompe (se puede guardar igual) y `EvaluadorBlandas` calcula el puntaje y la lista de casos.

---

## 5. Estructura y relación con el diseño

```
backend/src/main/java/co/edu/unbosque/horarios/
├── motor/                         ← núcleo, Java puro (no depende de Spring ni de la BD)
│   ├── dominio/                   Franja, Aula, Docente, Asignatura, Seccion, Sesion, Instancia, Estado, Parametros
│   ├── restricciones/             Restriccion, RestriccionUnaria, RestriccionRelacional,
│   │                              AulaLibre, DocenteLibre, GrupoLibre, UnaClasePorDia, CaracteristicasAula,
│   │                              CapacidadAula, DisponibilidadDocente, DiasNoConsecutivos, RegistroRestricciones
│   ├── solver/                    Solver (Strategy), BacktrackingSolver (retroceso + MRV + forward checking),
│   │                              OpcionesBusqueda, ResultadoSolver, EstadoResultado
│   ├── ServicioProgramacion.java  revisión previa → método (Strategy) con reintentos → verificador → preferencias
│   ├── ValidadorEntrada.java      revisión previa (RD-08, RD-10, RD-11, franjas por docente y grupo, recursos)
│   ├── Editor.java                conflictos (genéricos por regla), alternativas, validarMovimiento, aplicar
│   ├── Verificador.java           verificador independiente (no usa las clases de reglas)
│   ├── EvaluadorBlandas.java      RB-01 a RB-06: puntaje 0-100 y casos concretos
│   └── datos/GeneradorDatos.java  datos de prueba (5.º a 7.º) y escenarios sin solución
├── servicio/                      casos de uso + puertos FuenteInstancia y AlmacenHorarios (inversión de dependencias)
├── persistencia/                  entidades JPA, repositorios y adaptadores H2
├── web/                           HorariosController (API REST) y manejo de errores
└── ConfiguracionAplicacion.java   ensambla todo; aquí se elige el Solver
```

- **Strategy (Tabla 4):** `Solver` es la interfaz; `BacktrackingSolver` la implementa y `ServicioProgramacion`
  recibe el método. Para probar otro (por ejemplo, CP-SAT) basta con otra clase que implemente `Solver` y cambiar el
  bean en `ConfiguracionAplicacion`. Todo resultado termina en uno de cuatro estados: `HORARIO_ENCONTRADO`,
  `SIN_SOLUCION`, `TIEMPO_AGOTADO`, `DATOS_INVALIDOS`.
- **Abierto/cerrado e inversión de dependencias (Tabla 5, RNF-06):** el motor y el editor solo conocen
  `RestriccionUnaria` y `RestriccionRelacional`. Cada regla declara `afecta()` y `relacionadas()` para que el motor
  pode sin conocerla. `UnaClasePorDia` y `DiasNoConsecutivos` se activan en `RegistroRestricciones` sin tocar el
  motor. El servicio depende de los puertos `FuenteInstancia` y `AlmacenHorarios`, no de JPA.
- **Búsqueda (Figura B.1, diagrama de actividades):** CSP con dominio (franja, aula) por clase, guardado como máscara
  de bits. Las reglas unarias se aplican una vez al preparar los dominios, **MRV** elige la clase con menos opciones,
  los valores se ordenan por preferencias y el **forward checking (poda temprana)** descarta de las clases pendientes
  las opciones que quedan bloqueadas; se retrocede apenas una se queda sin opciones. Si se agota el tiempo, se
  reintenta con otro orden.
- **Capas (diagrama de despliegue: controller → service → solver → restricciones → repository → model):**
  `web` → `servicio` → `motor` / `persistencia`.

### Pruebas

`backend/src/test/java/.../MotorTest.java` (JUnit 5, sin Spring), 12 pruebas: horario completo sin incumplimientos
en 10 semillas, el doble de datos en menos de 5 s, que sin MRV ni forward checking la búsqueda no termina con las
aulas ocupadas, alcance de 22 asignaturas y 51 créditos, docente no habilitado, docente sin franjas, grupo 7A,
tope de sección obligatorio, una clase por día, días no consecutivos (motor y editor), puntaje de preferencias y que
el editor detecta un cruce y solo propone alternativas válidas. Se ejecutan con `./mvnw test`.

---

## 6. API REST

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/catalogo` | Franjas, aulas, docentes, asignaturas, secciones, clases, parámetros y advertencias |
| PUT | `/api/parametros` | Guarda los parámetros (incluida la regla de días no consecutivos) |
| POST | `/api/datos/cargar?conjunto=base` | Carga un escenario de prueba |
| GET | `/api/horario` | Horario guardado del periodo, con sus conflictos y su puntaje de preferencias |
| POST | `/api/horario/generar` | Genera el horario (o devuelve la causa si no existe) |
| GET | `/api/horario/alternativas/{clase}` | Franjas válidas y sugeridas para una clase |
| POST | `/api/horario/validar` | `{sesion, franja, aula}` → válido, o motivos y clases con las que choca |
| POST | `/api/horario/aplicar` | Valida y guarda el cambio |
| POST | `/api/horario/deshacer` | Deshace el último cambio |
| GET | `/api/horario/exportar.csv` | Descarga el horario en CSV (RF-16) |

Los tiempos se calculan en cada ejecución, así que pueden variar un poco respecto a los del dossier, que se midieron
con el prototipo en Python.

---

## 7. Pendiente respecto a los documentos

- **RF-01 a RF-07, registrar y modificar datos:** hoy se consultan y se cargan escenarios; falta el CRUD de franjas y
  recesos, asignaturas, docentes (disponibilidad y habilitación) y aulas.
- **Modelo entidad-relación:** las entidades `Grupo`, `Características` y `Conflicto`, y los campos `tiempo_ms` y
  `puntaje_blandas` en `Horario`, aún no existen como tablas propias.
- **Despliegue:** el diagrama indica PostgreSQL; para la exposición se usa H2 embebida.
- **RNF-07 (usuarios autorizados):** pendiente de decidir, porque la Tabla 5 excluye la gestión de usuarios.
