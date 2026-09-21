# Vorgefertigte Builds (Pre-built JARs)

Diese Dateien sind **exakt derselbe Build** wie das GitHub-Release
[`v1.2.11-audited`](https://github.com/fakten60-svg/OpenDqrkis/releases/tag/v1.2.11-audited)
— direkt aus dem Quellbaum dieses Repositorys erzeugt (JDK 21, Gradle 9.2.1) und hier
eingespeichert, damit der geprüfte Client auch **ohne GitHub-Release-Navigation** und
**über Tags hinweg** verfügbar bleibt.

| Datei | Zweck | SHA-256 |
|---|---|---|
| `dqrkis-b1.1.jar` | installierbare Mod (in `.minecraft/mods/` legen; Fabric API erforderlich) | `7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430` |
| `dqrkis-b1.1-sources.jar` | Quellcode-Archiv zum Build | `bfb27574fed3adeba96bff3d80abcdcbeb85258221e2bd0266e4c5c7480f79f0` |

## Integrität prüfen

```bash
sha256sum dqrkis-b1.1.jar
# muss liefern: 7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430
```

Weicht die Prüfsumme ab, ist die Datei nicht der geprüfte Build — nicht verwenden.

## Herkunft & Prüfung

- Gebaut aus Commit `ecf7b27` (Tag `v1.2.11-audited`) — siehe `AUDIT_REPORT.md` (§12.5):
  Bytecode-Scan **0 Treffer** über alle Schadcode-Muster, Start-Nachweis mit Screenshots in
  `docs/verification/` und `docs/screenshots/`.
- Identische Datei liegt als Release-Asset unter
  [`v1.2.11-audited`](https://github.com/fakten60-svg/OpenDqrkis/releases/tag/v1.2.11-audited).

## Selbst bauen (empfohlen)

```bash
git clone https://github.com/fakten60-svg/OpenDqrkis.git && cd OpenDqrkis
chmod +x ./gradlew && ./gradlew build
# -> build/libs/dqrkis-b1.1.jar
```

Bei identischer Toolchain kann ein eigener Build dieselbe Prüfsumme liefern; garantiert ist
in jedem Fall ein äquivalentes Verhalten — der Quellcode ist die Autorität, nicht die Binary.
