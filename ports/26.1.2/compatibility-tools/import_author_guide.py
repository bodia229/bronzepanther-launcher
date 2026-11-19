"""Import PI-Docs verbatim and adapt its markup, never generate documentation prose."""
import concurrent.futures,hashlib,html,io,json,re,struct,subprocess,urllib.request,zipfile
from pathlib import Path
tests=Path(__file__).resolve().parent
assets=tests.parent/'Draconic-Evolution/src/main/resources/assets/draconicevolution'
archive=tests/'pi-author-docs.zip'
z=zipfile.ZipFile(archive)
prefix='PI-Docs-master/ModDocs/draconicevolution/3.0.0/'
structure=json.loads(z.read(prefix+'structure/structure.json'))
pages=[];images={};originals=[]
previous_path=assets/'guide/author-guide.json'
previous=json.loads(previous_path.read_text(encoding='utf-8')).get('images',{}) if previous_path.exists() else {}
out=assets/'guide/author';out.mkdir(parents=True,exist_ok=True)

def image_size(data):
 if data.startswith(b'\x89PNG'):return struct.unpack('>II',data[16:24])
 if data.startswith(b'\xff\xd8'):
  stream=io.BytesIO(data);stream.read(2)
  while True:
   marker=stream.read(2)
   if len(marker)<2:break
   while marker[1]==255:marker=bytes([255])+stream.read(1)
   size=struct.unpack('>H',stream.read(2))[0]
   if marker[1] in [192,193,194,195,197,198,199,201,202,203,205,206,207]:
    _,height,width=struct.unpack('>BHH',stream.read(5));return width,height
   stream.seek(size-2,1)
 raise ValueError('Unsupported author image format')

def plain(text):
 text=re.sub(r'§k[^§\n]*','',text)
 text=re.sub(r'§(?:align|colour|color|shadow|size):[^\s]+','',text)
 text=re.sub(r'§(?:table|rule|vSpacer|hSpacer)\{[^}]*\}','\n',text)
 text=re.sub(r'§[0-9a-fk-or]','',text,flags=re.I)
 text=re.sub(r'</tr\s*>','\n',text,flags=re.I)
 text=re.sub(r'</td\s*>',' | ',text,flags=re.I)
 text=re.sub(r'</?(?:table|tr|td|span|div)[^>]*>','',text,flags=re.I)
 text=re.sub(r'^\s*#{1,6}\s*','',text,flags=re.M)
 text=re.sub(r'^\s*//[^\n]*','',text,flags=re.M)
 return html.unescape(text).strip()

def balanced(text,start,opening,closing):
 depth=0;quoted=False;escaped=False
 for index in range(start,len(text)):
  ch=text[index]
  if escaped:escaped=False;continue
  if ch=='\\' and quoted:escaped=True;continue
  if ch=='"':quoted=not quoted;continue
  if quoted:continue
  if ch==opening:depth+=1
  elif ch==closing:
   depth-=1
   if depth==0:return index
 raise ValueError('Unclosed PI documentation markup '+repr(text[start:start+180]))

