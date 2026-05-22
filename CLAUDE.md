# Dilaer — Marcador automático SIM (Android)

## Propósito
App Android personal para marcar secuencialmente una lista de contactos (CSV) usando los
minutos de la SIM local. Sin VoIP, sin pagos por minuto. Distribución por sideload (ADB).

## Comandos
- Compilar APK debug: `./gradlew assembleDebug`
- Limpiar: `./gradlew clean`
- Instalar en dispositivo conectado: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## Stack
- Kotlin 2.0 + Jetpack Compose (Material 3, sin XML de UI)
- minSdk 31 (requerido por `TelephonyCallback`), targetSdk/compileSdk 35
- Patrón: Activity + AndroidViewModel + StateFlow

## Telefonía
- Permisos: `CALL_PHONE`, `READ_PHONE_STATE`
- Marcación: `Intent.ACTION_CALL` con URI `tel:`
- Detección de fin de llamada: `TelephonyCallback.CallStateListener`
  (transición OFFHOOK → IDLE)
- Cool-off entre llamadas: configurable 2–30 s (default 5 s)

## Reglas de diseño
- Importación CSV vía Storage Access Framework — no se requiere `READ_EXTERNAL_STORAGE`.
- Heurística de parser: detecta cabecera y autodetecta columna de teléfono (≥ 6 dígitos).
- Estado por contacto: `PENDING` / `CALLED` / `SKIPPED`. La lista persiste durante la
  sesión hasta que se importe otro CSV o se pulse "Reiniciar lista".
