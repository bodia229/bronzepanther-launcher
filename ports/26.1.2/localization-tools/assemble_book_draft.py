"""Adapt community translations to the installed GuideME resources; retain provenance."""
from pathlib import Path
import collections, hashlib, json, re
import yaml

ROOT = Path(__file__).resolve().parents[3] / 'work/localization-2026-11-19'
pages = json.loads((ROOT / 'book-selected.json').read_text(encoding='utf-8'))

def split(text):
    text = text.replace('\r\n', '\n')
    if text.startswith('---\n'):
        head, body = text[4:].split('\n---', 1)
        return yaml.safe_load(head), body.lstrip('\n')
    return {}, text

def insert_before_recipe(text, addition):
    marker = re.search(r'^## Рецепт', text, re.M)
    return text[:marker.start()] + addition + '\n\n' + text[marker.start():] if marker else text + '\n\n' + addition + '\n'

provenance = []
for p in pages:
    en = p['english'].replace('\r\n', '\n')
    ru = p['russian'].replace('\r\n', '\n')
    meta, _ = split(en)
    ru_meta, body = split(ru)
    # Category identifiers, item IDs, ordering and parent paths are machine data.
    # Copy current metadata while preserving translated display titles.
    if 'navigation' in meta and 'title' in ru_meta.get('navigation', {}):
        meta['navigation']['title'] = ru_meta['navigation']['title']
    for key in ('title', 'description'):
        if key in ru_meta:
            meta[key] = ru_meta[key]
    base = p['base']
    if '/ae2/ae2guide/' in base:
        body = re.sub(r'(\bid=["\'])chest(["\'])', r'\1me_chest\2', body)
    if base.endswith('aae_intro/quantum_armor.md'):
        # The current version adds a camouflage card to the utility group.
        anchor = '<ItemImage id="advanced_ae:magnet_card" scale="2"></ItemImage>'
        body = body.replace(anchor, anchor + '\n<ItemImage id="advanced_ae:camo_card" scale="2"></ItemImage>')
        anchor = '* <ItemLink id="advanced_ae:magnet_card" />'
        body = body.replace(anchor, anchor + '\n* <ItemLink id="advanced_ae:camo_card" />')
    if base == 'assets/ae2/ae2guide/items-blocks-machines/wireless_terminals.md':
        # AE2WTLib replaces the crafting-terminal documentation, not base AE2.
        body = re.split(r'^# Беспроводной терминал (?:создания|крафта)', body, maxsplit=1, flags=re.M)[0]
        body += '# Беспроводной терминал крафта\n\n<ItemImage id="wireless_crafting_terminal" scale="4" />\n\n'
        body += '[AE2WTLib](ae2wtlib:ae2wtlib/ae2wtlib-index.md) изменяет <ItemLink id="ae2:wireless_crafting_terminal" />.\n'
        body += 'Его возможности теперь описаны на [отдельной странице](ae2wtlib:ae2wtlib/wireless_crafting_terminal.md).\n'
    if base.endswith('ae2-mechanics/autocrafting.md'):
        # Preserve translated explanation; restore current visual and example link.
        body += '\n\n## Пример рекурсивного крафта\n\n<ItemImage id="minecraft:netherite_upgrade_smithing_template" scale="4" />\n\n'
        body += 'Рецепты дублирования кузнечных шаблонов относятся к рекурсивным: результат вновь нужен как ингредиент. '
        body += 'Для таких рецептов используйте [специальную схему](../example-setups/recursive-crafting-setup.md).\n'
    if base.endswith('ae2-mechanics/me-network-connections.md'):
        body += '\n\n<GameScene zoom="6" background="transparent">\n  <ImportStructure src="../assets/assemblies/subpart_no_connection.snbt" />\n  <IsometricCamera yaw="195" pitch="30" />\n</GameScene>\n'
    if base.endswith('items-blocks-machines/facades.md'):
        body = insert_before_recipe(body, '## Скрытие фасадов\n\nФасады скрываются, пока в любой руке находится <ItemLink id="network_tool" />. '
                                    'Это позволяет взаимодействовать с блоками за фасадами, не снимая их.')
    if base.endswith('items-blocks-machines/interface.md'):
        # Community prose already describes this interaction without a clickable item.
        body += '\n\n<ItemLink id="storage_bus" /> на ненастроенном интерфейсе предоставляет своей сети доступ ко всему '
        body += '[сетевому хранилищу](../ae2-mechanics/import-export-storage.md) сети интерфейса.\n'
    if base.endswith('items-blocks-machines/network_tool.md'):
        body = insert_before_recipe(body, '## Скрытие фасадов\n\n<ItemLink id="facade" /> скрывается, пока сетевой инструмент находится в любой руке. '
                                    'С блоками за скрытыми фасадами можно взаимодействовать, не снимая фасады.')
    if base.endswith('items-blocks-machines/sky_stone.md'):
        body = body.replace('Поместите плоскость уничтожения лицевой стороной вверх на максимальной высоте мира, чтобы получить пыль небесного камня.',
                            'Поместите <ItemLink id="annihilation_plane" /> рабочей стороной вверх на максимальной высоте строительства, чтобы получить <ItemLink id="sky_dust" />.')
    if base.endswith('epp_intro/upgrade_items.md'):
        marker = '<ItemImage id="extendedae:drive_upgrade" scale="4"></ItemImage>'
        body = body.replace(marker, marker + '\n<ItemImage id="extendedae:wireless_connector_upgrade" scale="4"></ItemImage>')
        body += '\n| <ItemImage id="extendedae:wireless_connector_upgrade" scale="3"></ItemImage> | <ItemImage id="extendedae:wireless_connect" scale="3"></ItemImage> | <ItemImage id="extendedae:wireless_hub" scale="3"></ItemImage> |\n'
    text = '---\n' + yaml.safe_dump(meta, allow_unicode=True, sort_keys=False).rstrip() + '\n---\n\n' + body
    dest = ROOT / 'draft' / p['target']
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(text, encoding='utf-8')
    provenance.append({'path': p['target'], 'source': p['source'], 'source_sha256': p['translation_sha256'],
                       'adapted_sha256': hashlib.sha256(text.encode()).hexdigest()})

