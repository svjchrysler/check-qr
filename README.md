# CheckQr

Punto de venta por voz para comerciantes bolivianos. Cuando un cliente
transfiere, la app del banco manda un aviso; CheckQr lo lee y lo dice en voz
alta, para que el comerciante no tenga que mirar la pantalla mientras atiende.

Plan **Básico**: trabaja con los avisos del banco (nivel 🟡) y con la alerta de
"no llegó" (🔴). La confirmación contra la API del banco (🟢) es plan Pro y está
fuera de alcance, aunque el modelo de datos ya la contempla para no migrar el
esquema después.

## Ficha técnica

| | |
|---|---|
| minSdk / targetSdk | 26 / 37 (Android 17) |
| Kotlin / KSP | 2.4.20 / 2.3.12 |
| AGP / Gradle / JDK | 9.4.1 / 9.6.1 / 21 |
| Backend | Go 1.27 + Postgres 18 |

## Cómo correrlo

```bash
./gradlew test              # 119 tests de JVM, sin emulador
./gradlew :app:assembleDebug
cd server && make test-integracion   # necesita un Postgres local
```

Para probar la cadena de captura **sin hacer transferencias reales**, la
variante `debug` trae un inyector de avisos:

```bash
adb shell "am broadcast -a com.seef.checkqr.INYECTAR_AVISO \
  -n com.seef.checkqr.debug/com.seef.checkqr.capture.listener.debugtools.InyectorDeAvisos \
  --es paquete 'com.bcp.innovacxion.yapeapp' \
  --es titulo 'Yape' \
  --es texto 'Recibiste un pago de Bs 50,00 de Juan Perez'"

adb logcat -s InyectorDeAvisos   # muestra qué decidió el parser
```

El acceso a notificaciones hay que concederlo a mano; en un emulador:

```bash
adb shell settings put secure enabled_notification_listeners \
  "$(adb shell settings get secure enabled_notification_listeners):com.seef.checkqr.debug/com.seef.checkqr.capture.listener.ServicioDeAvisosBancarios"
```

## Lo que define el diseño

Cuatro restricciones que no son preferencias y que explican la forma del código:

**La voz obliga a un servicio en primer plano iniciado por el usuario.** Con
targetSdk 37, reproducir audio sin pantalla visible exige un servicio en primer
plano arrancado por una acción del usuario. Si no se cumple, el audio **falla en
silencio, sin ningún error**. Por eso el botón "Abrir caja" es la única puerta,
el servicio es `START_NOT_STICKY` (un servicio que el sistema recrea solo sería
justo lo prohibido) y nada arranca la voz desde `BOOT_COMPLETED`.

**El modo de fallo por omisión de esta app es el silencio.** El listener se
desconecta, una marca mata el servicio, el sistema oculta el texto de un aviso,
el celular se reinicia: nada de eso produce un error visible, simplemente dejan
de llegar pagos. De ahí la pantalla *Estado del sistema*, los avisos explícitos
en la pantalla de caja y la guía de batería por marca.

**El dinero no se adivina.** Todo en centavos enteros, nunca `Float` ni
`Double`. Cuando un texto es ambiguo o contradice el formato que declara la
plantilla del banco, el lector devuelve `null` en lugar de arriesgar: perder un
aviso es visible y se corrige, leer Bs 1,50 como Bs 150 no.

**La app no necesita el backend.** Room es la fuente de verdad. Captura, voz,
cuadre y verificación son locales; el servidor solo reenvía pagos a los cajeros
y sirve las plantillas firmadas. Si se cae, el mostrador sigue funcionando.

## Garantías que impone la base, no el código Kotlin

- **Deduplicación**: índice `UNIQUE` sobre `dedup_key` con `INSERT OR IGNORE`.
  Dos avisos del mismo cobro llegando a la vez desde hilos distintos no pueden
  duplicarse; un "consultar y después insertar" perdería esa carrera.
- **Un pago se reclama una sola vez**: un único `UPDATE ... WHERE
  claimed_by_receipt_id IS NULL`. Comprobarlo en Kotlin dejaría una ventana para
  que dos cajeros cobren el mismo aviso.
