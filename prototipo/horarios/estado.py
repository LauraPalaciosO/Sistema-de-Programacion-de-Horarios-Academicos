"""Estado parcial de un horario. No conoce ninguna regla concreta."""
from collections import defaultdict


class Estado:
    def __init__(self):
        self.asignacion = {}                    # sesion.id -> (sesion, franja, aula)
        self._por_franja = defaultdict(list)    # franja.id -> [(sesion, aula)]
        self._por_dia = defaultdict(list)       # dia -> [(sesion, franja, aula)]

    def asignar(self, sesion, franja, aula):
        self.asignacion[sesion.id] = (sesion, franja, aula)
        self._por_franja[franja.id].append((sesion, aula))
        self._por_dia[franja.dia].append((sesion, franja, aula))

    def desasignar(self, sesion):
        _, franja, aula = self.asignacion.pop(sesion.id)
        self._por_franja[franja.id].remove((sesion, aula))
        self._por_dia[franja.dia].remove((sesion, franja, aula))

    # Consultas genéricas que usan las restricciones
    def en_franja(self, franja_id):
        return self._por_franja.get(franja_id, ())

    def en_dia(self, dia):
        return self._por_dia.get(dia, ())

    def copia_sin(self, sesion):
        """Estado sin una sesión: sirve para validar un movimiento manual."""
        e = Estado()
        for sid, (s, f, a) in self.asignacion.items():
            if sid != sesion.id:
                e.asignar(s, f, a)
        return e
