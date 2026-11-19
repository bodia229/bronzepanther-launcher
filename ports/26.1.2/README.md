# BronzePanther modpack compatibility sources — Minecraft 26.1.2

Unofficial NeoForge 26.1.2.122 / Java 25 port distributed as part of the BronzePanther modpack. This is not an official Draconic Evolution release. Original ownership and the bundled licenses remain unchanged.

* Draconic Evolution: brandon3055 and contributors, https://github.com/Draconic-Inc/Draconic-Evolution
* Brandon’s Core: brandon3055 and contributors, https://github.com/Draconic-Inc/BrandonsCore
* CodeChickenLib: covers1624 and contributors, https://github.com/TheCBProject/CodeChickenLib
* Original guide: Draconic-Inc/PI-Docs 3.0.0. Original texts and images, their hashes and provenance are bundled in the guide. Missing Russian passages have a separately marked modpack translation. Historical equipment is marked archived.

Build with JDK 25, in order: CodeChickenLib-modern, BrandonsCore, Draconic-Evolution; run each Gradle wrapper with `jar`. Libraries are referenced through the sibling build/libs directories. No launcher signing keys, accounts, live world data, player progress or server credentials are included.

Changes include native item/entity rendering, GUI atlas and layout compatibility, transaction-based energy APIs, guide viewing and Russian translations, and FTB Quests → EMI integration. The single additive quest chapter does not replace any previous chapter or player/team progress.

The two third-party client compatibility JARs change obsolete @OnlyIn annotation metadata only. Their modification tools are included. Original licenses and credits must be retained. No claim is made to ownership of upstream code, art or audio.
