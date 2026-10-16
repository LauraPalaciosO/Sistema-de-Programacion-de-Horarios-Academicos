"""Registro de restricciones activas. Es el único punto que conoce las reglas concretas."""
from .restricciones import (AulaLibre, DocenteLibre, GrupoLibre,
                            FranjaInstitucional, CapacidadAula,
                            CaracteristicasAula, DisponibilidadDocente)


def restricciones_duras():
    return [
        FranjaInstitucional(), CapacidadAula(), CaracteristicasAula(),
        DisponibilidadDocente(),
        AulaLibre(), DocenteLibre(), GrupoLibre(),
    ]