def legacy_item(value):
 parts=value.split(',',3);item=parts[0].strip()
 try:meta=int(parts[2]) if len(parts)>2 else 0
 except ValueError:meta=0
 name=item.removeprefix('draconicevolution:')
 aliases={'draconium_ore':'overworld_draconium_ore','draconic_ingot':'awakened_draconium_ingot','draconic_block':'awakened_draconium_block','draconic_core':'draconium_core','fusion_crafting_core':'crafting_core','energy_storage_core':'energy_core','dislocator_advanced':'advanced_dislocator','item_dislocation_inhibitor':'dislocation_inhibitor','draconic_spawner':'stabilized_spawner','wyvern_pick':'wyvern_pickaxe','draconic_pick':'draconic_pickaxe','draconic_staff_of_power':'draconic_staff'}
 if item.startswith('draconicevolution:'):
  aliases['diss_enchanter']='disenchanter'
  if name=='nugget':name='awakened_draconium_nugget' if meta==1 else 'draconium_nugget'
  elif name=='reactor_component':name='reactor_injector' if meta==1 else 'reactor_stabilizer'
  elif name=='reactor_part':name=['reactor_prt_stab_frame','reactor_prt_in_rotor','reactor_prt_out_rotor','reactor_prt_rotor_full','reactor_prt_focus_ring'][min(meta,4)]
  elif name=='crafting_injector':name=['basic_crafting_injector','wyvern_crafting_injector','awakened_crafting_injector','chaotic_crafting_injector'][min(meta,3)]
  elif name=='energy_crystal':name=['basic','wyvern','draconic'][meta%3]+'_'+['relay','io','wireless'][min(meta//3,2)]+'_crystal'
  elif name=='flow_gate':name='fluid_gate' if meta&8 else 'flux_gate'
  elif name=='dislocator_bound':name=['player_dislocator','p2p_dislocator_unbound','p2p_dislocator'][min(meta,2)]
  elif name=='draconium_capacitor':name=['wyvern_capacitor','draconic_capacitor','creative_op_capacitor'][min(meta,2)]
  else:name=aliases.get(name,name)
  item='draconicevolution:'+name
 return item

def blocks(text):
 # The archive contains a few mistyped closing delimiters in display options.
 text=re.sub(r'\{((?:spacing|size|width|height):[0-9%]+)\]',r'{\1}',text)
 result=[];pattern=re.compile(r'§(img|stack|recipe|link|entity)\[')
 start=0
 for match in pattern.finditer(text):
  if match.start()<start:continue
  before=plain(text[start:match.start()])
  if before:result.append({'type':'text','text':before})
  kind=match[1];end=balanced(text,match.end()-1,'[',']');value=text[match.end():end]
  finish=end+1;options=''
  if finish<len(text) and text[finish]=='{':
   option_end=balanced(text,finish,'{','}');options=text[finish+1:option_end];finish=option_end+1
  if kind=='img':
   key=hashlib.sha256(value.encode()).hexdigest()[:20]
   images.setdefault(value,{'key':key})
   result.append({'type':'image','key':key,'source':value})
  elif kind=='link':
   label=re.search(r'alt_text\s*:\s*"([^"]*)"',options)
   result.append({'type':'link','target':value,'text':plain(label[1]) if label else value})
  elif kind=='entity':
   held=re.search(r'main_hand\s*:\s*"([^"]+)"',options)
   if held:result.append({'type':'stack','item':legacy_item(held[1]),'source_markup':text[match.start():finish]})
  else:
   item=legacy_item(value)
   block={'type':kind,'item':item,'source_item':value}
   soul=re.search(r'EntityName\s*:\s*"([^"]+)"',value)
   if soul:block['soul_entity']='draconicevolution:random_display_entity' if soul[1]=='[random]' else soul[1]
   result.append(block)
  start=finish
 after=plain(text[start:])
 if after:result.append({'type':'text','text':after})
 return result

langs={loc:{entry['page_uri']:entry['translation'] for entry in json.loads(z.read(prefix+'structure/lang/'+loc+'.json'))} for loc in ['ru_ru','en_us']}
def walk(node,parent='',depth=0):
 nodeid=node.get('id','');uri='draconicevolution:'+('/'.join(filter(None,[parent,nodeid])))
 if node is structure:uri='draconicevolution:'
 file=node.get('file')
 if file and nodeid!='test_page':
  page={'id':uri,'depth':depth,'file':file,'hidden':node.get('hidden',False),'locales':{}}
  for loc in ['ru_ru','en_us']:
   candidates=[prefix+loc+'/'+file,prefix+'structure/'+loc+'/'+file]
   found=next((p for p in candidates if p in z.namelist()),None)
   if found:
    raw=z.read(found);target=out/loc/file;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
    originals.append({'path':loc+'/'+file,'sha256':hashlib.sha256(raw).hexdigest(),'bytes':len(raw)})
    page['locales'][loc]={'title':langs[loc].get(uri,nodeid.replace('_',' ').title()) or 'Draconic Evolution','blocks':blocks(raw.decode('utf-8-sig'))}
  if page['locales']:pages.append(page)
 for child in sorted(node.get('pages',[]),key=lambda p:p.get('sorting_weight',0)):
  walk(child,'/'.join(filter(None,[parent,nodeid])),depth+1)
walk(structure)

def fetch_image(pair):
 url,meta=pair
 cached=previous.get(meta['key'])
 if cached and 'texture' in cached:
  local=assets/cached['texture'].split(':',1)[1]
  if local.exists() and hashlib.sha256(local.read_bytes()).hexdigest()==cached['sha256']:return meta['key'],cached
 request=urllib.request.Request(url.replace('http://','https://'),headers={'User-Agent':'BronzePanther-author-docs-import'})
 try:
  raw=urllib.request.urlopen(request,timeout=25).read();width,height=image_size(raw)
  suffix='.png' if raw.startswith(b'\x89PNG') else '.jpg'
  relative='textures/guide/author/'+meta['key']+suffix
  target=assets/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(raw)
  return meta['key'],{'texture':'draconicevolution:'+relative,'width':width,'height':height,'source':url,'sha256':hashlib.sha256(raw).hexdigest()}
 except Exception as exc:return meta['key'],{'source':url,'error':str(exc)}
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:image_data=dict(pool.map(fetch_image,images.items()))
if any(v.get('texture','').endswith('.jpg') for v in image_data.values()):
 java=Path.home()/'AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin/java.exe'
 subprocess.run([str(java),str(tests/'ConvertGuideImages.java'),str(assets/'textures/guide/author')],check=True)
 for image in image_data.values():
  if image.get('texture','').endswith('.jpg'):
   image['original_sha256']=image['sha256']
   image['texture']=image['texture'][:-4]+'.png'
   image['sha256']=hashlib.sha256((assets/image['texture'].split(':',1)[1]).read_bytes()).hexdigest()
   image['format_adaptation']='Original JPEG pixels decoded to PNG for NativeImage compatibility'
manifest={'source':'https://github.com/Draconic-Inc/PI-Docs/tree/master/ModDocs/draconicevolution/3.0.0','documentation_version':'3.0.0','source_archive_sha256':hashlib.sha256(archive.read_bytes()).hexdigest(),'original_files':originals,'pages':pages,'images':image_data}
from guide_ru_supplement import apply_supplement
apply_supplement(manifest)
legacy_armor={'wyvern_helm':('Шлем виверны','Wyvern Helmet'),'wyvern_chest':('Нагрудник виверны','Wyvern Chestplate'),'wyvern_legs':('Поножи виверны','Wyvern Leggings'),'wyvern_boots':('Ботинки виверны','Wyvern Boots'),'draconic_helm':('Дракониевый шлем','Draconic Helmet'),'draconic_chest':('Дракониевый нагрудник','Draconic Chestplate'),'draconic_legs':('Дракониевые поножи','Draconic Leggings'),'draconic_boots':('Дракониевые ботинки','Draconic Boots')}
legacy_armor.update({'energy_infuser':('Зарядник энергии (архив)','Energy Infuser (archive)'),'ender_energy_manipulator':('Манипулятор энергией Края (архив)','Ender Energy Manipulator (archive)')})
for page in pages:
 archived=any(part in page['id'] for part in [':wyvern_armor',':draconic_armor',':wyvern_tools',':draconic_tools',':ender_energy_manipulator',':rain_sensor'])
 page['archived_equipment']=archived
 for locale,content in page['locales'].items():
  if archived:content['title']=('Архив: ' if locale=='ru_ru' else 'Archive: ')+content['title']
  for block in content['blocks']:
   name=block.get('item','').removeprefix('draconicevolution:')
   if name in legacy_armor:
    block['archived_item']=True;block['display_name']=legacy_armor[name][0 if locale=='ru_ru' else 1]
titles={locale:{page['id']:page['locales'].get(locale,page['locales']['en_us'])['title'] for page in pages} for locale in ['ru_ru','en_us']}
for page in pages:
 for locale,content in page['locales'].items():
  for block in content['blocks']:
   if block['type']=='link' and block['text']==block['target'] and block['target'] in titles[locale]:block['text']=titles[locale][block['target']]
(assets/'guide/author-guide.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Author guide:',len(pages),'visible sections;',len(originals),'verbatim source files;',len(image_data),'images;',sum('error' in v for v in image_data.values()),'image failures')
