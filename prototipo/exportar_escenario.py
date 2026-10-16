"""Exporta un escenario real del prototipo técnico para el prototipo de interacción.

Toma el horario factible (1x, semilla 1), provoca un cruce de docente (RD-02) en el
grupo 5A y calcula con el Editor qué franjas son válidas para resolverlo.
"""
import json
from horarios.edicion import Editor
from horarios.generador import generar
from horarios.registro import restricciones_duras
from horarios.solver import BacktrackingSolver

NOMBRES = {"5-01": "Bases de Datos", "5-02": "Programación Avanzada", "5-03": "Algoritmos y Complejidad",
           "5-04": "Sistemas Operativos", "5-05": "Probabilidad y Estadística",
           "5-06": "Ingeniería de Software I", "5-07": "Ética Profesional",
           "6-01": "Redes de Computadores", "6-02": "Ingeniería de Software II", "6-03": "Inteligencia Artificial",
           "6-04": "Arquitectura de Software", "6-05": "Computación Gráfica",
           "6-06": "Interacción Humano-Computador", "6-07": "Gestión de Proyectos"}

inst = generar(1.0, 1)
r = BacktrackingSolver(restricciones_duras()).resolver(inst)
asig = dict(r.asignacion)
ses = {s.id: s for s in inst.sesiones()}
F = {f.id: f for f in inst.franjas}

# Elegir una sesión X de 5A cuyo docente tenga otra sesión Y (de otro grupo) en una franja
# donde 5A esté libre: mover X allí produce solo un conflicto RD-02.
ocup_5a = {asig[s][0].id for s in asig if ses[s].grupo == "5A"}
elegido = None
for sid in sorted(asig):
    x = ses[sid]
    if x.grupo != "5A":
        continue
    for oid in sorted(asig):
        y = ses[oid]
        if y.docente == x.docente and y.grupo != "5A" and asig[oid][0].id not in ocup_5a:
            fy, _ = asig[oid]
            aula_ok = next((a for a in inst.aulas if a.capacidad >= x.seccion.estudiantes
                            and x.seccion.asignatura.requisitos <= a.caracteristicas
                            and all(not (asig[k][0].id == fy.id and asig[k][1].id == a.id) for k in asig)), None)
            if aula_ok and fy.id in inst.docentes[x.docente].disponibles:
                elegido = (sid, oid, fy, aula_ok); break
    if elegido:
        break
sid, oid, fy, aula_c = elegido
original = asig[sid]
asig[sid] = (fy, aula_c)                         # conflicto provocado
ed = Editor(inst, asig, restricciones_duras())

def nombre(s):
    return NOMBRES.get(s.seccion.asignatura.codigo, s.seccion.asignatura.nombre)

opciones = []
for f in inst.franjas:
    if f.id == fy.id:
        continue
    valida, fallas_aula = None, None
    for a in sorted(inst.aulas, key=lambda a: (a.id != aula_c.id, a.capacidad)):
        fallas = ed.validar_movimiento(sid, f, a)
        if not fallas:
            valida = a; break
        if a.id == aula_c.id:
            fallas_aula = fallas
    opciones.append({"franja": f.etiqueta(), "dia": f.dia, "inicio": f.inicio,
                     "valida": valida is not None, "aula": valida.id if valida else aula_c.id,
                     "motivos": [] if valida else [{"id": i, "texto": d, "recurso": rr} for i, d, rr in (fallas_aula or [])]})
x, y = ses[sid], ses[oid]
grid = []
for k, (f, a) in asig.items():
    s = ses[k]
    if s.grupo == "5A":
        grid.append({"sesion": k, "asignatura": nombre(s), "seccion": s.seccion.id, "docente": s.docente,
                     "aula": a.id, "dia": f.dia, "inicio": f.inicio, "conflicto": k == sid})
data = {
    "grupo": "5A",
    "sesiones": sorted(grid, key=lambda g: (g["dia"], g["inicio"])),
    "conflicto": {"restriccion": "RD-02", "texto": "Un docente no puede tener dos sesiones simultáneas",
                  "docente": x.docente, "franja": fy.etiqueta(), "dia": fy.dia, "inicio": fy.inicio,
                  "sesion": {"id": sid, "asignatura": nombre(x), "seccion": x.seccion.id, "aula": aula_c.id},
                  "otra": {"id": oid, "asignatura": nombre(y),
                           "seccion": y.seccion.id, "grupo": y.grupo, "aula": asig[oid][1].id},
                  "franja_original": original[0].etiqueta()},
    "opciones": opciones,
    "resumen": {"validas": sum(o["valida"] for o in opciones), "total": len(opciones)},
}
json.dump(data, open("resultados/escenario_interaccion.json", "w"), ensure_ascii=False, indent=1)
print(json.dumps(data["conflicto"], ensure_ascii=False, indent=1), data["resumen"])
for o in opciones[:40]:
    print(o["franja"], o["valida"], o["aula"], [m["id"] for m in o["motivos"]])
