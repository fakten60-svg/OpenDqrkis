# Sicherheitsrichtlinie (Security Policy)

## Unterstützte Version

| Version | Support |
|---|---|
| Tag `v1.2.11-audited` (Commit `74413d5` und Nachfolger auf `main`) | ✅ wird geprüft |

Nur der **auditierte Stand dieses Repositorys** wird unterstützt. Builds aus anderen
Quellen (Forks mit Änderungen, fremde JAR-Verteilungen, der ursprüngliche kommerzielle
Client) liegen außerhalb jedes Supports — der [Audit-Report](AUDIT_REPORT.md) gilt ausdrücklich
**nur** für genau diesen Quellbaum.

## Schwachstelle melden

Bitte nutze **[GitHub Security Advisories](https://github.com/fakten60-svg/OpenDqrkis/security/advisories/new)**
(„Report a vulnerability") — **keine öffentlichen Issues** für Sicherheitsmeldungen.

Relevant sind Meldungen zu:

- **Netzwerkverhalten**: jeder Code-Pfad, der eine Verbindung außer zur aktuell verbundenen
  Minecraft-Server-Verbindung aufbaut (das wäre ein Verstoß gegen die Kern-Zusage dieses Releases)
- **Code-Ausführung**: dynamisches Klassenladen, native Bibliotheken, Downloads zur Laufzeit
- **Datenabfluss**: Datei-/Clipboard-/Screenshot-Zugriff mit Export über den Spielkontext hinaus
- **Build-Kette**: manipulierte Wrapper/Abhängigkeiten, nicht deklarierte Repositories

Nicht im Scope (erwünscht, aber über normale Issues): Gameplay-Bugs, GUI-Fehler,
Kompatibilitätsprobleme, Balance-/Cheat-Diskussionen.

## Integrität des Release-Artefakts

Prüfe die Release-JAR nach dem Download:

```bash
sha256sum dqrkis-b1.1.jar
# erwartet (siehe AUDIT_REPORT.md §10.3):
# 7501719d056c1d8134212312fce28b6bc550299374e29dbab416c4a69599d430
```

Weicht die Prüfsumme ab, ist die Datei **nicht** der geprüfte Build — nicht verwenden und als
Issue melden. Eigene Builds aus dem Quellbaum erzeugen naturgemäß eine andere Prüfsumme.

## Hinweis zur Nutzung

Dieses Repository dokumentiert die Entfernung von Schadcode aus einem kommerziell vertriebenen
Cheat-Client und dient Forschungszwecken. Es bleibt ein **Cheat-Client**: Die Nutzung kann zu
Server-Bans führen. Die hier dokumentierten Audits ent-schärfen Malware-Fähigkeiten —
sie machen das Cheating weder legitim noch sicherer für deinen Account.
