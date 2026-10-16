"""Validación de cambios manuales y cálculo de alternativas (RF-12, RF-13, RNF-02).

Sirve para comprobar el supuesto más débil del boceto: que el sistema puede
explicar el conflicto y ofrecer alternativas válidas de inmediato.
"""
from .estado import Estado
from .restricciones import RestriccionRelacional, RestriccionUnaria


class Editor:
    def __init__(self, instancia, asignacion, restricciones):
        self.inst = instancia
        self.restricciones = restricciones
        self.ses = {s.id: s for s in instancia.sesiones()}
        self.estado = Estado()
        for sid, (f, a) in asignacion.items():
            self.estado.asignar(self.ses[sid], f, a)

    def validar_movimiento(self, sesion_id, franja, aula):
        """Devuelve las restricciones duras que incumpliría mover la sesión."""
        s = self.ses[sesion_id]
        actual = self.estado.asignacion[sesion_id]
        self.estado.desasignar(s)
        try:
            fallas = []
            for r in self.restricciones:
                if isinstance(r, RestriccionUnaria) and not r.permite(s, franja, aula, self.inst):
                    fallas.append((r.id, r.descripcion, r.recurso(s)))
                elif isinstance(r, RestriccionRelacional) and not r.permite(s, franja, aula, self.estado):
                    fallas.append((r.id, r.descripcion, r.recurso(s)))
            return fallas
        finally:
            self.estado.asignar(s, actual[1], actual[2])

    def alternativas(self, sesion_id, maximo=5):
        """Alternativas válidas ordenadas: primero sin cambiar de aula y sin repetir día."""
        s = self.ses[sesion_id]
        _, f0, a0 = self.estado.asignacion[sesion_id]
        dias_hermanas = {f.dia for sid, (x, f, _) in self.estado.asignacion.items()
                         if x.seccion.id == s.seccion.id and sid != sesion_id}
        opciones = []
        for f in self.inst.franjas:
            for a in self.inst.aulas:
                if (f.id, a.id) == (f0.id, a0.id):
                    continue
                if not self.validar_movimiento(sesion_id, f, a):
                    costo = (f.dia in dias_hermanas) * 10 + (a.id != a0.id) * 2 + abs(f.dia - f0.dia)
                    opciones.append((costo, f, a))
        opciones.sort(key=lambda o: (o[0], o[1].id, o[2].id))
        return [(f, a) for _, f, a in opciones[:maximo]], len(opciones)