- **Subida idempotente**: `UNIQUE (org_id, dedup_key)` en Postgres. El celular
  puede reintentar una subida que en realidad sí llegó, que es lo que pasa
  cuando se corta la red a mitad de la petición.

## Privacidad

El filtro por lista blanca de paquetes es la **primera sentencia** de
`onNotificationPosted`: un aviso de una app que no es de los 6 bancos se
descarta en memoria sin tocar disco.

La herramienta que sí graba todos los avisos vive en `src/debug` y la que no
graba nada en `src/main`, enlazadas por un módulo de Hilt distinto por variante.
Está verificado que la compilación de `release` no contiene el paquete
`debugtools` y que el mapping de R8 no tiene ni una referencia: la garantía es
estructural, no un `if (BuildConfig.DEBUG)` que alguien pueda romper.

`allowBackup=false` y reglas de extracción que excluyen todo, porque la base
guarda nombres de pagadores. Subir al backend un aviso no reconocido requiere
consentimiento explícito y está apagado por omisión.

## Estructura

```
app                       navegación y arranque
core/model  core/common   Kotlin puro: dominio, dinero, voz, comparador
core/database             Room; el dedup y el reclamo único viven aquí
core/datastore            preferencias y latido del listener
core/network              Retrofit + verificación Ed25519 con Tink
core/data                 repositorios y el ingestor de pagos
core/designsystem         tema y tipografía del modo mostrador
capture/listener          NotificationListenerService
capture/parser            Kotlin puro: plantillas por banco + corpus de tests
voice  sync  widget
feature/{onboarding,caja,equipo,cuadre,verificar}
server                    Go + Postgres
```

`core:model`, `core:common` y `capture:parser` son Kotlin puro a propósito: sus
tests corren en la JVM en un segundo, sin emulador. Ahí está la lógica que más
va a cambiar durante la vida del producto.

## Lo que falta antes del piloto

**Bloqueante — los datos reales de los bancos.** Los nombres de paquete de
`capture/parser/src/main/resources/plantillas_base.json` **no están
verificados**, y los 8 casos del corpus son sintéticos. Una lista blanca
equivocada significa cero capturas, en silencio. Hay que instalar la variante
`debug` en un celular con las 6 apps, hacerse un pago de Bs 1 con cada una,
exportar el JSON y meter cada aviso en `capture/parser/src/test/resources/`. El
propio test lo reporta y la app lo advierte en pantalla hasta que se haga.

**Backend.** `POST /v1/sesion` devuelve 501: el alta de usuarios y
organizaciones no tiene sentido hasta que exista. Sin eso, las invitaciones por
QR se generan y se leen (probado), pero no se pueden aceptar.

**Claves y credenciales.** Falta generar el par Ed25519 (`make claves` en
`server/`), poner la pública en `CLAVE_PUBLICA_PLANTILLAS` y la privada en
Secret Manager. Mientras esté vacía, el verificador rechaza todo y la app se
queda con las plantillas empaquetadas, que es el fallo seguro. Falta también
`google-services.json` para FCM.

**Play.** Distribuir **solo por Play**, pista interna: desde Android 13 el
sistema bloquea el acceso a notificaciones en apps instaladas fuera de una
tienda confiable, así que un APK suelto no sirve ni para probar. Hay que
declarar el tipo de servicio en primer plano y grabar el video que muestra cómo
lo activa el usuario. Conviene subir una versión mínima temprano: la revisión
del tipo `mediaPlayback` es el riesgo de calendario más grande y no se descubre
hasta subir. El manifest de `voice` lleva el bloque `specialUse` listo y
comentado, con la justificación redactada, por si Play lo objeta.

**Distribuir como App Bundle, no como APK.** El APK universal pesa 65 MB porque
ML Kit empaquetado trae librerías nativas para las cuatro ABI. El `.aab` son
36 MB y Play entrega solo la ABI del dispositivo: unos 25 MB en un celular
arm64. ML Kit va empaquetado y no descargable a propósito, porque el OCR tiene
que funcionar en el mostrador sin internet.

**Verificación pendiente en dispositivo real.** Todo lo verificado hasta ahora
es en emulador. Falta: un celular reiniciado de verdad, una tablet o pantalla
grande para el modo mostrador, y los avisos con contenido oculto de Android 15+,
cuyo comportamiento exacto hay que medir.
