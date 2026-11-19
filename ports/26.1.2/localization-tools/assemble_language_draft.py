from pathlib import Path
import collections,json,re
root=Path(__file__).resolve().parents[3]/'work/localization-2026-11-19'
entries=json.loads((root/'language-manual-review.json').read_text(encoding='utf-8'))
patches=json.loads((root/'language-draft.json').read_text(encoding='utf-8'))
seen=set()
token=re.compile(r'%(?:%|n|(?:\d+\$)?[-+#0 ]*\d*(?:\.\d+)?[sSdDfFbBhHcCxXoOeEgGaA])')
def signature(s):return collections.Counter(t for t in token.findall(s) if t not in ['%%','%n'])
for file in sorted(root.glob('manual-*.txt')):
 for line in file.read_text(encoding='utf-8').splitlines():
  number,ru=line.split('|',1);index=int(number)
  if index in seen:raise ValueError('Duplicate translation index '+number)
  entry=entries[index];seen.add(index)
  # IPN uses literal backslash-n in its own tooltip templates; preserve that convention.
  if '\\n' not in entry['en']:ru=ru.replace('\\n','\n')
  if signature(entry['en'])!=signature(ru):raise ValueError('Format argument mismatch '+number)
  if re.findall('§.',entry['en'])!=re.findall('§.',ru):raise ValueError('Formatting code mismatch '+number)
  patches.setdefault(entry['namespace'],{})[entry['key']]=ru
assert len(seen)==len(entries),(len(seen),len(entries))
for ns,values in patches.items():
 if values:
  dest=root/'draft/assets'/ns/'lang/ru_ru.json';dest.parent.mkdir(parents=True,exist_ok=True)
  dest.write_text(json.dumps(values,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
result={'manual_keys':len(seen),'total_patch_keys':sum(map(len,patches.values())),'namespaces':{k:len(v) for k,v in patches.items() if v}}
(root/'language-patch-summary.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(result,ensure_ascii=False,indent=1))
