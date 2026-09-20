# OpenDqrkis

Ein deobfuszierter, **sicherheitsgeprüfter und netzwerkfreier** Quellcode-Release des
Minecraft-Clients *Dqrkis* (Fabric, Minecraft 1.21.11, Java 21).

> **Wichtig:** Das ist ein **Cheat-Client**. Er verstößt gegen die Nutzungsbedingungen so gut wie
> aller Server und kann zu Bans führen. Dieses Repository existiert zu **Forschungs- und
> Sicherheitszwecken** — es dokumentiert, welche Schadcode-Komponenten im Original enthalten waren
> und wie sie entfernt wurden.

---

## Was ist das?

*Dqrkis* ist ein Minecraft-Cheat-Client für **Fabric 1.21.11**. Das Original wurde offenbar
kommerziell vertrieben und war obfuskiert. Dieses Repository enthält eine entpackte,
nachvollziehbare Java-Quellcode-Version, aus der **alle Schadcode-Fähigkeiten entfernt** wurden.

Der vollständige Prüfbericht steht in **[AUDIT_REPORT.md](AUDIT_REPORT.md)**.

---

## Was wurde entfernt / entschärft

### 1. Datenabfluss (Data Exfiltration) — entfernt
* **`utils/DiscordWebhook.java` komplett gelöscht.** Das war ein generischer HTTP-POST-Client,
  der beliebigen Text **und beliebige lokale Dateien** per `multipart/form-data` an eine frei
  wählbare URL senden konnte — ein reines Exfiltrations-Werkzeug.
* **`SpawnerProtect`**: Die Discord-Webhook-Benachrichtigung samt optionalem `@`-Ping einer
  Discord-ID wurde entfernt. Die lokale Warnung im Chat bleibt.
* **`SilentHomeSetter`**: Der automatische **Screenshot-Upload samt Koordinaten** zu Discord
  wurde komplett entfernt. Das Modul funktioniert weiterhin — es setzt nur noch lokal Homes.
* **`AuctionSniper`**: Der komplette API-Modus wurde entfernt, inklusive des Settings für den
  API-Schlüssel. Vorher wurde ein `Authorization: Bearer <apiKey>`-Header an einen externen
  Endpunkt geschickt. Der **manuelle Modus funktioniert unverändert**.

### 2. Remote Code Execution — entfernt
* **`Utils.replaceModFile()` und `Utils.doDestruct()` gelöscht.** Diese Funktionen luden eine JAR
  aus dem Internet herunter und **überschrieben damit die laufende Mod-Datei des Nutzers**.
* **`SelfDestruct`**: Die Settings `Replace Mod` und `Replace URL` (ein **frei editierbares
  Textfeld**) sowie der Download-Aufruf wurden entfernt. Das Modul entlädt und leert jetzt nur
  noch den Client-Speicher — ohne Dateisystem- oder Netzwerkzugriff.

### 3. Anti-Forensik — entfernt
* `Dqrkis.setLastModified()` / `resetModifiedDate()` wurden gelöscht. Damit wurde der
  Änderungszeitpunkt der eigenen JAR-Datei künstlich zurückgesetzt, um Manipulation zu verbergen.

### 4. Identitätstäuschung — behoben
* `fabric.mod.json` gab sich vollständig als die legitime Mod **ImmediatelyFast** aus: gleicher
  Name in der Beschreibung, echte Modrinth-/GitHub-Links von `RaphiMC/ImmediatelyFast` als
  eigene Homepage/Sources/Issues, **LGPL-3.0-Lizenz** und der fremde Icon-Pfad
  `assets/immediatelyfast/icon.png`. Dazu ein fremder Discord-Invite.
  **Alle Metadaten sind jetzt ehrlich** (Name, Beschreibung, Lizenz `All-Rights-Reserved`,
  korrekter Icon-Pfad, keine Fremd-Links).

### 5. Versteckte Sonderlogik — entfernt
* In `SpawnerProtect` gab es eine **nicht in der GUI sichtbare Ausnahme**:
  `if (player.getName().getString().equalsIgnoreCase("venom")) continue;`
  Für diesen Account wurde die Notfall-Erkennung übersprungen. Zeile gelöscht.

### 6. Versteckte Persistenz außerhalb des Spiels — entfernt
* `ProfileManager` schrieb die Konfiguration unter einem verschleiert wirkenden Zufallsordner
  (`UJHfsGGjbPfVZ`) im **System-Temp-Verzeichnis** und unter Linux/macOS direkt in das
  **Home-Verzeichnis** des Nutzers.
  **Jetzt:** Klartext-JSON in `.minecraft/config/dqrkis.json`. Nichts verlässt mehr den Spielordner.

### 7. Build- und Abhängigkeitsprobleme — behoben
* `build.gradle` referenzierte `lib/annotations-1.0.6.jar`, die **im Repository nicht existiert**.
  Ersetzt durch die reguläre, deklarierte Abhängigkeit `org.jetbrains:annotations:24.1.0`.
* `SelfDestruct` importierte `com.sun.jna.Memory`, obwohl `jna` **nie als Abhängigkeit deklariert
  war** — das Modul war damit nicht kompilierbar. JNA-Nutzung entfernt.
