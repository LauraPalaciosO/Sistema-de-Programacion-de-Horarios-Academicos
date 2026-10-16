"""Genera un horario con el prototipo técnico y lo muestra en la terminal.

Uso:
    python ver_horario.py            -> grupo 5A, caso 1
    python ver_horario.py 6B         -> otro grupo (5A, 5B, 6A, 6B, 7A, 7B)
    python ver_horario.py 7A 3       -> otro grupo y otro caso de prueba
"""
import sys

from horarios.generador import generar
from horarios.modelo import DIAS
from horarios.registro import restricciones_duras
from horarios.solver import BacktrackingSolver
from horarios.verificador import verificar

grupo = sys.argv[1].upper() if len(sys.argv) > 1 else "5A"
caso = int(sys.argv[2]) if len(sys.argv) > 2 else 1

inst = generar(1.0, caso)
r = BacktrackingSolver(restricciones_duras()).resolver(inst)
if r.estado != "factible":
    print("No se generó horario:", r.estado)
    for d in r.diagnostico:
        print(" -", d)
    sys.exit(1)

ses = {s.id: s for s in inst.sesiones()}
grupos = sorted({s.grupo for s in ses.values()})
if grupo not in grupos:
    print(f"El grupo {grupo} no existe. Grupos disponibles: {', '.join(grupos)}")
    sys.exit(1)

celda = {}
for sid, (f, a) in r.asignacion.items():
    s = ses[sid]
    if s.grupo == grupo:
        celda[(f.dia, f.inicio)] = f"{s.seccion.asignatura.codigo} {s.docente} {a.id}"

ANCHO = 18
horas = [7, 9, 13, 15, 17, 19]
print(f"\nHorario del grupo {grupo} (caso de prueba {caso}) - generado en {r.segundos:.2f} s\n")
print("Franja      " + "".join(d.ljust(ANCHO) for d in DIAS))
print("-" * (12 + ANCHO * 6))
for h in horas:
    fila = f"{h:02d}:00-{h + 2:02d}:00 "
    for d in range(6):
        if d == 5 and h > 9:
            fila += "(sin franja)".ljust(ANCHO)
        else:
            fila += celda.get((d, h), "·").ljust(ANCHO)
    print(fila)

errores = verificar(inst, r.asignacion)
print("\nCada celda: asignatura, docente y aula. «·» = franja libre.")
print(f"Clases del grupo: {len(celda)}  |  Reglas obligatorias incumplidas en todo el horario: {len(errores)}")
