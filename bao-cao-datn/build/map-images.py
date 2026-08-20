# -*- coding: utf-8 -*-
import zipfile, re, sys
z = zipfile.ZipFile(sys.argv[1] if len(sys.argv) > 1 else "docs/bao-cao-mau-moi.docx")
xml = z.read("word/document.xml").decode("utf-8", "ignore")
rels = z.read("word/_rels/document.xml.rels").decode("utf-8", "ignore")
rid2tgt = dict(re.findall(r'Id="([^"]+)"[^>]*Target="([^"]+)"', rels))

paras = re.findall(r"<w:p\b.*?</w:p>", xml, re.S)
def text(p): return "".join(re.findall(r"<w:t[^>]*>(.*?)</w:t>", p, re.S))
def style(p):
    m = re.search(r'<w:pStyle w:val="([^"]+)"', p); return m.group(1) if m else ""

cur = ""
for p in paras:
    st = style(p); t = text(p).strip()
    if st.lower().startswith("heading") and t:
        cur = t[:70]
    blips = re.findall(r'<a:blip r:embed="([^"]+)"', p)
    blips += re.findall(r'r:embed="([^"]+)"', p)
    for b in set(blips):
        tgt = rid2tgt.get(b, b)
        print("%-60s | %s" % (cur, tgt))
