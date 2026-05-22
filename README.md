# Dilaer

Marcador automático para Android que usa la SIM local. Subes un CSV con
`nombre, número` y la app marca secuencialmente, con un tiempo de espera
configurable entre llamadas para no quemar la línea.

## Requisitos
- Android 12 (API 31) o superior
- JDK 17+ y Android SDK para compilar
- ADB para instalar (sideload — no se distribuye por Play Store)

## Compilación

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Si no tienes el wrapper aún, genéralo una vez con Gradle global:

```bash
gradle wrapper --gradle-version=8.10.2
```

## Uso
1. Abrir la app. Conceder permiso de llamadas cuando lo solicite.
2. Pulsar **Importar CSV** y seleccionar el archivo (de Drive, descargas, etc.).
3. Ajustar la espera entre llamadas (recomendado 4–6 s).
4. Pulsar **Iniciar**. La app marca el primer contacto pendiente; cuando
   detecta que la llamada terminó (estado IDLE), espera el cool-off y
   marca el siguiente automáticamente.
5. **Pausar / Saltar / Detener / Reiniciar lista** disponibles en todo momento.

## Formato CSV
- Separador: `,`, `;` o tabulador (autodetectado por línea).
- Cabecera opcional (se omite si detecta `nombre`/`teléfono`/`phone`).
- Una columna → solo número.
- Dos+ columnas → el campo con más dígitos se toma como número, el resto
  se concatena como nombre.

Ejemplo: ver `sample/contactos.csv`.

## Notas importantes
- **iOS no soportado**: Apple obliga a confirmar cada llamada manualmente.
- **Política de uso justo**: las operadoras pueden suspender la línea si
  detectan marcado masivo sin pausas. Mantén el cool-off ≥ 4 s.
- **Solo uso personal/interno**: cumple con las leyes locales de
  telemarketing y registros de No Llamar antes de campañas comerciales.
