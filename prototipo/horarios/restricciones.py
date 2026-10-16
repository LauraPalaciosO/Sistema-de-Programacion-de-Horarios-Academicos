"""Restricciones duras con una interfaz común (U-04, RNF-06).

- RestriccionUnaria: depende solo de la sesión y del valor (franja, aula).
  Se aplica una vez para construir los dominios iniciales.
- RestriccionRelacional: depende de otras sesiones ya asignadas.
  La consulta el motor de búsqueda durante la poda (forward checking)
  y la consulta también la validación de cambios manuales (RF-13).

Agregar una regla = agregar una clase y registrarla en registro.py.
El motor y el estado no conocen reglas concretas.
"""
from __future__ import annotations

from abc import ABC, abstractmethod


class Restriccion(ABC):
    id: str = ""
    descripcion: str = ""

    def recurso(self, sesion) -> str:
        """Recurso involucrado, usado para explicar conflictos (RF-11)."""
        return sesion.id


class RestriccionUnaria(Restriccion):
    @abstractmethod
    def permite(self, sesion, franja, aula, instancia) -> bool: ...


class RestriccionRelacional(Restriccion):
    @abstractmethod
    def permite(self, sesion, franja, aula, estado) -> bool: ...

    def afecta(self, franja, instancia):
        """Franjas de otras sesiones que pueden quedar inválidas al ocupar `franja`.

        Por defecto solo la misma franja (cruces). Una regla con mayor alcance
        lo declara aquí, y el motor poda esas franjas sin conocer la regla.
        """
        return (franja.id,)

    def relacionadas(self, sesion, pendientes):
        """Sesiones pendientes que esta regla puede afectar al asignar `sesion`.

        Por defecto todas; una regla que solo relaciona ciertos recursos lo restringe
        para que la poda no revise sesiones que no comparten nada con la asignada.
        """
        return pendientes


# ---------------------------------------------------------------- unarias
class FranjaInstitucional(RestriccionUnaria):
    id, descripcion = "RD-04", "La sesión debe ubicarse en una franja institucional"

    def permite(self, sesion, franja, aula, instancia):
        return franja in instancia.franjas


class CapacidadAula(RestriccionUnaria):
    id, descripcion = "RD-06", "El aula debe tener capacidad suficiente"

    def permite(self, sesion, franja, aula, instancia):
        return aula.capacidad >= sesion.seccion.estudiantes

    def recurso(self, sesion):
        return f"sección {sesion.seccion.id} ({sesion.seccion.estudiantes} est.)"


class CaracteristicasAula(RestriccionUnaria):
    id, descripcion = "RD-07", "El aula debe tener las características requeridas"

    def permite(self, sesion, franja, aula, instancia):
        return sesion.seccion.asignatura.requisitos <= aula.caracteristicas

    def recurso(self, sesion):
        req = ", ".join(sorted(sesion.seccion.asignatura.requisitos)) or "—"
        return f"aulas con {req}"


class DisponibilidadDocente(RestriccionUnaria):
    id, descripcion = "RD-09", "El docente debe estar disponible en la franja"

    def permite(self, sesion, franja, aula, instancia):
        return franja.id in instancia.docentes[sesion.docente].disponibles

    def recurso(self, sesion):
        return f"docente {sesion.docente}"


# ------------------------------------------------------------ relacionales
class AulaLibre(RestriccionRelacional):
    id, descripcion = "RD-01", "Un aula no puede tener dos sesiones simultáneas"

    def permite(self, sesion, franja, aula, estado):
        return all(a.id != aula.id for _, a in estado.en_franja(franja.id))

    def relacionadas(self, sesion, pendientes):
        return pendientes          # cualquier sesión puede querer la misma aula

    def recurso(self, sesion):
        return "aulas"


class DocenteLibre(RestriccionRelacional):
    id, descripcion = "RD-02", "Un docente no puede tener dos sesiones simultáneas"

    def permite(self, sesion, franja, aula, estado):
        return all(s.docente != sesion.docente for s, _ in estado.en_franja(franja.id))

    def relacionadas(self, sesion, pendientes):
        return [t for t in pendientes if t.docente == sesion.docente]

    def recurso(self, sesion):
        return f"docente {sesion.docente}"


class GrupoLibre(RestriccionRelacional):
    id, descripcion = "RD-03", "Un grupo no puede tener dos asignaturas simultáneas"

    def permite(self, sesion, franja, aula, estado):
        return all(s.grupo != sesion.grupo for s, _ in estado.en_franja(franja.id))

    def relacionadas(self, sesion, pendientes):
        return [t for t in pendientes if t.grupo == sesion.grupo]

    def recurso(self, sesion):
        return f"grupo {sesion.grupo}"
