"""Evaluación de restricciones blandas (RF-10). No afecta la factibilidad."""
from collections import defaultdict


def evaluar_blandas(instancia, asignacion):
    por_seccion = defaultdict(list)
    for s in instancia.sesiones():
        if s.id in asignacion:
            por_seccion[s.seccion].append(asignacion[s.id][0].dia)
    consecutivos = mismo_dia = 0
    for sec, dias in por_seccion.items():
        dias = sorted(dias)
        for a, b in zip(dias, dias[1:]):
            if a == b:
                mismo_dia += 1
            elif b - a == 1:
                consecutivos += 1
    pref_total = pref_ok = 0
    carga_dia = defaultdict(lambda: defaultdict(int))
    for s in instancia.sesiones():
        if s.id not in asignacion:
            continue
        f = asignacion[s.id][0]
        pref = instancia.docentes[s.docente].preferidas
        if pref:
            pref_total += 1
            pref_ok += f.id in pref
        carga_dia[s.grupo][f.dia] += 1
    desbalance = sum(max(d.values()) - min(d.get(i, 0) for i in range(5))
                     for d in carga_dia.values()) / max(len(carga_dia), 1)
    return {
        "pares_mismo_dia": mismo_dia,
        "pares_dias_consecutivos_RB01": consecutivos,
        "pct_franjas_preferidas_RB02": round(100 * pref_ok / pref_total, 1) if pref_total else None,
        "desbalance_promedio_grupo_RB06": round(desbalance, 2),
    }
