# Sistema de Programación de Horarios Académicos

Proyecto académico desarrollado para la gestión y generación de horarios académicos mediante una aplicación web.

## Descripción

El proyecto busca apoyar el proceso de programación de horarios académicos, considerando la disponibilidad de docentes, aulas, grupos y franjas horarias.

La aplicación permitirá generar propuestas de horarios y validar que las asignaciones realizadas cumplan con las restricciones definidas para el sistema.

## Objetivo

Desarrollar una solución que facilite la generación de horarios académicos y reduzca los conflictos que pueden presentarse durante su planificación manual.

## Alcance

El proyecto está enfocado en la programación de horarios para los semestres quinto, sexto y séptimo del programa de Ingeniería de Sistemas de la Universidad El Bosque.

El sistema contempla la gestión de la información necesaria para generar los horarios y la validación de las restricciones asociadas a docentes, aulas, grupos y franjas horarias.

## Estructura del repositorio

| Carpeta | Contenido |
|---|---|
| [`Proyecto_EntregaFinal/`](Proyecto_EntregaFinal/) | **MVP**: aplicación web (Java 17 + Spring Boot + H2 embebida, React). Ver [Proyecto_EntregaFinal/README.md](Proyecto_EntregaFinal/README.md) |
| [`prototipo/`](prototipo/) | Prototipo técnico de la Fase 2 en Python (experimentos E1 a E5, diagramas) |
| [`Documentacion/`](Documentacion/) | Entregables del proyecto (Entregable 1, Fase 1, Dossier de Pre-diseño) |

## Cómo ejecutar el MVP

Requiere **Java 17 o superior**. No hace falta instalar la base de datos ni Node.

```bash
cd Proyecto_EntregaFinal/backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

Luego abra **http://localhost:8080**. También se puede abrir `Proyecto_EntregaFinal/backend` en IntelliJ IDEA y ejecutar
`HorariosApplication`.

## Equipo

**Grupo 1 – Ingeniería de Sistemas**
Universidad El Bosque

* Laura Natalia Olmos Parra
* Laura Valentina Ospina Rengifo
* Laura Palacios Ochoa
* Felipe Mejía Alvarado

## Documentación

La documentación y los entregables del proyecto se encuentran disponibles dentro de este repositorio.
