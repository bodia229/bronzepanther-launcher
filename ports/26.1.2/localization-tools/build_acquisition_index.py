"""Index actual frozen-pack loot/worldgen data. This is display data, never gameplay data."""
from pathlib import Path
import collections,hashlib,json,re,zipfile

ROOT=Path(__file__).resolve().parents[3]
TASK=ROOT/'work/localization-2026-11-19'
game=Path.home()/'AppData/Roaming/BronzePanther Launcher/instances/techmagic-rpg/minecraft'
vanilla=ROOT/'work/launcher/.test-data/engine/runtime/libraries/com/mojang/minecraft/26.1.2/minecraft-26.1.2-client.jar'
tables={};configured={};placed={};origins={}
for jar in [vanilla,*sorted((game/'mods').glob('*.jar'))]:
 with zipfile.ZipFile(jar) as z:
  for name in z.namelist():
   m=re.fullmatch(r'data/([^/]+)/(loot_table|worldgen/configured_feature|worldgen/placed_feature)/(.+)\.json',name)
   if not m:continue
   ns,kind,key=m.groups();identifier=ns+':'+key
   try:data=json.loads(z.read(name))
   except (ValueError,UnicodeError):continue
   {'loot_table':tables,'worldgen/configured_feature':configured,'worldgen/placed_feature':placed}[kind][identifier]=data
   origins[kind+':'+identifier]=jar.name

def items(node,ancestors=(),visited=frozenset()):
 if not isinstance(node,dict):return
 conditions=ancestors+tuple(node.get('conditions',[]))
 kind=node.get('type')
 if kind=='minecraft:item':
  yield node.get('name'),conditions,node.get('functions',[])
 if kind=='minecraft:loot_table':
  ref=node.get('value',node.get('name'))
  if isinstance(ref,str) and ref not in visited:
   yield from items(tables.get(ref,{}),conditions,visited|{ref})
  elif isinstance(ref,dict):yield from items(ref,conditions,visited)
 for field in ['pools','entries','children']:
  for child in node.get(field,[]):yield from items(child,conditions,visited)

def condition_notes(condition):
 if not isinstance(condition,dict):return []
 kind=condition.get('condition','').removeprefix('minecraft:')
 if kind=='killed_by_player':return ['Требуется убийство игроком.']
 if kind=='random_chance':return ['Случайное условие: '+format(condition.get('chance',0)*100,'.4g')+' %.']
 if kind=='random_chance_with_enchanted_bonus':return ['Вероятность зависит от зачарования «Добыча».']
 if kind in ['inverted','any_of','all_of']:
  if kind=='inverted':
   nested=condition.get('term',{});text=json.dumps(nested)
   if 'silk_touch' in text:return ['Без «Шёлкового касания».']
   return []
  return list(dict.fromkeys(note for term in condition.get('terms',[]) for note in condition_notes(term)))
 text=json.dumps(condition)
 if kind=='match_tool' and 'silk_touch' in text:return ['Требуется «Шёлковое касание».']
 if kind=='entity_properties':
  predicate=condition.get('predicate',{});flags=predicate.get('flags',{});notes=[]
  if flags.get('is_on_fire') is True:notes.append('Моб должен гореть в момент смерти.')
  if flags.get('is_baby') is False:notes.append('Только со взрослого моба.')
  color=predicate.get('components',{}).get('minecraft:sheep/color')
  colors={'white':'белый','orange':'оранжевый','magenta':'пурпурный','light_blue':'голубой','yellow':'жёлтый','lime':'лаймовый','pink':'розовый','gray':'серый','light_gray':'светло-серый','cyan':'бирюзовый','purple':'фиолетовый','blue':'синий','brown':'коричневый','green':'зелёный','red':'красный','black':'чёрный'}
  if color in colors:notes.append('Цвет шерсти овцы: '+colors[color]+'.')
  if predicate.get('type_specific',{}).get('sheared') is False:notes.append('Овца не должна быть острижена.')
  return notes
 if kind=='killed_by':return ['Требуется особый способ убийства.']
 return []

ore_features=collections.defaultdict(set)
for id,data in configured.items():
 if data.get('type') not in ['minecraft:ore','minecraft:scattered_ore']:continue
 for target in data.get('config',{}).get('targets',[]):
  block=target.get('state',{}).get('Name')
  if block:ore_features[id].add(block)

def vertical(anchor):
 if isinstance(anchor,int):return str(anchor)
 if not isinstance(anchor,dict):return '?'
 if 'absolute' in anchor:return str(anchor['absolute'])
 if 'above_bottom' in anchor:return str(anchor['above_bottom'])+' от дна измерения'
 if 'below_top' in anchor:return str(anchor['below_top'])+' ниже верха измерения'
 return '?'

