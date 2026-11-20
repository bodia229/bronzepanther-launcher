from pathlib import Path
import hashlib,json,re

ROOT=Path(__file__).resolve().parents[3]/'work/localization-2026-11-19'
draft=ROOT/'draft/assets/ae2/ae2guide/_ru_ru'
aliases={
 'items-blocks-machines/crafting_cpu_multiblock.md':{'Хранилище для изготовления':'crafting-storage'},
 'items-blocks-machines/cables.md':{'Плотный кабель':'dense-cable','Умный кабель':'smart-cable'},
 'ae2-mechanics/channels.md':{'Маршрутизация каналов':'channel-routing'},
 'ae2-mechanics/import-export-storage.md':{'Приоритет хранения':'storage-priority'},
 'example-setups/pipe-subnet.md':{'Предоставление в несколько мест':'providing-to-multiple-places'},
 'items-blocks-machines/terminals.md':{'Терминал шаблонов':'pattern-encoding-terminal'},
 'items-blocks-machines/interface.md':{'Специальные взаимодействия':'special-interactions'},
 'items-blocks-machines/annihilation_plane.md':{'Рецепт':'recipe'},
 'items-blocks-machines/entropy_manipulator.md':{'Рецепт':'recipe'},
 'items-blocks-machines/guide.md':{'Рецепт':'recipe'},
}
for file in (ROOT/'draft/assets').rglob('*.md'):
 text=file.read_text(encoding='utf-8')
 replacements={'автобусами':'шинами','автобусам':'шинам','автобусах':'шинах','автобусов':'шин','автобусы':'шины','автобусу':'шине','автобусом':'шиной','автобуса':'шины','автобусе':'шине','автобус':'шина'}
 for en,ru in replacements.items():
  text=re.sub(r'\b'+en+r'\b',lambda m:ru.capitalize() if m[0][0].isupper() else ru,text,flags=re.I)
 text=text.replace('## Модернизации','## Улучшения')
 text=re.sub(r'^(##) Recipe$',r'\1 Рецепт',text,flags=re.M)
 if file==draft/'items-blocks-machines/export_bus.md':
  text=text.replace('Шина импорта поддерживает','Шина экспорта поддерживает').replace('<RecipeFor id="import_bus" />','<RecipeFor id="export_bus" />')
  if '## Скорость работы' not in text:
   addition='''## Скорость работы

| Карты ускорения | Предметов за одну операцию |
|:---------------|:--------------------------|
| 0 | 1 |
| 1 | 8 |
| 2 | 32 |
| 3 | 64 |
| 4 | 96 |

'''
   text=text.replace('## Рецепт\n',addition+'## Рецепт\n',1)
 if file==draft/'items-blocks-machines/terminals.md':
  if '# Поиск в терминале' not in text:
   addition='''# Поиск в терминале

Поле поиска в терминале поддерживает регулярные выражения (*regex*). Например, выражение "gtceu:.*ore" находит все руды GregTech. Для составления собственных запросов потребуется знание синтаксиса регулярных выражений.

'''
   text=text.replace('# Терминал\n',addition+'# Терминал\n',1)
  text=text.replace('Терминал изготовления','Терминал крафта')
  text=text.replace('"gtceu:.*ore" находит все руды GregTech','"minecraft:.*ore" находит руды Minecraft по идентификатору')
 if file==draft/'items-blocks-machines/vibration_chamber.md':
  text=text.replace('## Config','## Конфигурация')
  if '## Рецепт' not in text:text=text.replace('<RecipeFor id="vibration_chamber" />','## Рецепт\n\n<RecipeFor id="vibration_chamber" />')
 file.write_text(text,encoding='utf-8')
for relative,pairs in aliases.items():
 file=draft/relative;text=file.read_text(encoding='utf-8')
 for heading,anchor in pairs.items():
  if '<a name="'+anchor+'"' in text:continue
  pattern=r'^(#+) '+re.escape(heading)+r'$'
  text,count=re.subn(pattern,lambda m:m[1]+' <a name="'+anchor+'" />'+heading,text,flags=re.M)
  assert count==1,(relative,heading,count)
 file.write_text(text,encoding='utf-8')
provenance=json.loads((ROOT/'book-draft-provenance.json').read_text(encoding='utf-8'))
for entry in provenance:
 entry['adapted_sha256']=hashlib.sha256((ROOT/'draft'/entry['path']).read_bytes()).hexdigest()
(ROOT/'book-draft-provenance.json').write_text(json.dumps(provenance,ensure_ascii=False,indent=2),encoding='utf-8')
(ROOT/'guide-anchor-checks.json').write_text(json.dumps(aliases,ensure_ascii=False,indent=2),encoding='utf-8')
print('Polished recipe headings and installed-pack search example; preserved',sum(map(len,aliases.values())),'legacy heading anchors.')
