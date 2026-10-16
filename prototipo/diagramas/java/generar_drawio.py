"""Genera entidades_java.drawio (diagrama de entidades para abrir y editar en draw.io / diagrams.net)."""
from xml.sax.saxutils import escape

ROW = 22          # alto de cada atributo o método
HEAD = 44         # alto del encabezado (estereotipo + nombre)
SEP = 8           # alto de la línea separadora

ENT = "fillColor=#dae8fc;strokeColor=#6c8ebf;"
ENUM = "fillColor=#e1d5e7;strokeColor=#9673a6;"
MEM = "fillColor=#f5f5f5;strokeColor=#666666;dashed=1;"

# nombre: (x, y, ancho, estereotipo, atributos, métodos, estilo)
CLASES = {
    "Horario": (470, 40, 250, "«@Entity»", [
        "- id : Long", "- periodo : String", "- generadoEn : LocalDateTime", "- estado : EstadoHorario"], [], ENT),
    "EstadoHorario": (800, 60, 170, "«enumeration»", ["BORRADOR", "PUBLICADO"], [], ENUM),
    "ParametrosInstitucionales": (1060, 40, 270, "«@Entity»", [
        "- id : Long", "- tamanoObjetivo : int = 40", "- tolerancia : double = 0.10",
        "- minimoEstudiantes : int = 10", "- sesionesMin : int = 1", "- sesionesMax : int = 4"],
        ["+ tamanoMaximo() : int"], ENT),
    "Asignacion": (470, 300, 250, "«@Entity»", ["- id : Long", "- indiceSesion : int"], [], ENT),
    "Franja": (60, 300, 230, "«@Entity»", [
        "- id : Long", "- dia : DiaSemana", "- inicio : LocalTime", "- fin : LocalTime"], [], ENT),
    "DiaSemana": (90, 40, 170, "«enumeration»", [
        "LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO"], [], ENUM),
    "Seccion": (400, 530, 270, "«@Entity»", [
        "- id : Long", "- codigo : String", "- grupo : String", "- estudiantes : int"],
        ["+ generarSesiones() : List<Sesion>"], ENT),
    "Aula": (760, 530, 290, "«@Entity»", [
        "- id : Long", "- codigo : String", "- capacidad : int", "- caracteristicas : Set<String>"],
        ["+ cumple(requisitos : Set<String>) : boolean"], ENT),
    "Docente": (40, 820, 300, "«@Entity»", ["- id : Long", "- nombre : String"],
        ["+ puedeDictar(a : Asignatura) : boolean", "+ estaDisponible(f : Franja) : boolean"], ENT),
    "Sesion": (760, 820, 210, "«no persistente»", ["- seccion : Seccion", "- indice : int"], [], MEM),
    "Asignatura": (400, 1060, 270, "«@Entity»", [
        "- id : Long", "- codigo : String", "- nombre : String", "- semestre : int", "- creditos : int",
        "- sesionesSemanales : int", "- requisitosAula : Set<String>"], [], ENT),
}

NOTAS = [  # id, x, y, w, h, texto, clase a la que apunta
    ("nota_param", 1060, 290, 270, 70,
     "Valores configurables por la institución (tope de estudiantes, tolerancia y sesiones por semana).",
     "ParametrosInstitucionales"),
    ("nota_asig", 800, 340, 250, 80,
     "Una asignación ubica una clase semanal (la sesión número indiceSesion de una sección) en una franja y un aula.",
     "Asignacion"),
    ("nota_sesion", 1020, 820, 230, 80,
     "Se crea en memoria para el motor a partir de cada sección; no se guarda en la base de datos.",
     "Sesion"),
]

ASOC = "endArrow=open;endFill=0;endSize=10;html=1;rounded=0;edgeStyle=orthogonalEdgeStyle;fontSize=11;"
# (origen, destino, etiqueta, mult. origen, mult. destino, estilo extra, salida, entrada)
RELACIONES = [
    ("Horario", "Asignacion", "contiene", "1", "0..*",
     "startArrow=diamondThin;startFill=1;startSize=14;endArrow=none;", "exitX=0.5;exitY=1;", "entryX=0.5;entryY=0;"),
    ("Horario", "EstadoHorario", "", "", "", "", "exitX=1;exitY=0.5;", "entryX=0;entryY=0.5;"),
    ("Asignacion", "Franja", "", "0..*", "1", "", "exitX=0;exitY=0.5;", "entryX=1;entryY=0.5;"),
    ("Asignacion", "Seccion", "", "0..*", "1", "", "exitX=0.35;exitY=1;", "entryX=0.4;entryY=0;"),
    ("Asignacion", "Aula", "", "0..*", "1", "", "exitX=0.75;exitY=1;", "entryX=0.25;entryY=0;"),
    ("Franja", "DiaSemana", "", "", "", "", "exitX=0.5;exitY=0;", "entryX=0.5;entryY=1;"),
    ("Seccion", "Docente", "dictada por", "0..*", "1", "", "exitX=0;exitY=0.85;", "entryX=1;entryY=0.3;"),
    ("Seccion", "Asignatura", "pertenece a", "0..*", "1", "", "exitX=0.5;exitY=1;", "entryX=0.5;entryY=0;"),
    ("Seccion", "Sesion", "genera", "1", "1..4", "dashed=1;", "exitX=1;exitY=0.85;", "entryX=0;entryY=0.5;"),
    ("Docente", "Asignatura", "habilitado para", "0..*", "1..*", "", "exitX=0.5;exitY=1;", "entryX=0;entryY=0.5;"),
    ("Docente", "Franja", "disponible en", "0..*", "0..*", "", "exitX=0.75;exitY=0;", "entryX=0.8;entryY=1;"),
    ("Docente", "Franja", "prefiere", "0..*", "0..*", "", "exitX=0.15;exitY=0;", "entryX=0.15;entryY=1;"),
]


