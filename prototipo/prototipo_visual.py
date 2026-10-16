"""Ejecuta el prototipo técnico y muestra TODO lo que hace en una página web.

Uso:
    python prototipo_visual.py             -> corrida normal (unos 30 s)
    python prototipo_visual.py --completo  -> agrega el tamaño cuádruple y 10 casos por tamaño

Genera prototipo_visual.html y lo abre en el navegador. Todo lo que aparece en la
página sale de esta ejecución: no hay datos escritos a mano.
"""
import datetime
import json
import platform
import statistics
import sys
import time
import webbrowser
from collections import Counter
from pathlib import Path

from horarios.dias_no_consecutivos import DiasNoConsecutivos
from horarios.edicion import Editor
from horarios.evaluacion import evaluar_blandas
from horarios.generador import (generar, imposible_docente, imposible_grupo_noche,
                                imposible_laboratorios)
from horarios.registro import restricciones_duras
from horarios.solver import BacktrackingSolver
from horarios.validacion import validar
from horarios.verificador import verificar

COMPLETO = "--completo" in sys.argv
BASE = Path(__file__).parent

# Nombres que se muestran en pantalla. Las asignaturas sin nombre real usan su código.
NOMBRES = {
    "5-01": "Bases de datos 2", "5-02": "Ingeniería de software 1",
    "5-03": "Microprocesadores y programación assembler", "5-04": "Sistemas operacionales",
    "5-05": "Dinámica de sistemas", "5-06": "Ingeniería de software 1", "5-07": "Electiva 3",
    "6-06": "Ingeniería de software 2",
}
nombre = lambda codigo: NOMBRES.get(codigo, f"Asignatura {codigo}")
REGLAS = {r.id: r.descripcion for r in restricciones_duras() + [DiasNoConsecutivos()]}


def paso(msg):
    print(f"  · {msg}", flush=True)


def sin_codigos(resultado, solver=None):
    """Diagnóstico en lenguaje del usuario (sin identificadores internos de reglas)."""
    if resultado.estado == "datos_invalidos":
        return None
    if solver is not None and solver.culpables:
        return [f"{REGLAS.get(rid, rid)} ({rec}): bloqueó {n} intentos"
                for (rid, rec), n in solver.culpables.most_common(3)]
    return []


def seccion_json(inst):
    return {s.id: {"codigo": s.asignatura.codigo, "nombre": nombre(s.asignatura.codigo), "grupo": s.grupo,
                   "docente": s.docente, "estudiantes": s.estudiantes,
                   "requisitos": sorted(s.asignatura.requisitos), "sesiones": s.asignatura.sesiones_semanales}
            for s in inst.secciones}


print("\nEjecutando el prototipo técnico…")
t_total = time.perf_counter()

# 1. Generación paso a paso -------------------------------------------------------
paso("Generando el horario y registrando cada decisión del motor")
inst = generar(1.0, 1)
solver = BacktrackingSolver(restricciones_duras(), registrar_pasos=True)
res = solver.resolver(inst)
errores = verificar(inst, res.asignacion)
blandas = evaluar_blandas(inst, res.asignacion)
generacion = {
    "estado": res.estado, "segundos": round(res.segundos, 3), "nodos": res.nodos, "retrocesos": res.retrocesos,
    "incumplimientos": len(errores), "blandas": blandas,
    "asignacion": {sid: [f.id, a.id] for sid, (f, a) in res.asignacion.items()},
    "pasos": solver.pasos,
}
datos_instancia = {
    "franjas": [{"id": f.id, "dia": f.dia, "inicio": f.inicio} for f in inst.franjas],
    "aulas": [{"id": a.id, "capacidad": a.capacidad, "caracteristicas": sorted(a.caracteristicas)} for a in inst.aulas],
    "docentes": len(inst.docentes), "secciones": seccion_json(inst),
    "grupos": sorted({s.grupo for s in inst.secciones}), "clases": len(inst.sesiones()),
}

