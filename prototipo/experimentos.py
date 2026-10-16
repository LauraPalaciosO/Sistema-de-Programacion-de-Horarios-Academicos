"""Experimentos del prototipo técnico (Fase 2, secciones D y G).

Uso: python experimentos.py  -> escribe resultados/*.json y resultados/resumen.md
"""
import cProfile
import json
import pstats
import random
import statistics
import sys
import time
from pathlib import Path

from horarios.edicion import Editor
from horarios.evaluacion import evaluar_blandas
from horarios.generador import (generar, imposible_docente, imposible_grupo_noche,
                                imposible_laboratorios)
from horarios.registro import restricciones_duras
from horarios.solver import BacktrackingSolver
from horarios.verificador import verificar

OUT = Path("resultados"); OUT.mkdir(exist_ok=True)
SEMILLAS = range(1, 11)


def p95(xs):
    xs = sorted(xs)
    return xs[min(len(xs) - 1, int(round(0.95 * (len(xs) - 1))))]


def correr(inst, **kw):
    r = BacktrackingSolver(restricciones_duras(), **kw).resolver(inst)
    errores = verificar(inst, r.asignacion) if r.estado == "factible" else None
    return r, errores


def e1_escalas():
    """Escenarios factibles: 1x, 2x y 4x duplicando docentes, aulas y cursos."""
    filas = []
    for esc in (1.0, 2.0, 4.0):
        tiempos, ok, errs, retro, blandas = [], 0, 0, [], []
        for sem in SEMILLAS:
            inst = generar(esc, sem, escalar_aulas=True)
            r, e = correr(inst, limite_segundos=30)
            tiempos.append(r.segundos); retro.append(r.retrocesos)
            if r.estado == "factible":
                ok += 1; errs += len(e)
                blandas.append(evaluar_blandas(inst, r.asignacion))
        filas.append({
            "escala": esc, "sesiones": len(inst.sesiones()), "docentes": len(inst.docentes),
            "aulas": len(inst.aulas), "secciones": len(inst.secciones), "corridas": len(SEMILLAS),
            "factibles": ok, "violaciones_duras_verificador": errs,
            "t_media_s": round(statistics.mean(tiempos), 3), "t_p95_s": round(p95(tiempos), 3),
            "t_max_s": round(max(tiempos), 3), "retrocesos_max": max(retro),
            "rb01_pares_consecutivos_media": round(statistics.mean(b["pares_dias_consecutivos_RB01"] for b in blandas), 2),
            "rb02_pct_preferidas_media": round(statistics.mean(b["pct_franjas_preferidas_RB02"] for b in blandas), 1),
        })
        print("E1", filas[-1], flush=True)
    return filas


def e1b_aulas_fijas():
    """Cursos y docentes crecen, pero las aulas no: busca el recurso que se agota primero."""
    filas = []
    for esc in (1.0, 2.0, 3.0):
        inst = generar(esc, 1, escalar_aulas=False)
        n = len(inst.sesiones()); cupos = len(inst.franjas) * len(inst.aulas)
        comp = sum(1 for s in inst.sesiones() if "computadores" in s.seccion.asignatura.requisitos)
        labs = sum(1 for a in inst.aulas if "computadores" in a.caracteristicas) * len(inst.franjas)
        r, _ = correr(inst, limite_segundos=30)
        filas.append({"escala": esc, "sesiones": n, "ocupacion_aulas_pct": round(100 * n / cupos, 1),
                      "ocupacion_labs_pct": round(100 * comp / labs, 1), "resultado": r.estado,
                      "segundos": round(r.segundos, 2), "diagnostico": r.diagnostico[:2]})
        print("E1b", filas[-1], flush=True)
    return filas


def e2_sin_heuristicas():
    """Backtracking cronológico (sin MRV, sin forward checking, sin orden blando) a escala 1x."""
    filas = []
    for sem in range(1, 6):
        inst = generar(1.0, sem, escalar_aulas=True)
        r, e = correr(inst, mrv=False, forward_checking=False, orden_blando=False, limite_segundos=30)
        filas.append({"semilla": sem, "resultado": r.estado, "segundos": round(r.segundos, 2),
                      "nodos": r.nodos, "retrocesos": r.retrocesos,
                      "violaciones": None if e is None else len(e)})
        print("E2", filas[-1], flush=True)
    return filas


