# Prototipo técnico — Sistema de Programación de Horarios Académicos (Grupo 1)

Prototipo de la Fase 2 ("De la incertidumbre al diseño"). Responde la pregunta de B2.2:
¿puede un backtracking heurístico generar un horario completo que respete las restricciones
duras y reconocer cuando no hay solución?

Los datos son sintéticos (casos de prueba), no institucionales. Supuestos en `horarios/generador.py`.

## Estructura
- `horarios/modelo.py` — entidades (Franja, Aula, Docente, Asignatura, Sección, Sesión, Parámetros)
- `horarios/restricciones.py` — interfaz Restriccion y reglas duras RD-01..RD-09
- `horarios/registro.py` — reglas activas (único punto que conoce las reglas concretas)
- `horarios/validacion.py` — validación de entrada y conteos de condición necesaria (v1 y v2)
- `horarios/solver.py` — interfaz Solver (Strategy) y BacktrackingSolver (MRV + forward checking)
- `horarios/edicion.py` — validación de cambios manuales y alternativas (RF-12, RF-13)
- `horarios/evaluacion.py` — restricciones blandas (RB-01, RB-02, RB-06)
- `horarios/verificador.py` — oráculo independiente para las pruebas (RNF-03)
- `experimentos.py` — E1 a E5; resultados en `resultados/`
- `exportar_escenario.py` — genera el escenario real del prototipo de interacción
- `interaccion/` — prototipo de interacción (HTML)
- `diagramas/` — diagramas UML (PlantUML, PNG, SVG)

## Reproducir
```
python3 experimentos.py            # todos
python3 experimentos.py e1 e3      # algunos
```
Requiere Python 3.10 o superior, sin dependencias externas.

## Origen
Código generado con asistencia de IA (Claude, Anthropic) y declarado en el anexo de
transparencia de uso de IA del dossier.
