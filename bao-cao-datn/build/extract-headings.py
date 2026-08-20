# -*- coding: utf-8 -*-
import zipfile, re, sys

path = sys.argv[1] if len(sys.argv) > 1 else "docs/bao-cao-mau-moi.docx"
xml = zipfile.ZipFile(path).read("word/document.xml").decode("utf-8", "ignore")
paras = re.findall(r"<w:p\b.*?</w:p>", xml, re.S)

def text(p):
    return "".join(re.findall(r"<w:t[^>]*>(.*?)</w:t>", p, re.S))

def style(p):
    m = re.search(r'<w:pStyle w:val="([^"]+)"', p)
    return m.group(1) if m else ""

count = 0
for p in paras:
    st = style(p)
    t = text(p).strip()
    if not t:
        continue
    is_heading = st.lower().startswith("heading") or "Ti" in st[:5]
    if is_heading:
        print("[%s] %s" % (st, t[:100]))
        count += 1
print("---- total headings: %d ----" % count)
