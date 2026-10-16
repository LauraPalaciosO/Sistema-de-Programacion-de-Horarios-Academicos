"""Validación de datos de entrada antes de la búsqueda (Módulo de Validación del M1).

Además de revisar coherencia (RD-08, RD-10, RD-11), aplica conteos de
condiciones necesarias: si un recurso requiere más franjas de las que existen,
no hay horario posible y se puede explicar por qué sin ejecutar el algoritmo.
"""
from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field

from .restricciones import RestriccionUnaria


@dataclass
class Diagnostico:
    nivel: str                 # "error" impide generar | "advertencia"
    restricciones: list
    mensaje: str
    elementos: list = field(default_factory=list)


def validar(instancia, restricciones, version=2):
    p = instancia.parametros
    diags = []
    unarias = [r for r in restricciones if isinstance(r, RestriccionUnaria)]

    for sec in instancia.secciones:
        a = sec.asignatura
        doc = instancia.docentes.get(sec.docente)
        if doc is None or a.codigo not in doc.habilitadas:
            diags.append(Diagnostico("error", ["RD-08"],
                         f"El docente {sec.docente} no está habilitado para {a.codigo}", [sec.id]))
        if not (p.sesiones_min <= a.sesiones_semanales <= p.sesiones_max):
            diags.append(Diagnostico("error", ["RD-10"],
                         f"{a.codigo} tiene {a.sesiones_semanales} sesiones; se permite {p.sesiones_min}-{p.sesiones_max}", [sec.id]))
        if sec.estudiantes > p.tamano_maximo:
            diags.append(Diagnostico("error", ["RD-11"],
                         f"La sección {sec.id} tiene {sec.estudiantes} estudiantes (máximo {p.tamano_maximo})", [sec.id]))
        if sec.estudiantes < p.minimo_estudiantes:
            diags.append(Diagnostico("advertencia", ["RF-14"],
                         f"La sección {sec.id} tiene menos de {p.minimo_estudiantes} estudiantes", [sec.id]))
    if any(d.nivel == "error" for d in diags):
        return diags

    sesiones = instancia.sesiones()
    # Dominio unario vacío: ninguna (franja, aula) cumple las reglas individuales
    for s in sesiones:
        if s.indice != 1:
            continue
        validos = 0
        bloqueos = defaultdict(int)
        for f in instancia.franjas:
            for au in instancia.aulas:
                fallo = next((r for r in unarias if not r.permite(s, f, au, instancia)), None)
                if fallo is None:
                    validos += 1
                else:
                    bloqueos[fallo.id] += 1
        if validos == 0:
            peor = max(bloqueos, key=bloqueos.get)
            diags.append(Diagnostico("error", [peor],
                         f"La sección {s.seccion.id} no tiene ninguna franja y aula compatibles", [s.seccion.id]))

    # Conteos de condición necesaria
    por_docente, por_grupo = defaultdict(list), defaultdict(list)
    for s in sesiones:
        por_docente[s.docente].append(s)
        por_grupo[s.grupo].append(s)
    for d, ss in por_docente.items():
        disp = len(instancia.docentes[d].disponibles & {f.id for f in instancia.franjas})
        if len(ss) > disp:
            diags.append(Diagnostico("error", ["RD-02", "RD-09"],
                         f"El docente {d} debe dictar {len(ss)} sesiones pero solo tiene {disp} franjas disponibles",
                         sorted({s.seccion.id for s in ss})))
    for g, ss in por_grupo.items():
        if len(ss) > len(instancia.franjas):
            diags.append(Diagnostico("error", ["RD-03"],
                         f"El grupo {g} necesita {len(ss)} sesiones y hay {len(instancia.franjas)} franjas", [g]))
    if version >= 2:
        diags += _hall_grupo_docentes(instancia, por_grupo)
    por_req = defaultdict(list)
    for s in sesiones:
        por_req[s.seccion.asignatura.requisitos].append(s)
    for req, ss in por_req.items():
        if not req:
            continue
        aulas = [a for a in instancia.aulas if req <= a.caracteristicas]
        cupo = len(aulas) * len(instancia.franjas)
        if len(ss) > cupo:
            diags.append(Diagnostico("error", ["RD-01", "RD-07"],
                         f"Hay {len(ss)} sesiones que requieren {', '.join(sorted(req))} y solo {cupo} cupos aula-franja", []))
    return diags


def _hall_grupo_docentes(instancia, por_grupo):
    """Versión 2 (agregada tras la prueba I2 del prototipo).

    Condición necesaria tipo Hall: para todo subconjunto de docentes de un grupo,
    sus sesiones en ese grupo no pueden superar las franjas en que alguno de ellos
    está disponible (el grupo no admite dos sesiones en la misma franja, RD-03).
    """
    from itertools import combinations
    franjas = {f.id for f in instancia.franjas}
    diags = []
    for g, ss in por_grupo.items():
        carga = defaultdict(int)
        for s in ss:
            carga[s.docente] += 1
        docs = sorted(carga)
        hallado = None
        for k in range(1, min(len(docs), 10) + 1):
            for sub in combinations(docs, k):
                disp = set().union(*(instancia.docentes[d].disponibles for d in sub)) & franjas
                n = sum(carga[d] for d in sub)
                if n > len(disp):
                    hallado = (sub, n, len(disp))
                    break
            if hallado:
                break
        if hallado:
            sub, n, m = hallado
            diags.append(Diagnostico("error", ["RD-03", "RD-09"],
                         f"El grupo {g} tiene {n} sesiones con los docentes {', '.join(sub)}, "
                         f"pero entre todos solo están disponibles en {m} franjas", [g, *sub]))
    return diags