# Missing author/community pages: translate prose in-place, keeping technical tags exact.
replacements = {
 'Functional Blocks': 'Функциональные блоки',
 'Generators': 'Генераторы',
 'Lunar and Hybrid Panel': 'Лунная и гибридная панели',
 'Storage and Transfer': 'Хранение и передача энергии',
 'Tiers and Crystals': 'Уровни и кристаллы',
 'Energizing Rods, Player Transmitters, Energy Hoppers and Energy Dischargers in the new tiers. Energizing Rods glow in the colour of their tier.':
 'Заряжающие стержни, передатчики энергии игроку, энергетические воронки и разрядники новых уровней. Заряжающие стержни светятся цветом своего уровня.',
 'Furnator, Magmator, Thermo Generator, Solar Panel and Reactor in the new tiers. They work exactly like their Powah counterparts, only stronger.':
 'Топливные генераторы, магматоры, термогенераторы, солнечные панели и реакторы новых уровней. Принцип работы тот же, что у соответствующих устройств Powah, но их характеристики выше.',
 "Higher reactor tiers also burn more fuel than a Nitro reactor: Voltaic 2×, Astral 4×, Quantum 8× (configurable as `reactor_fuel_multiplier`). The reactor screen still shows Powah's base consumption.":
 'Реакторы более высоких уровней расходуют больше топлива, чем реактор уровня «Нитро»: вольтовый — в 2 раза, астральный — в 4, квантовый — в 8. Множитель задаётся параметром `reactor_fuel_multiplier`. При этом в интерфейсе реактора по-прежнему указан базовый расход Powah.',
 'Advanced Powah! adds three tiers above Nitro: **Voltaic**, **Astral** and **Quantum**. Every tiered Powah block and item exists in these tiers, plus two new machines, the Lunar Panel and the Hybrid Panel.':
 'Advanced Powah! добавляет три уровня выше «Нитро»: **вольтовый**, **астральный** и **квантовый**. Все блоки и предметы Powah с системой уровней доступны и на этих уровнях. Также добавлены два новых устройства: лунная и гибридная панели.',
 'All values on these pages come from the current configuration (`config/advancedpowah-common.toml`).':
 'Все значения на этих страницах берутся из текущей конфигурации (`config/advancedpowah-common.toml`).',
 '- **Lunar Panel:** generates **1,200 FE/t** at night while it can see the sky.':
 '- **Лунная панель:** вырабатывает **1 200 FE/такт** ночью при открытом небе над ней.',
 '- **Hybrid Panel:** generates **2,000 FE/t** by day and by night while it can see the sky. Weather does not matter.':
 '- **Гибридная панель:** вырабатывает **2 000 FE/такт** и днём, и ночью при открытом небе над ней. Погода не влияет на работу.',
 'Both are connected from below, like a Solar Panel, and do not work in dimensions without a sky. Generation can be changed in the config (`machines.lunar_panel_generation`, `machines.hybrid_panel_generation`).':
 'Обе панели подключаются снизу, как солнечная панель, и не работают в измерениях без неба. Выработку можно изменить в конфигурации: `machines.lunar_panel_generation` и `machines.hybrid_panel_generation`.',
 "Energy Cells, Batteries, Ender Cells, Ender Gates and Energy Cables in the new tiers. Batteries fit into Curios slots like Powah's own.":
 'Энергоячейки, аккумуляторы, эндер-ячейки, эндер-врата и энергетические кабели новых уровней. Аккумуляторы можно устанавливать в слоты Curios, как и обычные аккумуляторы Powah.',
 'When you craft an Energy Cell, Battery or generator of a higher tier, the energy stored in the lower-tier ones is carried over (up to the new capacity).':
 'При создании энергоячейки, аккумулятора или генератора более высокого уровня накопленная в исходных устройствах энергия переносится в новое устройство, но не выше его ёмкости.',
 'Ender Cells and Ender Gates share energy per owner and channel, just like in Powah. Each tier has 12 channels.':
 'Эндер-ячейки и эндер-врата обмениваются энергией в пределах одного владельца и канала, как в Powah. На каждом уровне доступно 12 каналов.',
 'The new crystals are made in the Energizing Orb. Each tier needs the crystal block of the tier before it:':
 'Новые кристаллы создаются в заряжающей сфере. Для каждого уровня нужен кристаллический блок предыдущего уровня:',
 '- **Voltaic:** Nitro Crystal Block, Block of Diamond, Block of Lapis Lazuli, Nether Star → 8 crystals':
 '- **Вольтовый:** блок кристаллов «Нитро», алмазный блок, лазуритовый блок, звезда Незера → 8 кристаллов',
 "- **Astral:** Voltaic Crystal Block, Block of Netherite, Dragon's Breath, End Crystal → 4 crystals":
 '- **Астральный:** блок вольтовых кристаллов, незеритовый блок, драконье дыхание, кристалл Энда → 4 кристалла',
 '- **Quantum:** Astral Crystal Block, Block of Netherite, Echo Shard, Nether Star → 4 crystals':
 '- **Квантовый:** блок астральных кристаллов, незеритовый блок, осколок эха, звезда Незера → 4 кристалла',
 'Crystal blocks also work as heat sources for the Thermo Generator (Voltaic 3,500, Astral 4,500, Quantum 5,500).':
 'Кристаллические блоки также служат источниками тепла для термогенератора: вольтовый — 3 500, астральный — 4 500, квантовый — 5 500.',
 '| Capacity |': '| Ёмкость |', '| Generates |': '| Выработка |', '| Max I/O |': '| Макс. ввод/вывод |',
}
for p in json.loads((ROOT / 'book-missing.json').read_text(encoding='utf-8')):
    text = p['english']
    # Longer paragraphs before shorter headings, preserving exact identifiers/paths.
    for en, ru in sorted(replacements.items(), key=lambda item: -len(item[0])):
        text = text.replace(en, ru)
    dest = ROOT / 'draft' / p['target']
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(text, encoding='utf-8')
    provenance.append({'path': p['target'], 'source': 'BronzePanther translation of installed Advanced Powah 1.1.14',
                       'english_sha256': hashlib.sha256(p['english'].encode()).hexdigest(),
                       'adapted_sha256': hashlib.sha256(text.encode()).hexdigest()})
(ROOT / 'book-draft-provenance.json').write_text(json.dumps(provenance, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'pages': len(provenance), 'community': len(pages), 'new_translations': len(provenance)-len(pages)}))
