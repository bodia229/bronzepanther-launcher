"""Update empty item stacks in upstream guide scenes, never live world data."""
from pathlib import Path
import base64,hashlib,json,re,urllib.request,zipfile

root=Path(__file__).resolve().parents[3]
task=root/'work/localization-2026-11-19';draft=task/'draft'
jar=next((root/'work/draconic-port/tests/fullpack-client-run/mods').glob('ExtendedAE-*.jar'))
license=task/'downloaded/ExtendedAE-LICENSE.txt'
if not license.exists():
 req=urllib.request.Request('https://api.github.com/repos/GlodBlock/ExtendedAE/license',headers={'Accept':'application/vnd.github+json','User-Agent':'BronzePanther-guide-compatibility'})
 with urllib.request.urlopen(req,timeout=30) as response:info=json.load(response)
 license.write_bytes(base64.b64decode(info['content']))
assert 'LESSER GENERAL PUBLIC LICENSE' in license.read_text(encoding='utf-8')
(draft/'LICENSES/ExtendedAE-LGPL-3.0.txt').write_bytes(license.read_bytes())
gpl=task/'downloaded/GPL-3.0.txt'
if not gpl.exists():
 with urllib.request.urlopen('https://www.gnu.org/licenses/gpl-3.0.txt',timeout=30) as response:gpl.write_bytes(response.read())
assert 'GNU GENERAL PUBLIC LICENSE' in gpl.read_text(encoding='utf-8')
(draft/'LICENSES/GPL-3.0.txt').write_bytes(gpl.read_bytes())
changes=[]
with zipfile.ZipFile(jar) as z:
 for name in ['caner_auto.snbt','caner_example.snbt']:
  path='assets/extendedae/ae2guide/structure/'+name
  original=z.read(path)
  text=original.decode('utf-8')
  # TileCaner stores ItemStack.OPTIONAL_CODEC. Empty stacks are encoded as {},
  # not a positive or wrapped-negative Count attached to the forbidden air item.
  fixed,count=re.subn(r'target:\s*\{Count:\s*-?\d+b,\s*id:\s*"minecraft:air"\}', 'target: {}',text)
  assert count==1,(name,count)
  target=draft/path;target.parent.mkdir(parents=True,exist_ok=True)
  target.write_text(fixed,encoding='utf-8')
  changes.append({'path':path,'original_sha256':hashlib.sha256(original).hexdigest(),'adapted_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'change':'Empty ItemStack OPTIONAL_CODEC representation only'})
(draft/'SOURCES/guide-scene-compatibility.json').write_text(json.dumps({'author':'GlodBlock and ExtendedAE contributors','source':'https://github.com/GlodBlock/ExtendedAE','installed_jar':jar.name,'license':'LGPL-3.0','changes':changes},indent=2),encoding='utf-8')
print('Adapted two ExtendedAE guide scenes; only empty-stack metadata changed; upstream LGPL license included.')
