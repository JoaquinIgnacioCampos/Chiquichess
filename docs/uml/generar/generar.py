"""Genera el diagrama de clases completo a partir de los diagramas por tema de docs/uml.

Uso (desde cualquier carpeta):
    python docs/uml/generar/generar.py

1. Lee los bloques Mermaid de docs/uml/0*.md: clases, estereotipos y relaciones.
2. Busca en src/main/java el paquete de cada clase (si falta alguna, avisa y no genera nada).
3. Escribe docs/uml/uml-completo.dot (formato Graphviz).
4. Escribe docs/uml/generar/dibujar.html: al abrirla en el navegador dibuja el diagrama y tiene
   botones para descargar uml-completo.svg y uml-completo.png. Esos dos archivos van en docs/uml/.
"""
import glob
import json
import os
import re
import sys

AQUI = os.path.dirname(os.path.abspath(__file__))
UML = os.path.dirname(AQUI)
REPO = os.path.dirname(os.path.dirname(UML))
FUENTES = os.path.join(REPO, 'src', 'main', 'java')

# Tipos declarados dentro de otro archivo (EventoDePartida.java).
ANIDADAS = {n: 'Nucleo.juego' for n in
            ['JugadaHecha', 'JugadaDeshecha', 'JugadaRehecha', 'PartidaFinalizada', 'Rasgo']}

FLECHA = r'(<\|--|<\|\.\.|\*--|o--|-->|\.\.>|\.\.\|>|<-->)'
RELACION = re.compile(r'^\s*(\w+)\s*(?:"[^"]*"\s*)?' + FLECHA + r'\s*(?:"[^"]*"\s*)?(\w+)\s*(?::\s*(.*))?$')
CLASE = re.compile(r'^\s*class (\w+)')
ESTEREOTIPO = re.compile(r'<<(interface|sealed interface|record|enumeration|abstract|utility|JavaFX Application)>>')

# Notación UML de cada flecha de Mermaid. dir=back deja arriba al padre o al contenedor.
ESTILO = {
    '<|--': 'dir=back arrowtail=empty',
    '<|..': 'dir=back arrowtail=empty style=dashed',
    '*--': 'dir=back arrowtail=diamond',
    'o--': 'dir=back arrowtail=odiamond',
    '-->': 'arrowhead=vee',
    '..>': 'arrowhead=vee style=dashed',
    '..|>': 'arrowhead=empty style=dashed',
    '<-->': 'dir=both arrowhead=vee arrowtail=vee',
}
COLORES = {'adaptadores': ('#FFF4E0', '#D99A2B'), 'Nucleo': ('#E8F0FB', '#4A78B5')}


def paquetes_del_codigo():
    paquete = dict(ANIDADAS)
    for archivo in glob.glob(os.path.join(FUENTES, '**', '*.java'), recursive=True):
        relativo = os.path.relpath(archivo, FUENTES).replace(os.sep, '/')
        paquete[os.path.basename(archivo)[:-5]] = relativo.rsplit('/', 1)[0].replace('/', '.')
    return paquete


def leer_diagramas():
    clases, estereotipos, relaciones, vistas = [], {}, [], set()
    for archivo in sorted(glob.glob(os.path.join(UML, '0*.md'))):
        texto = open(archivo, encoding='utf-8').read()
        for bloque in re.findall(r'```mermaid\n(.*?)```', texto, re.S):
            if not bloque.lstrip().startswith('classDiagram'):
                continue
            actual = None
            for linea in bloque.splitlines():
                m = CLASE.match(linea)
                if m:
                    if m.group(1) not in clases:
                        clases.append(m.group(1))
                    actual = m.group(1) if linea.rstrip().endswith('{') else None
                    continue
                e = ESTEREOTIPO.search(linea)
                if e and actual:
                    estereotipos.setdefault(actual, e.group(1))
                if linea.strip() == '}':
                    actual = None
                r = RELACION.match(linea)
                if r and actual is None:
                    a, flecha, b, etiqueta = r.groups()
                    if (a, flecha, b) in vistas:
                        continue
                    vistas.add((a, flecha, b))
                    for c in (a, b):
                        if c not in clases:
                            clases.append(c)
                    relaciones.append((a, flecha, b, etiqueta.strip() if etiqueta else None))
    return clases, estereotipos, relaciones


