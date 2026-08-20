"""Shift Pha 1 (IDX cluster) right by DX in mermaid SVG output.

- Move IDX cluster rect, label
- Move Pha 1 nodes (U, T, CH, EM, DB)
- Move Pha 1 internal edges (L_U_T, L_T_CH, L_CH_EM, L_EM_DB)
- For cross-edge L_DB_RT: shift only x-coords > THRESHOLD (Pha 1 side)
- Shift edge label "truy vấn vector" by DX/2 to stay centered
- Update viewBox width
"""
import sys, re, io
sys.stdout.reconfigure(encoding="utf-8")

DX = 50.0          # SVG units to shift Pha 1 right (~100px at scale=2)
THRESHOLD = 300.0  # midline between Pha 2 (x<300) and Pha 1 (x>300)

PHA1_NODES = {"U", "T", "CH", "EM", "DB"}
PHA1_INTERNAL_EDGES = {"L_U_T_0", "L_T_CH_0", "L_CH_EM_0", "L_EM_DB_0"}
CROSS_EDGE = "L_DB_RT_0"

def shift_coords_in_path(d, dx, thresh=None):
    """Shift x of every (x,y) pair. If thresh given, only shift when x > thresh."""
    def repl(m):
        x, y = float(m.group(1)), float(m.group(2))
        if thresh is None or x > thresh:
            x = x + dx
        return f"{x},{y}"
    return re.sub(r"(-?\d+\.?\d*),(-?\d+\.?\d*)", repl, d)

def shift_translate(transform, dx):
    def repl(m):
        x = float(m.group(1)) + dx
        return f"translate({x}, {m.group(2)})"
    return re.sub(r"translate\(([\d.\-]+),\s*([\d.\-]+)\)", repl, transform)

def main(svg_in, svg_out):
    svg = open(svg_in, "r", encoding="utf-8").read()

    # 1) Update viewBox: width += DX
    def vb_repl(m):
        x, y, w, h = m.group(1).split()
        new_w = float(w) + DX
        return f'viewBox="{x} {y} {new_w} {h}"'
    svg = re.sub(r'viewBox="([^"]+)"', vb_repl, svg)

    # 2) Shift IDX cluster rect
    def idx_cluster_repl(m):
        block = m.group(0)
        # shift rect x
        block2 = re.sub(r'(<rect[^>]*x=")([\d.\-]+)(")',
                        lambda rm: f'{rm.group(1)}{float(rm.group(2))+DX}{rm.group(3)}',
                        block, count=1)
        return block2
    svg = re.sub(r'<g[^>]*class="cluster"[^>]*id="my-svg-IDX"[^>]*>.*?</g>',
                 idx_cluster_repl, svg, count=1, flags=re.DOTALL)

    # 3) Shift IDX cluster-label transform (the one at y=8 is Pha 1 title)
    # Mermaid puts cluster-label OUTSIDE the cluster group. Find it by y coord (8).
    def label_repl(m):
        tx, ty = float(m.group(1)), float(m.group(2))
        if abs(ty - 8.0) < 1.0:  # Pha 1 title at y=8
            tx += DX
        return f'<g class="cluster-label" transform="translate({tx}, {ty})"'
    svg = re.sub(r'<g class="cluster-label" transform="translate\(([\d.\-]+),\s*([\d.\-]+)\)"',
                 label_repl, svg)

    # 4) Shift Pha 1 nodes
    def node_repl(m):
        full = m.group(0)
        nid = m.group(1)
        # extract short id, e.g. "my-svg-flowchart-DB-7" -> "DB"
        short = nid.replace("my-svg-flowchart-", "").rsplit("-", 1)[0]
        if short in PHA1_NODES:
            full = re.sub(r'transform="translate\(([\d.\-]+),\s*([\d.\-]+)\)"',
                          lambda tm: f'transform="translate({float(tm.group(1))+DX}, {tm.group(2)})"',
                          full, count=1)
        return full
    svg = re.sub(r'<g class="node[^"]*" id="([^"]+)"[^>]*transform="translate\([\d.\-]+,\s*[\d.\-]+\)"',
                 node_repl, svg)

    # 5) Shift Pha 1 internal edge paths (full +DX) and cross-edge (only x>threshold +DX)
    def edge_repl(m):
        full = m.group(0)
        eid = m.group(1)
        short = eid.replace("my-svg-", "")
        d_match = re.search(r' d="([^"]+)"', full)
        if not d_match:
            return full
        d = d_match.group(1)
        if short in PHA1_INTERNAL_EDGES:
            d_new = shift_coords_in_path(d, DX)
        elif short == CROSS_EDGE:
            d_new = shift_coords_in_path(d, DX, THRESHOLD)
        else:
            return full
        return full.replace(f' d="{d}"', f' d="{d_new}"')
    svg = re.sub(r'<path[^>]*id="(my-svg-L_[^"]+)"[^>]*d="[^"]+"[^>]*/?>',
                 edge_repl, svg)

    # 6) Shift edge label "truy vấn vector". It's positioned at midpoint.
    # Move by DX/2 so it stays roughly centered between shifted DB and original RT.
    def edge_label_repl(m):
        # the edgeLabel transform — only the one at y~717 (the truy vấn vector label)
        tx, ty = float(m.group(1)), float(m.group(2))
        if 600 < ty < 800:  # heuristic: the cross-edge label
            tx += DX / 2
        return f'<g class="edgeLabel" transform="translate({tx}, {ty})"'
    svg = re.sub(r'<g class="edgeLabel" transform="translate\(([\d.\-]+),\s*([\d.\-]+)\)"',
                 edge_label_repl, svg)

    # Also: foreignObject inside edgeLabel may have x position; mermaid uses transform on inner span
    # Mermaid edge label HTML wrapper also gets translated. Look for span <g class="label" transform=
    # Usually under edgeLabel, the inner <g class="label" transform="translate(-w/2, -h/2)"> is relative
    # so should be fine.

    open(svg_out, "w", encoding="utf-8").write(svg)
    print(f"Wrote {svg_out} (DX={DX})")

if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
