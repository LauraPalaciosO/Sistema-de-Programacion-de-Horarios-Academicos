"""Motor de generación (Strategy): interfaz Solver y backtracking heurístico.

BacktrackingSolver implementa:
- MRV: elige la sesión con menos valores (franja, aula) restantes.
- Forward checking: al asignar, poda los valores incompatibles de las demás.
- Orden de valores guiado por restricciones blandas (RB-01, RB-02).
- Diagnóstico: cuenta qué restricción y qué recurso vacían dominios (RF-11).
Con mrv=False y forward_checking=False se obtiene un backtracking cronológico simple.
"""
from __future__ import annotations

import time
from abc import ABC, abstractmethod
from collections import Counter, defaultdict
from dataclasses import dataclass, field

from .estado import Estado
from .restricciones import RestriccionRelacional, RestriccionUnaria
from .validacion import validar


@dataclass
class Resultado:
    estado: str                    # factible | sin_solucion | limite_tiempo | datos_invalidos
    asignacion: dict = field(default_factory=dict)
    nodos: int = 0
    retrocesos: int = 0
    segundos: float = 0.0
    diagnostico: list = field(default_factory=list)


class Solver(ABC):
    @abstractmethod
    def resolver(self, instancia) -> Resultado: ...


class BacktrackingSolver(Solver):
    def __init__(self, restricciones, mrv=True, forward_checking=True,
                 orden_blando=True, limite_segundos=10.0, validar_entrada=True, version_validacion=2,
                 registrar_pasos=False):
        self.unarias = [r for r in restricciones if isinstance(r, RestriccionUnaria)]
        self.relacionales = [r for r in restricciones if isinstance(r, RestriccionRelacional)]
        self.restricciones = restricciones
        self.mrv, self.fc, self.orden_blando = mrv, forward_checking, orden_blando
        self.limite = limite_segundos
        self.validar_entrada = validar_entrada
        self.version_validacion = version_validacion
        self.registrar_pasos = registrar_pasos   # guarda cada decisión para poder mostrarla
        self.pasos = []

    # ------------------------------------------------------------------ API
    def resolver(self, inst) -> Resultado:
        t0 = time.perf_counter()
        if self.validar_entrada:
            diags = [d for d in validar(inst, self.restricciones, self.version_validacion) if d.nivel == "error"]
            if diags:
                return Resultado("datos_invalidos", segundos=time.perf_counter() - t0,
                                 diagnostico=[f"[{', '.join(d.restricciones)}] {d.mensaje}" for d in diags])
        self.inst = inst
        self.sesiones = inst.sesiones()
        self.franja = {f.id: f for f in inst.franjas}
        self.aula = {a.id: a for a in inst.aulas}
        self.hermanas = defaultdict(list)
        for s in self.sesiones:
            self.hermanas[s.seccion.id].append(s)
        self.dom = {s.id: self._dominio_inicial(s) for s in self.sesiones}
        self.ses = {s.id: s for s in self.sesiones}
        self.estado = Estado()
        self.nodos = self.retrocesos = 0
        self.culpables = Counter()
        self.deadline = t0 + self.limite
        self.agotado = False

        vacias = [s for s in self.sesiones if not self.dom[s.id]]
        if vacias:
            return Resultado("sin_solucion", segundos=time.perf_counter() - t0,
                             diagnostico=[f"La sesión {s.id} no tiene valores compatibles" for s in vacias])
        ok = self._buscar()
        seg = time.perf_counter() - t0
        if ok:
            asig = {sid: (f, a) for sid, (_, f, a) in self.estado.asignacion.items()}
            return Resultado("factible", asig, self.nodos, self.retrocesos, seg)
        estado = "limite_tiempo" if self.agotado else "sin_solucion"
        return Resultado(estado, {}, self.nodos, self.retrocesos, seg, self._explicar())

    # ------------------------------------------------------------ búsqueda
    def _dominio_inicial(self, s):
        dom = {}
        for f in self.inst.franjas:
            aulas = {a.id for a in self.inst.aulas
                     if all(r.permite(s, f, a, self.inst) for r in self.unarias)}
            if aulas:
                dom[f.id] = aulas
        return dom

    @staticmethod
    def _tam(d):
        return sum(len(v) for v in d.values())

    def _elegir(self, pendientes):
        if not self.mrv:
            return pendientes[0]
        return min(pendientes, key=lambda s: (self._tam(self.dom[s.id]), -len(self.hermanas[s.seccion.id])))

    def _costo_blando(self, s, f):
        costo = 0
        for h in self.hermanas[s.seccion.id]:
            if h.id in self.estado.asignacion:
                dh = self.estado.asignacion[h.id][1].dia
                if dh == f.dia:
                    costo += 10            # dos sesiones el mismo día
                elif abs(dh - f.dia) == 1:
                    costo += 3             # RB-01: días consecutivos
        pref = self.inst.docentes[s.docente].preferidas
        if pref and f.id not in pref:
            costo += 1                     # RB-02
        return costo

    def _valores(self, s):
        vals = [(fid, aid) for fid, aulas in self.dom[s.id].items() for aid in sorted(aulas)]
        if self.orden_blando:
            cap = s.seccion.estudiantes
            req = s.seccion.asignatura.requisitos
            vals.sort(key=lambda v: (self._costo_blando(s, self.franja[v[0]]),
                                     len(self.aula[v[1]].caracteristicas - req),   # no gastar laboratorios
                                     self.aula[v[1]].capacidad - cap, v))
        return vals

    def _consistente(self, s, f, a):
        return next((r for r in self.relacionales if not r.permite(s, f, a, self.estado)), None)

    def _buscar(self):
        pendientes = [s for s in self.sesiones if s.id not in self.estado.asignacion]
        if not pendientes:
            return True
        if time.perf_counter() > self.deadline:
            self.agotado = True
            return False
        s = self._elegir(pendientes)
        opciones = self._tam(self.dom[s.id])
        rechazos = Counter()
        for fid, aid in self._valores(s):
            f, a = self.franja[fid], self.aula[aid]
            if not self.fc:
                falla = self._consistente(s, f, a)
                if falla:
                    rechazos[(falla.id, falla.recurso(s))] += 1
                    continue
            self.nodos += 1
            self.estado.asignar(s, f, a)
            podas, ok = self._propagar(s, f) if self.fc else ([], True)
            if self.registrar_pasos:
                self.pasos.append({"tipo": "asigna", "sesion": s.id, "opciones": opciones,
                                   "pendientes": len(pendientes) - 1, "franja": fid, "aula": aid,
                                   "descartadas": len(podas), "ok": ok})
            if ok and self._buscar():
                return True
            for tid, pf, pa in podas:
                self.dom[tid].setdefault(pf, set()).add(pa)
            self.estado.desasignar(s)
            self.retrocesos += 1
            if self.registrar_pasos:
                self.pasos.append({"tipo": "retrocede", "sesion": s.id, "franja": fid, "aula": aid})
            if self.agotado:
                return False
        if rechazos:
            self.culpables.update(rechazos)
        return False

    def _propagar(self, s, f):
        """Forward checking por regla: cada regla poda solo sus franjas y sesiones relacionadas."""
        podas = []
        pendientes = [t for t in self.sesiones if t.id not in self.estado.asignacion]
        for r in self.relacionales:
            franjas = r.afecta(f, self.inst)
            for t in r.relacionadas(s, pendientes):
                dt = self.dom[t.id]
                for fid in franjas:
                    aulas = dt.get(fid)
                    if not aulas:
                        continue
                    ft = self.franja[fid]
                    for aid in list(aulas):
                        if not r.permite(t, ft, self.aula[aid], self.estado):
                            aulas.discard(aid)
                            podas.append((t.id, fid, aid))
                    if not aulas:
                        del dt[fid]
                if not dt:
                    self.culpables[(r.id, r.recurso(t))] += 1
                    for tid, pf, pa in podas:
                        self.dom[tid].setdefault(pf, set()).add(pa)
                    return [], False
        return podas, True

    def _explicar(self):
        if not self.culpables:
            return ["No se encontró un horario completo con los datos suministrados."]
        out = []
        for (rid, recurso), n in self.culpables.most_common(3):
            out.append(f"[{rid}] {recurso}: bloqueó {n} intentos de asignación")
        return out
