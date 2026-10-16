"""Prueba de cambio (sección F): la Facultad vuelve obligatoria la preferencia
de no programar una asignatura en días consecutivos.

Regla nueva: las sesiones de una misma sección no pueden quedar el mismo día ni en
días consecutivos cuando la asignatura tiene 2 o 3 sesiones semanales.
"""
from .restricciones import RestriccionRelacional


class DiasNoConsecutivos(RestriccionRelacional):
    id = "DIAS-NO-CONSECUTIVOS"
    descripcion = "Las sesiones de una sección no pueden quedar el mismo día ni en días consecutivos"

    def permite(self, sesion, franja, aula, estado):
        if sesion.seccion.asignatura.sesiones_semanales not in (2, 3):
            return True
        for dia in (franja.dia - 1, franja.dia, franja.dia + 1):
            for otra, _, _ in estado.en_dia(dia):
                if otra.seccion.id == sesion.seccion.id and otra.id != sesion.id:
                    return False
        return True

    def afecta(self, franja, instancia):
        return tuple(f.id for f in instancia.franjas if abs(f.dia - franja.dia) <= 1)

    def relacionadas(self, sesion, pendientes):
        return [t for t in pendientes if t.seccion.id == sesion.seccion.id]

    def recurso(self, sesion):
        return f"sección {sesion.seccion.id}"
