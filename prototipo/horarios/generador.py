"""Generador de instancias de prueba (datos sintéticos, NO institucionales).

Supuestos (deben confirmarse con el Programa):
- 22 asignaturas: 7 en 5.º, 7 en 6.º y 8 en 7.º, 51 créditos en total (dato del M1).
- Sesiones semanales según créditos: 4 cr -> 3, 3 cr -> 2, 2 cr -> 2, 1 cr -> 1.
- Escala = secciones por asignatura: 1x = 2 grupos por semestre (A, B), 1,5x = 3, 2x = 4.
- Cada grupo (p. ej. 5A) es una cohorte que toma todas las asignaturas de su semestre.
- 8 aulas compartidas: 5 salones y 3 salas de cómputo; no crecen con la escala.
- Docentes: 6 por grupo de escala; 25 % solo disponibles desde las 05:00 p. m. (RD-09).
"""
import random

from .modelo import (Asignatura, Aula, Docente, Instancia, Parametros, Seccion,
                     franjas_institucionales)

CREDITOS = {5: [4, 3, 3, 2, 2, 2, 1], 6: [4, 3, 3, 2, 2, 2, 1], 7: [3, 3, 3, 2, 2, 2, 1, 1]}
SESIONES = {4: 3, 3: 2, 2: 2, 1: 1}
COMPUTO = {"5-02", "5-04", "6-01", "6-03", "6-05", "7-02", "7-04"}
SILLAS = {"6-06", "7-06"}


def asignaturas():
    out = []
    for sem, creds in CREDITOS.items():
        for i, c in enumerate(creds, 1):
            cod = f"{sem}-{i:02d}"
            req = set()
            if cod in COMPUTO:
                req.add("computadores")
            if cod in SILLAS:
                req.add("sillas_moviles")
            out.append(Asignatura(cod, f"Asignatura {cod}", sem, c, SESIONES[c], frozenset(req)))
    return out


def aulas(laboratorios=3, copias=1):
    salones = [Aula("S101", 45), Aula("S102", 45, frozenset({"sillas_moviles"})),
               Aula("S103", 45), Aula("S201", 40, frozenset({"sillas_moviles"})), Aula("S202", 40)]
    labs = [Aula("LAB1", 45, frozenset({"computadores"})), Aula("LAB2", 40, frozenset({"computadores"})),
            Aula("LAB3", 35, frozenset({"computadores"}))]
    base = salones + labs[:laboratorios]
    out = list(base)
    for k in range(2, copias + 1):                       # duplicar aulas al escalar
        out += [Aula(f"{a.id}-{k}", a.capacidad, a.caracteristicas) for a in base]
    return out


def generar(escala=1.0, semilla=1, laboratorios=3, escalar_aulas=False):
    rnd = random.Random(semilla)
    franjas = franjas_institucionales()
    noche = {f.id for f in franjas if f.inicio >= 17}
    asigs = asignaturas()
    n_grupos = round(2 * escala)
    letras = "ABCDEFGHIJKL"[:n_grupos]
    n_doc = 6 * n_grupos

    docentes = {}
    for k in range(n_doc):
        did = f"D{k + 1:02d}"
        if k % 4 == 3:                                   # 25 % solo en la noche
            disp = set(noche)
        else:
            disp = {f.id for f in franjas}
            disp -= set(rnd.sample(sorted(disp), rnd.randint(4, 8)))
        manana = rnd.random() < 0.5
        pref = {f.id for f in franjas if (f.inicio < 12) == manana} & disp
        docentes[did] = Docente(did, set(), disp, pref)
    ids = list(docentes)
    for i, a in enumerate(asigs):                        # docentes habilitados crecen con la escala
        for j in range(n_grupos):
            docentes[ids[(i + j * 7 + 3 * j * j) % n_doc]].habilitadas.add(a.codigo)
    for did in ids:                                      # y cada docente con 3-4 asignaturas
        while len(docentes[did].habilitadas) < 3:
            docentes[did].habilitadas.add(rnd.choice(asigs).codigo)

    carga = {d: 0 for d in ids}
    secciones = []
    for a in asigs:
        for L in letras:
            cands = [d for d in ids if a.codigo in docentes[d].habilitadas]
            limite = lambda d: (len(docentes[d].disponibles) * 6) // 10
            cands.sort(key=lambda d: (carga[d] + a.sesiones_semanales > limite(d), carga[d], d))
            d = cands[0]
            carga[d] += a.sesiones_semanales
            max_est = 35 if "computadores" in a.requisitos else 44
            secciones.append(Seccion(f"{a.codigo}{L}", a, f"{a.semestre}{L}",
                                     rnd.randint(18, max_est), d))
    copias = round(escala) if escalar_aulas else 1
    return Instancia(f"factible-{escala}x-s{semilla}", franjas, aulas(laboratorios, copias),
                     docentes, secciones, Parametros())


# ------------------------------------------------------- instancias imposibles
def imposible_docente(semilla=1):
    """I1: un docente con más sesiones que franjas disponibles (detectable por conteo)."""
    inst = generar(1.0, semilla)
    d = max(inst.docentes.values(), key=lambda x: sum(
        s.asignatura.sesiones_semanales for s in inst.secciones if s.docente == x.id))
    d.disponibles = set(sorted(d.disponibles)[:4])
    inst.nombre = f"imposible-docente-{d.id}"
    return inst


def imposible_grupo_noche(semilla=1):
    """I2: dos docentes nocturnos comparten el grupo 7A.

    Cada docente, por separado, tiene franjas suficientes, y el grupo también.
    Pero las 14 sesiones del grupo 7A solo caben en las 10 franjas nocturnas:
    ningún conteo individual lo detecta; solo la búsqueda puede descubrirlo.
    """
    from dataclasses import replace
    inst = generar(1.0, semilla)
    noche = {f.id for f in inst.franjas if f.inicio >= 17}
    secs_7a = [s for s in inst.secciones if s.grupo == "7A"]
    mitad = len(secs_7a) // 2
    for did, grupo in (("DX1", secs_7a[:mitad]), ("DX2", secs_7a[mitad:])):
        inst.docentes[did] = Docente(did, {s.asignatura.codigo for s in grupo}, set(noche), set())
        for s in grupo:
            inst.secciones[inst.secciones.index(s)] = replace(s, docente=did)
    inst.nombre = "imposible-grupo-7A-nocturno"
    return inst


def imposible_laboratorios(semilla=1):
    """I3: escala 2x con una sola sala de cómputo (detectable por conteo)."""
    inst = generar(2.0, semilla, laboratorios=1)
    inst.nombre = "imposible-1-laboratorio-2x"
    return inst
