---
navigation:
  title: Лунная и гибридная панели
  parent: index.md
  icon: advancedpowah:hybrid_panel
  position: 4
item_ids:
  - advancedpowah:lunar_panel
  - advancedpowah:hybrid_panel
---

# Лунная и гибридная панели

- **Лунная панель:** вырабатывает **1 200 FE/такт** ночью при открытом небе над ней.
- **Гибридная панель:** вырабатывает **2 000 FE/такт** и днём, и ночью при открытом небе над ней. Погода не влияет на работу.

Обе панели подключаются снизу, как солнечная панель, и не работают в измерениях без неба. Выработку можно изменить в конфигурации: `machines.lunar_panel_generation` и `machines.hybrid_panel_generation`.

| | Ёмкость | Макс. ввод/вывод |
|---|---|---|
| <ItemLink id="advancedpowah:lunar_panel" /> | <powah:EnergyCapacity id="advancedpowah:lunar_panel" /> | <powah:EnergyMaxIO id="advancedpowah:lunar_panel" /> |
| <ItemLink id="advancedpowah:hybrid_panel" /> | <powah:EnergyCapacity id="advancedpowah:hybrid_panel" /> | <powah:EnergyMaxIO id="advancedpowah:hybrid_panel" /> |

<Row>
<RecipesFor id="advancedpowah:lunar_panel" />
<RecipesFor id="advancedpowah:hybrid_panel" />
</Row>