def alto(nombre):
    _, _, _, _, attrs, mets, _ = CLASES[nombre]
    return HEAD + ROW * len(attrs) + (SEP + ROW * len(mets) if mets else 0)


def celdas():
    out, nid = [], 0
    for nombre, (x, y, w, est, attrs, mets, color) in CLASES.items():
        h = alto(nombre)
        out.append(f'<mxCell id="{nombre}" value="{escape(f"<i>{est}</i><br><b>{nombre}</b>")}" '
                   f'style="swimlane;fontStyle=0;align=center;verticalAlign=top;childLayout=stackLayout;horizontal=1;'
                   f'startSize={HEAD};horizontalStack=0;resizeParent=1;resizeParentMax=0;resizeLast=0;collapsible=0;'
                   f'marginBottom=0;html=1;whiteSpace=wrap;fontSize=12;{color}" vertex="1" parent="1">'
                   f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')
        yy = HEAD
        filas = [("a", t) for t in attrs] + ([("sep", "")] if mets else []) + [("m", t) for t in mets]
        for tipo, texto in filas:
            nid += 1
            if tipo == "sep":
                out.append(f'<mxCell id="r{nid}" value="" style="line;strokeWidth=1;fillColor=none;align=left;'
                           f'verticalAlign=middle;spacingTop=-1;spacingLeft=3;spacingRight=3;rotatable=0;'
                           f'labelPosition=right;points=[];portConstraint=eastwest;strokeColor=inherit;" '
                           f'vertex="1" parent="{nombre}"><mxGeometry y="{yy}" width="{w}" height="{SEP}" as="geometry"/></mxCell>')
                yy += SEP
                continue
            out.append(f'<mxCell id="r{nid}" value="{escape(texto)}" style="text;strokeColor=none;fillColor=none;'
                       f'align=left;verticalAlign=middle;spacingLeft=6;spacingRight=4;overflow=hidden;rotatable=0;'
                       f'points=[[0,0.5],[1,0.5]];portConstraint=eastwest;whiteSpace=wrap;html=1;fontSize=12;" '
                       f'vertex="1" parent="{nombre}"><mxGeometry y="{yy}" width="{w}" height="{ROW}" as="geometry"/></mxCell>')
            yy += ROW
    for i, (o, d, etq, mo, md, extra, sal, ent) in enumerate(RELACIONES, 1):
        estilo = ASOC + extra + sal + ent
        out.append(f'<mxCell id="e{i}" value="{escape(etq)}" style="{estilo}" edge="1" parent="1" '
                   f'source="{o}" target="{d}"><mxGeometry relative="1" as="geometry"/></mxCell>')
        for lado, mult in ((-1, mo), (1, md)):
            if mult:
                out.append(f'<mxCell id="e{i}{"s" if lado < 0 else "t"}" value="{mult}" '
                           f'style="edgeLabel;resizable=0;html=1;align={"left" if lado < 0 else "right"};'
                           f'verticalAlign=bottom;fontSize=11;" connectable="0" vertex="1" parent="e{i}">'
                           f'<mxGeometry x="{lado}" relative="1" as="geometry">'
                           f'<mxPoint x="{6 if lado < 0 else -6}" y="-4" as="offset"/></mxGeometry></mxCell>')
    for nid_, x, y, w, h, texto, clase in NOTAS:
        out.append(f'<mxCell id="{nid_}" value="{escape(texto)}" style="shape=note;whiteSpace=wrap;html=1;size=14;'
                   f'fillColor=#fff2cc;strokeColor=#d6b656;align=left;verticalAlign=middle;spacingLeft=8;'
                   f'spacingRight=8;fontSize=11;" vertex="1" parent="1">'
                   f'<mxGeometry x="{x}" y="{y}" width="{w}" height="{h}" as="geometry"/></mxCell>')
        out.append(f'<mxCell id="{nid_}_c" style="endArrow=none;dashed=1;html=1;strokeColor=#999999;" edge="1" '
                   f'parent="1" source="{nid_}" target="{clase}"><mxGeometry relative="1" as="geometry"/></mxCell>')
    titulo = "Entidades del sistema (paquete co.edu.unbosque.horarios.model)"
    out.append(f'<mxCell id="titulo" value="{escape(titulo)}" style="text;html=1;fontSize=16;fontStyle=1;'
               f'align=center;verticalAlign=middle;" vertex="1" parent="1">'
               f'<mxGeometry x="300" y="-10" width="700" height="30" as="geometry"/></mxCell>')
    return "\n        ".join(out)


xml = f'''<mxfile host="app.diagrams.net" type="device">
  <diagram id="entidades" name="Entidades">
    <mxGraphModel dx="1400" dy="900" grid="1" gridSize="10" guides="1" tooltips="1" connect="1" arrows="1" fold="1" page="1" pageScale="1" pageWidth="1400" pageHeight="1300" math="0" shadow="0">
      <root>
        <mxCell id="0"/>
        <mxCell id="1" parent="0"/>
        {celdas()}
      </root>
    </mxGraphModel>
  </diagram>
</mxfile>
'''
if __name__ == "__main__":
    import pathlib
    p = pathlib.Path(__file__).with_name("entidades_java.drawio")
    p.write_text(xml, encoding="utf-8")
    print("Generado", p.name)