def e2b_ajustadas():
    """Instancias ajustadas (aulas fijas): con y sin heurísticas."""
    filas = []
    for esc in (2.0, 2.5):
        for sem in (1, 2, 3):
            inst = generar(esc, sem, escalar_aulas=False)
            ocup = round(100 * len(inst.sesiones()) / (len(inst.franjas) * len(inst.aulas)), 1)
            for modo, kw in (("MRV+FC", {}), ("cronologico", dict(mrv=False, forward_checking=False, orden_blando=False))):
                r, e = correr(inst, limite_segundos=30, **kw)
                filas.append({"escala": esc, "semilla": sem, "ocupacion_aulas_pct": ocup, "modo": modo,
                              "resultado": r.estado, "segundos": round(r.segundos, 2), "retrocesos": r.retrocesos,
                              "violaciones": None if e is None else len(e)})
                print("E2b", filas[-1], flush=True)
    return filas


def e3_imposibles():
    filas = []
    casos = [("I1 docente sin franjas", imposible_docente(), 2),
             ("I2 grupo 7A nocturno", imposible_grupo_noche(), 1),
             ("I2 grupo 7A nocturno", imposible_grupo_noche(), 2),
             ("I3 un solo laboratorio (2x)", imposible_laboratorios(), 2)]
    for nombre, inst, version in casos:
        r, _ = correr(inst, limite_segundos=60, version_validacion=version)
        filas.append({"caso": nombre, "validacion": f"v{version}", "resultado": r.estado,
                      "segundos": round(r.segundos, 2), "retrocesos": r.retrocesos,
                      "diagnostico": r.diagnostico[:2]})
        print("E3", filas[-1], flush=True)
    return filas


def e4_edicion():
    """Validación de cambios manuales (RNF-02) y cálculo de alternativas para el panel de conflicto."""
    filas = []
    for esc in (1.0, 2.0):
        inst = generar(esc, 1, escalar_aulas=True)
        r, _ = correr(inst)
        ed = Editor(inst, r.asignacion, restricciones_duras())
        rnd = random.Random(7)
        ids = list(r.asignacion)
        tv, rechazados, ta, n_alt = [], 0, [], []
        for _ in range(500):
            sid = rnd.choice(ids); f = rnd.choice(inst.franjas); a = rnd.choice(inst.aulas)
            t0 = time.perf_counter(); fallas = ed.validar_movimiento(sid, f, a)
            tv.append(time.perf_counter() - t0); rechazados += bool(fallas)
        for sid in ids[:50]:
            t0 = time.perf_counter(); _, total = ed.alternativas(sid)
            ta.append(time.perf_counter() - t0); n_alt.append(total)
        filas.append({"escala": esc, "validacion_ms_media": round(1000 * statistics.mean(tv), 3),
                      "validacion_ms_max": round(1000 * max(tv), 3), "movimientos_rechazados_pct": round(100 * rechazados / 500, 1),
                      "alternativas_ms_media": round(1000 * statistics.mean(ta), 1),
                      "alternativas_ms_max": round(1000 * max(ta), 1),
                      "alternativas_validas_media": round(statistics.mean(n_alt), 1),
                      "sesiones_sin_alternativas": sum(1 for n in n_alt if n == 0)})
        print("E4", filas[-1], flush=True)
    return filas


def e5_perfil():
    """Dónde se va el tiempo (cuello de botella) a escalas 1x, 2x y 4x."""
    filas = []
    for esc in (1.0, 2.0, 4.0):
        inst = generar(esc, 1, escalar_aulas=True)
        solver = BacktrackingSolver(restricciones_duras())
        pr = cProfile.Profile(); pr.enable(); r = solver.resolver(inst); pr.disable()
        st = pstats.Stats(pr)
        total = sum(v[3] for k, v in st.stats.items() if k[2] == "resolver")
        def cum(nombre):
            return sum(v[3] for k, v in st.stats.items() if k[2] == nombre)
        def llamadas(nombre):
            return sum(v[1] for k, v in st.stats.items() if k[2] == nombre)
        filas.append({"escala": esc, "sesiones": len(inst.sesiones()), "t_perfilado_s": round(total, 2),
                      "pct_forward_checking": round(100 * cum("_propagar") / total, 1),
                      "pct_dominio_inicial": round(100 * cum("_dominio_inicial") / total, 1),
                      "pct_validacion_entrada": round(100 * cum("validar") / total, 1),
                      "pct_orden_valores": round(100 * cum("_valores") / total, 1),
                      "llamadas_permite": sum(llamadas("permite") for _ in [0])})
        print("E5", filas[-1], flush=True)
    return filas


if __name__ == "__main__":
    pasos = sys.argv[1:] or ["e1", "e1b", "e2", "e3", "e4", "e5"]
    for p in pasos:
        datos = globals()[{"e1": "e1_escalas", "e1b": "e1b_aulas_fijas", "e2": "e2_sin_heuristicas", "e2b": "e2b_ajustadas",
                           "e3": "e3_imposibles", "e4": "e4_edicion", "e5": "e5_perfil"}[p]]()
        (OUT / f"{p}.json").write_text(json.dumps(datos, ensure_ascii=False, indent=2))
