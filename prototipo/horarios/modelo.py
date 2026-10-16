"""Modelo de datos del prototipo técnico (Fase 1, U-02).

Las entidades siguen el vocabulario del Entregable M1:
asignatura -> secciones (grupos) -> sesiones semanales de 2 h.
"""
from __future__ import annotations

from dataclasses import dataclass, field

DIAS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"]


@dataclass(frozen=True)
class Franja:
    """Bloque institucional de 2 horas (RD-04, RD-05)."""
    id: int
    dia: int          # 0 = lunes ... 5 = sábado
    inicio: int       # hora de inicio (24 h)

    @property
    def fin(self) -> int:
        return self.inicio + 2

    def etiqueta(self) -> str:
        return f"{DIAS[self.dia]} {self.inicio:02d}-{self.fin:02d}"


@dataclass(frozen=True)
class Aula:
    id: str
    capacidad: int
    caracteristicas: frozenset = frozenset()


@dataclass
class Docente:
    id: str
    habilitadas: set            # códigos de asignatura que puede orientar (RD-08)
    disponibles: set            # ids de franja permitidas (RD-09)
    preferidas: set = field(default_factory=set)   # RB-02


@dataclass(frozen=True)
class Asignatura:
    codigo: str
    nombre: str
    semestre: int
    creditos: int
    sesiones_semanales: int     # RD-10 (1 a 4)
    requisitos: frozenset = frozenset()   # RD-07


@dataclass(frozen=True)
class Seccion:
    """Una sección es la oferta de una asignatura para un grupo (cohorte)."""
    id: str
    asignatura: Asignatura
    grupo: str                  # cohorte que no puede tener cruces (RD-03), p. ej. "5A"
    estudiantes: int
    docente: str                # relación docente-sección recibida como dato (Tabla 5 del M1)


@dataclass(frozen=True)
class Sesion:
    """Unidad básica de asignación: una sesión semanal de una sección."""
    id: str
    seccion: Seccion
    indice: int

    @property
    def docente(self) -> str:
        return self.seccion.docente

    @property
    def grupo(self) -> str:
        return self.seccion.grupo


@dataclass
class Parametros:
    """Valores institucionales configurables (RD-12)."""
    tamano_objetivo: int = 40
    tolerancia: float = 0.10
    minimo_estudiantes: int = 10          # RF-14
    sesiones_min: int = 1
    sesiones_max: int = 4

    @property
    def tamano_maximo(self) -> int:
        return int(self.tamano_objetivo * (1 + self.tolerancia))


@dataclass
class Instancia:
    nombre: str
    franjas: list
    aulas: list
    docentes: dict              # id -> Docente
    secciones: list
    parametros: Parametros = field(default_factory=Parametros)

    def sesiones(self) -> list:
        out = []
        for sec in self.secciones:
            for i in range(sec.asignatura.sesiones_semanales):
                out.append(Sesion(f"{sec.id}#{i + 1}", sec, i + 1))
        return out


def franjas_institucionales() -> list:
    """Franjas de 2 h sin cruzar el receso de 12:00 a 13:00 (RD-04).

    Lunes a viernes: 07, 09, 13, 15, 17, 19 (la franja 21-22 no alcanza 2 h).
    Sábado: 07, 09 (la jornada termina a la 01:00 p. m.).
    """
    franjas, k = [], 0
    for dia in range(5):
        for h in (7, 9, 13, 15, 17, 19):
            franjas.append(Franja(k, dia, h)); k += 1
    for h in (7, 9):
        franjas.append(Franja(k, 5, h)); k += 1
    return franjas