ore_notes=collections.defaultdict(set)
for id,data in placed.items():
 feature=data.get('feature');blocks=ore_features.get(feature,set()) if isinstance(feature,str) else set()
 for placement in data.get('placement',[]):
  if placement.get('type')!='minecraft:height_range':continue
  height=placement.get('height',{});minimum=height.get('min_inclusive');maximum=height.get('max_inclusive')
  if minimum is None and maximum is None:
   text='Высота генерации Y: '+vertical(height)
  else:text='Высота генерации Y: '+vertical(minimum)+' … '+vertical(maximum)
  for block in blocks:ore_notes[block].add(text)

entries=[];ambiguous=[]
chest_labels=json.loads((TASK/'chest-source-labels.json').read_text(encoding='utf-8'))
for id,table in sorted(tables.items()):
 ns,path=id.split(':',1)
 if path.startswith('entities/'):kind='mob';source=ns+':'+path[len('entities/'):]
 elif path.startswith('chests/'):kind='chest';source=id
 elif path.startswith('blocks/'):kind='block';source=ns+':'+path[len('blocks/'):]
 elif path.startswith('gameplay/fishing'):kind='fishing';source=id
 elif path=='gameplay/piglin_bartering':kind='bartering';source=id
 elif path in ['gameplay/sniffer_digging','gameplay/cat_morning_gift','gameplay/chicken_lay','gameplay/armadillo_shed','gameplay/panda_sneeze','gameplay/turtle_grow'] or path.startswith('gameplay/hero_of_the_village/'):
  kind='special';source=id
 else:continue
 by_item=collections.defaultdict(lambda:{'notes':set(),'variants':0})
 for item,conditions,functions in items(table,visited=frozenset({id})):
  if not isinstance(item,str):continue
  if kind=='block' and item==source and source not in ore_notes:continue
  components={}
  for function in functions:
   if function.get('function')=='minecraft:set_potion' and isinstance(function.get('id'),str):
    components['minecraft:potion_contents']={'potion':function['id']}
  variant=json.dumps(components,sort_keys=True,separators=(',',':')) if components else ''
  record=by_item[(item,variant)];record['variants']+=1
  for c in conditions:record['notes'].update(condition_notes(c))
  for f in functions:
   if f.get('function')=='minecraft:looting_enchant':record['notes'].add('Количество может увеличиваться с зачарованием «Добыча».')
   if f.get('function')=='minecraft:enchanted_count_increase':record['notes'].add('Количество зависит от зачарования.')
   if f.get('function')=='minecraft:furnace_smelt':
    cooked={'minecraft:beef':'minecraft:cooked_beef','minecraft:porkchop':'minecraft:cooked_porkchop','minecraft:chicken':'minecraft:cooked_chicken','minecraft:mutton':'minecraft:cooked_mutton','minecraft:rabbit':'minecraft:cooked_rabbit','minecraft:cod':'minecraft:cooked_cod','minecraft:salmon':'minecraft:cooked_salmon'}.get(item)
    if cooked:
     cooked_record=by_item[(cooked,'')];cooked_record['variants']+=1
     cooked_record['notes'].add('Моб горит при смерти или оружие выплавляет добычу.')
  if any(c.get('condition','').startswith('neoforge:') for c in conditions):ambiguous.append([id,item])
 for (item,variant),record in sorted(by_item.items()):
  notes=sorted(record['notes'])
  # Alternatives in one source can have mutually exclusive conditions (e.g. burning).
  # List them as alternatives, never combine their predicates into a fake requirement.
  if record['variants']>1 and notes:notes=['В таблице есть несколько вариантов выпадения.']+['Вариант: '+n for n in notes]
  if kind=='block':notes+=sorted(ore_notes.get(source,[]))
  entry={'kind':kind,'source':source,'item':item,'table':id,'notes':notes}
  if variant:
   entry['components']=json.loads(variant)
   entry['variant']=hashlib.sha256(variant.encode()).hexdigest()[:12]
  if kind=='chest':
   label_key=path.removeprefix('chests/').removeprefix('ic2_')
   if ns in ('industrial','minecraft') and label_key.startswith('village_'):label_key='village/'+label_key
   if ns=='tf_dnv':label_key='tf_dnv/'+label_key
   assert label_key in chest_labels,(id,label_key)
   entry['label']=chest_labels[label_key]
  entries.append(entry)

dest=TASK/'draft/assets/bronzepanther/emi/acquisition.json';dest.parent.mkdir(parents=True,exist_ok=True)
dest.write_text(json.dumps({'schema':1,'entries':entries},ensure_ascii=False,separators=(',',':')),encoding='utf-8')
report={'loot_tables':len(tables),'entries':len(entries),'by_kind':dict(collections.Counter(e['kind'] for e in entries)),
        'ore_blocks_with_heights':len(ore_notes),'custom_conditions_review':ambiguous}
(TASK/'acquisition-index-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False)[:1500])
