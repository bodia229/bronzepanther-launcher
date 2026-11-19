from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[3]/'work/localization-2026-11-19'
translations={
 'Enable Effects':'Включить эффекты','Quantum Armor':'Квантовая броня','Quantum Computer':'Квантовый компьютер',
 'Attack Speed Boost':'Прибавка к скорости атаки','Evasion Chance':'Шанс уклонения','HP Buffer Hearts':'Дополнительное здоровье в сердцах',
 'Luck Boost':'Прибавка к удаче','Maximum Flight Speed':'Максимальная скорость полёта','Maximum Jump Height':'Максимальная высота прыжка',
 'Maximum Magnet Range':'Максимальный радиус магнита','Maximum Reach Boost':'Максимальное увеличение дальности взаимодействия',
 'Maximum Sprint Speed':'Максимальная скорость бега','Maximum Step Height':'Максимальная высота подъёма без прыжка',
 'Maximum Walk Speed':'Максимальная скорость ходьбы','Percentage Damage Absorption':'Доля поглощаемого урона',
 'Regeneration Per Tick':'Восстановление здоровья за такт','Strength Boost':'Прибавка к силе','Swim Speed Boost':'Прибавка к скорости плавания',
 'Data Entangler Multiplication':'Множитель запутывателя данных','Maximum Multi-Threaders':'Максимум многопоточных блоков',
 'Multi-Threader Multiplication':'Множитель многопоточного блока','Maximum Data Entanglers':'Максимум запутывателей данных',
 'Allowed Sides':'Разрешённые стороны','Apply to all':'Применить ко всем','Quantum Crafter T.':'Терм. квант. сборщиков',
 'Style Config':'Настройки оформления','Quantum Crafter Terminal':'Терминал квантовых сборщиков',
 'Filtered Import':'Фильтрация ввода','Input is unrestricted':'Ввод без ограничений','Invalid Pattern':'Неверный шаблон',
 'Display Mode':'Режим отображения','Attack Speed':'Скорость атаки','Auto Feed':'Автоматическое питание','Auto Stock':'Пополнение запасов',
 'Jump Height':'Высота прыжка','Lava Immunity':'Защита от лавы','Night Vision':'Ночное зрение','No Flight Drift':'Без инерции в полёте',
 'Pick-Craft':'Крафт выбранного блока','Portable Workbench':'Переносной верстак','Reach Boost':'Дальность взаимодействия',
 'Sprint Speed':'Скорость бега','Step Assist':'Подъём без прыжка','Swim Speed':'Скорость плавания','Walk Speed':'Скорость ходьбы',
 'Water Breathing':'Подводное дыхание','Camo Card':'Карта маскировки','Close Cosmetic Inventory':'Закрыть косметический инвентарь',
 'Hide Toggle Button':'Скрыть кнопку переключения','Toggle Button X Position':'Положение кнопки по X',
 'Toggle Button Y Position':'Положение кнопки по Y','Disable /coshat Command':'Отключить команду /coshat',
 'Persistent Item':'Несгораемый предмет','Drop Property Here':'Перетащите свойство сюда',
 'Item Dislocator Activated':'Перемещатель предметов включён','Item Dislocator Deactivated':'Перемещатель предметов выключен',
 'Villager Trading':'Торговля с жителями','Enable Items Highlight':'Подсвечивать предметы','Blink the Highlight':'Мигающая подсветка',
 'Highlight Whole Slot':'Подсвечивать весь слот','Whole Slot Color':'Цвет подсветки всего слота','Background Only Color':'Цвет фоновой подсветки',
 'Enable Villager Trading':'Включить торговлю с жителями','Local Bookmarks Color':'Цвет локальных закладок',
 'Global Bookmarks Color':'Цвет глобальных закладок','Continuous Crafting Method':'Способ непрерывного крафта',
 'Highlight Clicking Slot':'Подсвечивать нажатый слот','§bRemember Checkbox State':'§bЗапоминать состояние флажка',
 'Items Matching':'Сопоставление предметов','Differentiate By NBT':'Различать по NBT','NBT Comparison Type':'Способ сравнения NBT',
 'Stackable Threshold':'Порог размера стака','Background Color':'Цвет фона','Item Scrolling':'Перемещение прокруткой',
 'Scrolling Cursor Items':'Прокрутка предметов на курсоре','Leave One':'Оставлять один','Drop/Throw':'Выбрасывание',
 'Temporary Disable Scrolling':'Временно отключить прокрутку','Auto refill cursor':'Пополнять предметы на курсоре',
 'Add Global Bookmark':'Добавить глобальную закладку','Add Local Bookmark':'Добавить локальную закладку',
 'Do Global Trades':'Выполнить глобальные сделки','Do Local Trades':'Выполнить локальные сделки','Allow Extra':'Допускать лишние данные',
 'Search Tab':'Вкладка поиска','Priority List':'Список приоритетов','Bulk Rename':'Массовое переименование',
 'Disable Fast Swipe':'Отключить быстрое перемещение','Zoom Dependencies':'Предметы для приближения',
 'Miscellaneous':'Разное','Disabled':'Выключено','Enabled':'Включено','Regulate':'Регулирование','Camo':'Маскировка',
 'Evasion':'Уклонение','Flight':'Полёт','Magnet':'Магнит','Regeneration':'Регенерация','Strength':'Сила','Close':'Закрыть',
 'Active':'Включено','Inactive':'Выключено','Overridden':'Внешнее управление','General':'Общие','Spread':'Распределение','Exact':'Точное совпадение',
}
entries=json.loads((ROOT/'draft-language-audit.json').read_text(encoding='utf-8'))['short_english_review']
entries+=json.loads((ROOT/'single-word-review.json').read_text(encoding='utf-8'))
patches={}; changed=[]
for ns,key,en in entries:
 if en not in translations:continue
 file=ROOT/'draft/assets'/ns/'lang/ru_ru.json'
 if ns not in patches:patches[ns]=json.loads(file.read_text(encoding='utf-8')) if file.exists() else {}
 patches[ns][key]=translations[en];changed.append({'namespace':ns,'key':key,'english':en,'russian':translations[en]})
for ns,patch in patches.items():
 file=ROOT/'draft/assets'/ns/'lang/ru_ru.json';file.parent.mkdir(parents=True,exist_ok=True)
 file.write_text(json.dumps(patch,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
(ROOT/'short-translation-provenance.json').write_text(json.dumps(changed,ensure_ascii=False,indent=2),encoding='utf-8')
print('Translated short labels:',len(changed))