def escapar(texto):
    return texto.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def armar_dot(clases, estereotipos, relaciones, paquete):
    grupos = {}
    for c in clases:
        grupos.setdefault(paquete[c], []).append(c)
    salida = [
        'digraph UML {',
        '  graph [rankdir=TB newrank=true nodesep=0.25 ranksep=0.7 fontname="Helvetica" '
        'label=<<B>ChiquiChess: diagrama de clases completo</B><BR/><FONT POINT-SIZE="12">Generado a partir de '
        'docs/uml (sin atributos ni métodos: ver cada diagrama por tema)</FONT>> labelloc=t fontsize=22];',
        '  node [shape=box style="rounded,filled" fillcolor=white fontname="Helvetica" fontsize=11 margin="0.12,0.05"];',
        '  edge [fontname="Helvetica" fontsize=9 color="#555555" arrowsize=0.8];',
    ]
    for nombre in sorted(grupos):
        fondo, borde = COLORES['adaptadores' if nombre.startswith('adaptadores') else 'Nucleo']
        salida.append(f'  subgraph cluster_{nombre.replace(".", "_")} {{')
        salida.append(f'    label=<<B>{escapar(nombre)}</B>>; labeljust=l; fontsize=14; style="rounded,filled"; '
                      f'fillcolor="{fondo}"; color="{borde}"; penwidth=1.5;')
        for c in grupos[nombre]:
            if c in estereotipos:
                etiqueta = f'<<FONT POINT-SIZE="8"><I>«{escapar(estereotipos[c])}»</I></FONT><BR/><B>{c}</B>>'
            else:
                etiqueta = f'<<B>{c}</B>>'
            salida.append(f'    {c} [label={etiqueta}];')
        salida.append('  }')
    for a, flecha, b, etiqueta in relaciones:
        extra = f' xlabel=<{escapar(etiqueta)}>' if etiqueta else ''
        salida.append(f'  {a} -> {b} [{ESTILO[flecha]}{extra}];')
    salida.append('}')
    return '\n'.join(salida) + '\n', len(grupos)


PAGINA = '''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><title>UML completo</title>
<style>
  body {{ margin: 0; font-family: Helvetica, Arial, sans-serif; background: #fff; }}
  header {{ position: sticky; top: 0; padding: 10px 16px; background: #f4f4f4; border-bottom: 1px solid #ccc; }}
  button {{ margin-right: 8px; padding: 6px 12px; font-size: 14px; }}
  #estado {{ margin-left: 8px; color: #555; }}
</style></head>
<body>
<header>
  <button id="svg" disabled>Descargar uml-completo.svg</button>
  <button id="png" disabled>Descargar uml-completo.png</button>
  <span id="estado">Dibujando…</span>
</header>
<div id="diagrama"></div>
<!-- Archivo generado por generar.py: no editar a mano. -->
<script src="https://cdn.jsdelivr.net/npm/@viz-js/viz@3.11.0/lib/viz-standalone.js"></script>
<script>
const DOT = {dot};
function descargar(nombre, url) {{
  const a = document.createElement('a'); a.href = url; a.download = nombre; a.click();
}}
Viz.instance().then(viz => {{
  const svg = viz.renderSVGElement(DOT);
  document.getElementById('diagrama').appendChild(svg);
  const texto = '<?xml version="1.0" encoding="UTF-8"?>\\n' + new XMLSerializer().serializeToString(svg);
  document.getElementById('svg').onclick = () =>
    descargar('uml-completo.svg', URL.createObjectURL(new Blob([texto], {{type: 'image/svg+xml'}})));
  document.getElementById('png').onclick = async () => {{
    const img = new Image();
    img.src = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(texto);
    await img.decode();
    const escala = 1.5 * 4 / 3;  // de puntos a píxeles, con 1,5 de aumento
    const lienzo = document.createElement('canvas');
    lienzo.width = Math.round(parseFloat(svg.getAttribute('width')) * escala);
    lienzo.height = Math.round(parseFloat(svg.getAttribute('height')) * escala);
    const ctx = lienzo.getContext('2d');
    ctx.fillStyle = '#fff'; ctx.fillRect(0, 0, lienzo.width, lienzo.height);
    ctx.drawImage(img, 0, 0, lienzo.width, lienzo.height);
    lienzo.toBlob(b => descargar('uml-completo.png', URL.createObjectURL(b)), 'image/png');
  }};
  document.getElementById('svg').disabled = false;
  document.getElementById('png').disabled = false;
  document.getElementById('estado').textContent = {resumen};
}}).catch(e => {{ document.getElementById('estado').textContent = 'Error: ' + e.message; }});
</script>
</body></html>
'''


def main():
    paquete = paquetes_del_codigo()
    clases, estereotipos, relaciones = leer_diagramas()
    faltan = [c for c in clases if c not in paquete]
    if faltan:
        sys.exit('Estas clases de los diagramas no existen en src/main/java: ' + ', '.join(faltan))
    dot, cantidad_de_paquetes = armar_dot(clases, estereotipos, relaciones, paquete)
    with open(os.path.join(UML, 'uml-completo.dot'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(dot)
    resumen = f'{len(clases)} clases, {len(relaciones)} relaciones, {cantidad_de_paquetes} paquetes'
    with open(os.path.join(AQUI, 'dibujar.html'), 'w', encoding='utf-8', newline='\n') as f:
        f.write(PAGINA.format(dot=json.dumps(dot), resumen=json.dumps(resumen)))
    print(resumen)
    print('Ahora abrí docs/uml/generar/dibujar.html y guardá el SVG y el PNG en docs/uml/.')


if __name__ == '__main__':
    main()
