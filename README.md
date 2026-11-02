# Liquid Glass Android

Ein schlankes Android-Kontrollzentrum mit schwebendem, transparentem Panel und Hintergrundunschärfe. Die App heißt auf dem Gerät **Glas Kontrollzentrum** und ist in Kotlin umgesetzt.

## Funktionen

- Helligkeit und Medienlautstärke über Schieberegler anpassen
- Taschenlampe, Screenshot, Bildschirmsperre und Mitteilungen auslösen
- WLAN-, Bluetooth-, Internet-, Energie- und Systemeinstellungen öffnen
- Das Panel über einen Griff am rechten Bildschirmrand einblenden

## Voraussetzungen

- Android 12 oder neuer (API 31+)
- JDK 17 oder neuer
- Gradle 8.9
- Android SDK mit Plattform 35 und Build-Tools 35.0.0

## APK erstellen

Im Repository-Stammverzeichnis ausführen:

```bash
gradle -p GlassCC assembleDebug
```

Die Debug-APK wird hier abgelegt:

```text
GlassCC/app/build/outputs/apk/debug/app-debug.apk
```

Optional lässt sie sich mit ADB installieren:

```bash
adb install -r GlassCC/app/build/outputs/apk/debug/app-debug.apk
```

## Erste Einrichtung

1. App öffnen und **Bedienungshilfen öffnen** wählen.
2. Den Dienst **Glas Kontrollzentrum** in den Android-Bedienungshilfen aktivieren.
3. Zur App zurückkehren und über **Helligkeit erlauben** den Schreibzugriff auf Systemeinstellungen freigeben.
4. Falls Android den Dienst blockiert, in den App-Informationen unter **Eingeschränkte Einstellungen zulassen** die Freigabe erteilen.
5. Den kleinen Griff am rechten Bildschirmrand nach links ziehen, um das Panel zu öffnen. Tippen außerhalb des Panels schließt es.

## Hinweise

- Die Bedienungshilfe wird benötigt, um das schwebende Panel und globale Aktionen wie Screenshot, Sperren und Mitteilungen bereitzustellen.
- WLAN- und Bluetooth-Kacheln öffnen die jeweiligen Android-Einstellungen; sie schalten die Funkverbindungen nicht direkt um.
- Hintergrundunschärfe hängt von Android-Version und Gerätehersteller ab und kann im Energiesparmodus deaktiviert sein.
- Die App fordert keine Internet-Berechtigung an.