# 2. Conflicto real y alternativas ------------------------------------------------
paso("Provocando un cruce de docente y calculando qué franjas lo resuelven")
ses = {s.id: s for s in inst.sesiones()}
asig = dict(res.asignacion)
ocup_5a = {asig[k][0].id for k in asig if ses[k].grupo == "5A"}
conflicto = None
for sid in sorted(asig):
    x = ses[sid]
    if x.grupo != "5A":
        continue
    for oid in sorted(asig):
        y = ses[oid]
        if y.docente == x.docente and y.grupo != "5A" and asig[oid][0].id not in ocup_5a:
            fy = asig[oid][0]
            libre = next((a for a in inst.aulas if a.capacidad >= x.seccion.estudiantes
                          and x.seccion.asignatura.requisitos <= a.caracteristicas
                          and all(not (asig[k][0].id == fy.id and asig[k][1].id == a.id) for k in asig)), None)
            if libre and fy.id in inst.docentes[x.docente].disponibles:
                conflicto = (sid, oid, fy, libre)
                break
    if conflicto:
        break
sid, oid, fy, aula_c = conflicto
original = asig[sid]
asig[sid] = (fy, aula_c)
ed = Editor(inst, asig, restricciones_duras())
detectados = [e for e in verificar(inst, asig)]
t0 = time.perf_counter()
pares, total_validas = ed.alternativas(sid, maximo=200)
sugeridas, vistas = [], set()
for f, a in pares:                      # una sugerencia por franja, en el orden del editor
    if f.id not in vistas:
        vistas.add(f.id)
        sugeridas.append((f, a))
sugeridas = sugeridas[:5]
t_alt = (time.perf_counter() - t0) * 1000
opciones = []
for f in inst.franjas:
    if f.id == fy.id:
        continue
    t0 = time.perf_counter()
    valida, fallas_aula = None, None
    for a in sorted(inst.aulas, key=lambda a: (a.id != aula_c.id, a.capacidad)):
        fallas = ed.validar_movimiento(sid, f, a)
        if not fallas:
            valida = a
            break
        if a.id == aula_c.id:
            fallas_aula = fallas
    opciones.append({"franja": f.id, "valida": valida is not None, "aula": (valida or aula_c).id,
                     "motivos": sorted({i for i, _, _ in (fallas_aula or [])}),
                     "ms": round((time.perf_counter() - t0) * 1000, 3)})
aula_de = {o["franja"]: o["aula"] for o in opciones if o["valida"]}
sugeridas = [(f, next(a for a in inst.aulas if a.id == aula_de.get(f.id, a.id))) for f, a in sugeridas]
conflicto_json = {
    "sesion": sid, "otra": oid, "franja": fy.id, "aula": aula_c.id, "docente": ses[sid].docente,
    "franja_original": original[0].id, "asignacion": {k: [f.id, a.id] for k, (f, a) in asig.items()},
    "detectados": len(detectados), "sugeridas": [[f.id, a.id] for f, a in sugeridas],
    "total_validas": total_validas, "ms_alternativas": round(t_alt, 2), "opciones": opciones,
}

# 3. Casos sin solución ----------------------------------------------------------
paso("Probando los casos sin solución (uno de ellos espera hasta 20 s a propósito)")
imposibles = []
casos = [
    ("Docente sin franjas suficientes", "Un docente debe dictar más clases que franjas en las que está disponible.",
     imposible_docente(), 2, 20),
    ("Grupo 7A solo con docentes nocturnos — revisión previa inicial",
     "Las 14 clases del grupo 7A quedan con dos docentes que solo dictan de noche, y de noche solo hay 10 franjas. "
     "Cada docente por separado parece tener tiempo suficiente.", imposible_grupo_noche(), 1, 20),
    ("Grupo 7A solo con docentes nocturnos — revisión previa mejorada", "El mismo caso anterior.",
     imposible_grupo_noche(), 2, 20),
    ("Una sola sala de cómputo con el doble de cursos",
     "Se duplican los cursos y queda una única sala con computadores.", imposible_laboratorios(), 2, 20),
]
for titulo, desc, inst_i, version, limite in casos:
    s_i = BacktrackingSolver(restricciones_duras(), limite_segundos=limite, version_validacion=version)
    r_i = s_i.resolver(inst_i)
    if r_i.estado == "datos_invalidos":
        diag = [d.mensaje for d in validar(inst_i, restricciones_duras(), version) if d.nivel == "error"]
    else:
        diag = sin_codigos(r_i, s_i)
    imposibles.append({"titulo": titulo, "descripcion": desc, "estado": r_i.estado,
                       "segundos": round(r_i.segundos, 2), "diagnostico": diag, "limite": limite,
                       "horario_invalido": bool(r_i.asignacion)})