* Die unbenutzte, leere `shadow`-Konfiguration wurde entfernt. Sie diente nur dazu, fremde JARs in
  die Mod-JAR einzubetten (war zum Prüfzeitpunkt leer).
* `logs/latest.log` wurde aus der Versionskontrolle genommen.

---

## Was NICHT geändert wurde

* **Die Cheat-Funktionen selbst** (Aimbot, AutoCrystal, ESP, AutoReconnect, Ping-Spoof …).
  Die Entschärfung entfernt Malware-Fähigkeiten, nicht das Cheating. Das ist eine bewusste
  Entscheidung, keine Lücke im Audit.
* `utils/EncryptedString.java` — ein XOR-„Verschlüsselungs“-Helper. Er ist wirkungslos als
  Verschleierung (die Literale stehen als Klartext im Bytecode), aber auch harmlos.

---

## Nachweis: Der Client ist offline

Nach der Überarbeitung liefert eine Suche im gesamten Quellbaum:

```bash
grep -rn "java\.net\.\|HttpURLConnection\|HttpClient\|openConnection\|Socket\|WebSocket\|Webhook" src/   # 0 Treffer
grep -rnoE "https?://[^\" ]+" src/                                                                       # 0 Treffer
```

Es existiert **keine** Netzwerk-API und **keine** URL mehr. Der Client kann nur noch über die
normale Minecraft-Protokollschicht mit dem Server kommunizieren, zu dem der Nutzer sich verbindet.

Zusätzlich entfernt: keine `ProcessBuilder`/`Runtime.exec`, kein `ClassLoader`/`defineClass`,
kein `System.load`, keine Base64-/AES-/Cipher-Nutzung, keine Registry-/Autostart-/Task-Manipulation,
keine `.bat`/`.sh`/`.vbs`/`.ps1`-Erzeugung, kein Schreibzugriff außerhalb des Spielordners.

---

## Selbst bauen

**Voraussetzungen**
* **JDK 21** (z. B. Eclipse Temurin, Microsoft OpenJDK oder Oracle JDK 21)
* Internetzugang beim ersten Build (Gradle lädt Fabric Loom, Minecraft und Mappings)

**Linux / macOS**

```bash
git clone https://github.com/fakten60-svg/OpenDqrkis.git
cd OpenDqrkis
chmod +x ./gradlew
./gradlew build
```

**Windows (PowerShell / CMD)**

```powershell
git clone https://github.com/fakten60-svg/OpenDqrkis.git
cd OpenDqrkis
.\gradlew.bat build
```

Die fertige Mod liegt danach in:

```
build/libs/dqrkis-<version>.jar
```

Verwende **nicht** die Datei mit dem Suffix `-sources.jar`.

**Nur die JAR bauen**

```bash
./gradlew jar
```

**Installation**

1. [Fabric Loader](https://fabricmc.net/use/) für **Minecraft 1.21.11** installieren.
2. [Fabric API](https://modrinth.com/mod/fabric-api) in den `mods`-Ordner legen.
3. Die gebaute `dqrkis-<version>.jar` ebenfalls in `.minecraft/mods/` legen.
4. Minecraft mit dem Fabric-Profil starten.

> Der Client ist ein **Client-Mod** (`"environment": "client"`). Er lässt sich nicht auf einem
> Dedicated Server installieren.

---

## Build über GitHub Actions

Der Workflow `.github/workflows/gradle.yml` baut das Projekt bei jedem Push automatisch und legt
die JAR als Artifact ab:

1. Repository → **Actions** → **Build Mod**
2. Den neuesten erfolgreichen Lauf öffnen
3. Unter **Artifacts** → `minecraft-mod` herunterladen

---

## Projektstruktur (Kurzfassung)

```
src/main/java/xyz/dqrkis/
├── Dqrkis.java              # Client-Singleton, hält Manager
├── Main.java                # Fabric Entrypoint
├── event/                   # Event-Bus (Tick, Packet, Render, Input …)
├── gui/                     # ClickGUI, TabGUI, Setting-Widgets
├── managers/                # FriendManager, ProfileManager (Config in .minecraft/config)
├── mixin/                   # 28 Minecraft-Mixins (Rendering, Input, Netzwerk-Hooks)
├── module/
│   ├── modules/combat/      # Aimbot-, Crystal-, Totem-Module
│   ├── modules/misc/        # AutoReconnect, SpawnerProtect, AuctionSniper …
│   ├── modules/render/      # ESP, HUD, Scoreboard-Module
│   ├── modules/cart/        # Cart-/Anchor-Module
│   ├── modules/client/      # ClickGUI, Friends, SelfDestruct
│   └── setting/             # Setting-Typen
└── utils/                   # Render-, Inventory-, Rotations- und Netz-Helfer
```

---

## Lizenz / Haftung

`All-Rights-Reserved` (siehe [LICENSE](LICENSE)). Es wird **keinerlei Gewährleistung** übernommen.
Die Nutzung eines Cheat-Clients erfolgt **auf eigenes Risiko** und kann zum Ausschluss von
Servern, zum Verlust von Accounts oder zu anderen Sanktionen führen.

Die Analyse dokumentiert ausschließlich, was im vorliegenden Code enthalten war. Sie ist keine
Aussage über andere Versionen, Builds oder Distributionen dieses Clients.
