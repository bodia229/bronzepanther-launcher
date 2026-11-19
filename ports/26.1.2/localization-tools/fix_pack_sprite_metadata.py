"""Repair metadata against the actual installed images, preserving their pixels."""
from pathlib import Path
import json,zipfile,struct

root=Path(__file__).resolve().parents[3]
draft=root/'work/localization-2026-11-19/draft'
packs=root/'work/draconic-port/tests/fullpack-client-run'
gravel=packs/'resourcepacks/Gravel GUI 1.2.4.zip'
fixed=[]
with zipfile.ZipFile(gravel) as z:
 for suffix in ['toast/tutorial','friends/toast_background','container/brewing_stand/bubbles','container/brewing_stand/brew_progress']:
  name='assets/minecraft/textures/gui/sprites/'+suffix+'.png'
  w,h=struct.unpack('>II',z.read(name)[16:24])
  if suffix.startswith(('toast/','friends/')):
   # Border must leave a nonempty center; retain the source's 16px corners horizontally.
   meta={'gui':{'scaling':{'type':'nine_slice','width':w,'height':h,'border':{'left':16,'right':16,'top':15,'bottom':15}}}}
  else:
   # These are progress sprites, not animation strips. Give the single frame its actual dimensions.
   meta={'animation':{'width':w,'height':h,'frames':[0]}}
  target=draft/(name+'.mcmeta');target.parent.mkdir(parents=True,exist_ok=True)
  target.write_text(json.dumps(meta),encoding='utf-8');fixed.append(name)
with zipfile.ZipFile(next((packs/'mods').glob('Powah-*.jar'))) as z:
 name='assets/powah/textures/item/ender_core.png'
 assert struct.unpack('>II',z.read(name)[16:24])==(16,16)
 # Upstream ships one frame but references frames 3,5,6,8. No invented replacement artwork.
 target=draft/(name+'.mcmeta');target.parent.mkdir(parents=True,exist_ok=True)
 target.write_text(json.dumps({'animation':{'width':16,'height':16,'frames':[0]}}),encoding='utf-8');fixed.append(name)
report={'source':'Installed Gravel GUI 1.2.4 and Powah 7.0.4-alpha','unchanged_image_pixels':True,'fixed_metadata':fixed}
(draft/'SOURCES/sprite-metadata-fixes.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
print('Repaired metadata for',len(fixed),'installed sprites; source image pixels unchanged')