# 4. Escalabilidad ---------------------------------------------------------------
tamanos = [("Actual", 1.0), ("Doble", 2.0)] + ([("Cuádruple", 4.0)] if COMPLETO else [])
n_casos = 10 if COMPLETO else 3
paso(f"Midiendo escalabilidad ({n_casos} casos por tamaño)")
escala = []
for etiqueta, e in tamanos:
    tiempos, ok, inc = [], 0, 0
    for semilla in range(1, n_casos + 1):
        i_e = generar(e, semilla, escalar_aulas=True)
        r_e = BacktrackingSolver(restricciones_duras(), limite_segundos=30).resolver(i_e)
        tiempos.append(r_e.segundos)
        if r_e.estado == "factible":
            ok += 1
            inc += len(verificar(i_e, r_e.asignacion))
    escala.append({"tamano": etiqueta, "docentes": len(i_e.docentes), "aulas": len(i_e.aulas),
                   "secciones": len(i_e.secciones), "clases": len(i_e.sesiones()), "casos": n_casos,
                   "completos": ok, "incumplimientos": inc, "promedio": round(statistics.mean(tiempos), 3),
                   "maximo": round(max(tiempos), 3), "tiempos": [round(t, 3) for t in tiempos]})
    paso(f"  {etiqueta}: {escala[-1]['promedio']} s en promedio")
paso("Midiendo qué pasa si crecen los cursos pero no las aulas")
ocupacion = []
for e, lim in ((2.0, 10), (2.5, 10)):
    i_o = generar(e, 1, escalar_aulas=False)
    r_o = BacktrackingSolver(restricciones_duras(), limite_segundos=lim).resolver(i_o)
    ocupacion.append({"ocupacion": round(100 * len(i_o.sesiones()) / (len(i_o.franjas) * len(i_o.aulas)), 1),
                      "clases": len(i_o.sesiones()), "aulas": len(i_o.aulas), "estado": r_o.estado,
                      "segundos": round(r_o.segundos, 2), "limite": lim})

# 5. Prueba de cambio -------------------------------------------------------------
paso("Prueba de cambio: volviendo obligatoria la regla de días no consecutivos")
cambio = {}
# Se busca un caso de prueba donde la preferencia no se haya cumplido, para que el efecto se vea.
for semilla_c in range(1, 21):
    i_c = generar(1.0, semilla_c)
    r_c = BacktrackingSolver(restricciones_duras()).resolver(i_c)
    if evaluar_blandas(i_c, r_c.asignacion)["pares_dias_consecutivos_RB01"] > 0:
        break
for clave, reglas in (("antes", restricciones_duras()), ("despues", restricciones_duras() + [DiasNoConsecutivos()])):
    i_c = generar(1.0, semilla_c)
    s_c = {x.id: x for x in i_c.sesiones()}
    r_c = BacktrackingSolver(reglas).resolver(i_c)
    porsec = {}
    for k, (f, a) in r_c.asignacion.items():
        if s_c[k].seccion.asignatura.sesiones_semanales in (2, 3):
            porsec.setdefault(s_c[k].seccion.id, []).append(f.dia)
    afectadas = sorted(sec for sec, d in porsec.items()
                       if any(b - a2 <= 1 for a2, b in zip(sorted(d), sorted(d)[1:])))
    cambio[clave] = {"estado": r_c.estado, "segundos": round(r_c.segundos, 3), "afectadas": afectadas,
                     "incumplimientos": len(verificar(i_c, r_c.asignacion)),
                     "asignacion": {k: [f.id, a.id] for k, (f, a) in r_c.asignacion.items()}}
cambio["semilla"] = semilla_c
cambio["secciones"] = seccion_json(i_c)

DATA = {
    "meta": {"fecha": datetime.datetime.now().strftime("%d/%m/%Y %H:%M"), "python": platform.python_version(),
             "equipo": platform.node(), "segundos_total": round(time.perf_counter() - t_total, 1), "completo": COMPLETO},
    "instancia": datos_instancia, "generacion": generacion, "conflicto": conflicto_json,
    "imposibles": imposibles, "escala": escala, "ocupacion": ocupacion, "cambio": cambio,
}
plantilla = (BASE / "interaccion" / "plantilla_visual.html").read_text(encoding="utf-8")
salida = BASE / "prototipo_visual.html"
salida.write_text(plantilla.replace("__DATA__", json.dumps(DATA, ensure_ascii=False)), encoding="utf-8")
print(f"\nListo en {DATA['meta']['segundos_total']} s. Página generada: {salida.name}")
if "--sin-abrir" not in sys.argv:
    webbrowser.open(salida.resolve().as_uri())
