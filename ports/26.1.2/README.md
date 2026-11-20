# BronzePanther modpack compatibility sources — Minecraft 26.1.2

Unofficial NeoForge 26.1.2.122 / Java 25 port distributed as part of the BronzePanther modpack. This is not an official Draconic Evolution release. Original ownership and the bundled licenses remain unchanged.

* Draconic Evolution: brandon3055 and contributors, https://github.com/Draconic-Inc/Draconic-Evolution
* Brandon’s Core: brandon3055 and contributors, https://github.com/Draconic-Inc/BrandonsCore
* CodeChickenLib: covers1624 and contributors, https://github.com/TheCBProject/CodeChickenLib
* Original guide: Draconic-Inc/PI-Docs 3.0.0. Original texts and images, their hashes and provenance are bundled in the guide. Missing Russian passages have a separately marked modpack translation. Historical equipment is marked archived.

Build with JDK 25, in order: CodeChickenLib-modern, BrandonsCore, Draconic-Evolution; run each Gradle wrapper with `jar`. Libraries are referenced through the sibling build/libs directories. No launcher signing keys, accounts, live world data, player progress or server credentials are included.

Changes include native item/entity rendering, GUI atlas and layout compatibility, transaction-based energy APIs, guide viewing and Russian translations, and FTB Quests → EMI integration. The single additive quest chapter does not replace any previous chapter or player/team progress.

## Localization and recipe lookup

`localization/` contains the editable Markdown/JSON source form of the Russian resource pack, attribution, provenance and bundled translation licenses. Its 196 added Russian guide pages adapt author/community text to the installed versions. Existing author translations remain available through the baseline pack. `localization-tools/` records the assembly and validation tooling; those scripts expect the original BronzePanther workspace inputs. The editable assets can also be packaged directly as a resource-pack ZIP without those external inputs.

The core client plugin supplies Industrial processing recipes, fills native recipe lookup gaps after author integrations, and indexes loot/acquisition with exact mob-soul variants. Equivalent recipe aliases are checked against the active registration batch to avoid duplicate entries. The acquisition index describes the distributed pack's loot tables and documented runtime mechanics; arbitrary later server datapack changes or custom hooks require regenerating it. Acquisition entries are informational and never invent crafting outputs or modify loot mechanics.

Render changes share immutable reactor sphere vertices while snapshotting transforms, light and overlay per submission. Sprite metadata repairs preserve installed image pixels. Client settings, worlds, quest identifiers and player progress are not migrated or reset by these changes.

Guide polish uses Minecraft's default UI font for GuideME body styles, preserving the other typography, colors and layout. Only GuideME's style initialization is adapted; other game screens and user font resource packs are not changed. Two ExtendedAE demonstration scenes encode empty target stacks with the modern optional-item-stack representation. The derived SNBT source files, original hashes, change description and LGPL/GPL license texts are included under localization/.

The two third-party client compatibility JARs change obsolete @OnlyIn annotation metadata only. Their modification tools are included. Original licenses and credits must be retained. No claim is made to ownership of upstream code, art or audio.
