from pathlib import Path
import hashlib,json,re,sys,urllib.request,zipfile

ROOT=Path(__file__).resolve().parents[3]
TASK=ROOT/'work/localization-2026-11-19'
version=sys.argv[1] if len(sys.argv)>1 else '1.3.2'
assert re.fullmatch(r'\d+\.\d+\.\d+',version),'Invalid resource-pack version'
draft=TASK/'draft'
old=ROOT/'work/client-preset/resourcepacks/BronzePanther-Russian-1.2.0.zip'
license_path=TASK/'downloaded/AppliedGuideTranslations-LICENSE.txt'
if not license_path.exists():
    with urllib.request.urlopen('https://raw.githubusercontent.com/Zilibobiksus/AppliedGuideTranslations/main/LICENSE',timeout=30) as response:
        license_path.write_bytes(response.read())
assert 'GNU AFFERO GENERAL PUBLIC LICENSE' in license_path.read_text(encoding='utf-8')
meta={'pack':{'min_format':84,'max_format':84,'description':'BronzePanther — русские интерфейсы и руководства'}}
(draft/'pack.mcmeta').write_text(json.dumps(meta,ensure_ascii=False),encoding='utf-8')
(draft/'LICENSES').mkdir(exist_ok=True)
(draft/'LICENSES/AppliedGuideTranslations-AGPL-3.0.txt').write_bytes(license_path.read_bytes())
(draft/'SOURCES').mkdir(exist_ok=True)
for name in ['language-provenance.json','book-draft-provenance.json','short-translation-provenance.json']:
    (draft/'SOURCES'/name).write_bytes((TASK/name).read_bytes())
credits='''# Русские переводы BronzePanther

Дополнение к BronzePanther-Russian-1.2.0.zip. Авторские переводы имеют приоритет
при выборе источника; недостающие строки и адаптации подготовлены для сборки.
Все технические идентификаторы, рецепты и механики сохранены.

## Руководства
- BMP Translations: BMProjects-Development/BMP-Translations,
  https://github.com/BMProjects-Development/BMP-Translations
  (версия 1.21.1 v1.1.10; адаптация страниц под установленные версии).
- Applied Guide Translations: Zilibobiksus,
  https://github.com/Zilibobiksus/AppliedGuideTranslations
  (AGPL-3.0-or-later; текст лицензии приложен; адаптации под установленные версии).
- Advanced Powah 1.1.14: перевод оригинальных шести страниц для этой сборки.
- ExtendedAE (GlodBlock и соавторы, LGPL-3.0): исправлены только данные пустых предметов в двух демонстрационных сценах руководства; исходные сцены доступны в https://github.com/GlodBlock/ExtendedAE. Лицензия и сведения об изменениях включены в LICENSES/ и SOURCES/.

## Авторские языковые файлы
Draconic-Inc/Draconic-Evolution; Traben-0/Entity_Texture_Features;
Traben-0/Entity_Model_Features; pedroksl/AdvancedAE; InventoryProfilesNext/IPN.
Происхождение отдельных строк и страниц указано в SOURCES/*.json.
Исходники адаптированных Markdown и JSON находятся непосредственно в assets/;
архив ресурспака является предпочтительной формой этих переводов для изменения.
Оригинальные авторские тексты и права принадлежат их авторам.
'''
(draft/'TRANSLATIONS.md').write_text(credits,encoding='utf-8')
dest=TASK/('BronzePanther-Russian-'+version+'.zip')
files=sorted(p for p in draft.rglob('*') if p.is_file())
with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in files:
        info=zipfile.ZipInfo(p.relative_to(draft).as_posix(),date_time=(2026,11,19,0,0,0))
        info.compress_type=zipfile.ZIP_DEFLATED
        z.writestr(info,p.read_bytes())
result={'archive':dest.name,'bytes':dest.stat().st_size,'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),
        'files':len(files),'guide_pages':sum(p.suffix=='.md' and 'assets' in p.parts for p in files)}
(TASK/'pack-build.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps(result))
