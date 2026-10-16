"""Verificador independiente (oráculo de pruebas, RNF-03).

No reutiliza las clases de restricciones ni el motor: recalcula cada regla dura
directamente sobre los datos, para que un error en el motor no se oculte a sí mismo.
"""
from collections import Counter


def verificar(instancia, asignacion):
    errores = []
    sesiones = instancia.sesiones()
    franjas = {f.id: f for f in instancia.franjas}
    faltan = [s.id for s in sesiones if s.id not in asignacion]
    if faltan:
        errores.append(f"RD-10: {len(faltan)} sesiones sin asignar")
    aula_f, doc_f, grp_f = Counter(), Counter(), Counter()
    for s in sesiones:
        if s.id not in asignacion:
            continue
        f, a = asignacion[s.id]
        if f.id not in franjas or f.fin - f.inicio != 2:
            errores.append(f"RD-04/RD-05: {s.id}")
        if a.capacidad < s.seccion.estudiantes:
            errores.append(f"RD-06: {s.id} en {a.id}")
        if not s.seccion.asignatura.requisitos <= a.caracteristicas:
            errores.append(f"RD-07: {s.id} en {a.id}")
        if f.id not in instancia.docentes[s.docente].disponibles:
            errores.append(f"RD-09: {s.id}")
        if s.seccion.asignatura.codigo not in instancia.docentes[s.docente].habilitadas:
            errores.append(f"RD-08: {s.id}")
        aula_f[(f.id, a.id)] += 1
        doc_f[(f.id, s.docente)] += 1
        grp_f[(f.id, s.grupo)] += 1
    errores += [f"RD-01: aula {k[1]} franja {k[0]}" for k, n in aula_f.items() if n > 1]
    errores += [f"RD-02: docente {k[1]} franja {k[0]}" for k, n in doc_f.items() if n > 1]
    errores += [f"RD-03: grupo {k[1]} franja {k[0]}" for k, n in grp_f.items() if n > 1]
    return errores
