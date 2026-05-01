# PassWallet — estado corto para sesiones limpias

Repo: `/home/jaime/android-apps/PassWallet`

Identidad de app:
- `applicationId`: `org.baumweg.passwallet`
- No instalar APK debug sobre el móvil: hay release firmada instalada.
- Para probar cambios en el móvil, generar APK release firmado con `versionCode` superior.

Release más reciente verificada:
- APK: `android/build/outputs/apk/release/PassWallet-26.5.1-release.apk`
- `versionName`: `26.5.1`
- `versionCode`: `202605010`
- Verificado: `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, `testDebugUnitTest`, `assembleRelease`, `git diff --check`, `apksigner verify`, `aapt dump badging`.

Preferencias de trabajo:
- Cambios pequeños, uno por sesión si es posible.
- Mantener estilo moderno tipo Material/FossWallet, sin reescrituras grandes innecesarias.
- Preservar arquitectura legacy/XML cuando la pantalla ya sea XML/View.
- Responder en español.
- Al terminar un cambio relevante: compilar release firmada, verificar y entregar APK.

Notas técnicas importantes:
- La app está fijada en modo oscuro; se eliminó el selector claro/oscuro.
- La acción de eliminar desde la vista del pase mueve a papelera, no borra definitivamente.
- La eliminación definitiva queda para vaciar papelera.
- La corrección ortográfica/sugerencias/autofill está desactivada en campos editables relevantes.
- El selector de tipo de código de barras ya es desplegable Material.
- Formatos añadidos y soportados por encoder ZXing: `CODABAR`, `CODE_93`, `UPC_A`, `UPC_E`.
- La actualización de pase: respuestas HTTP sin pase válido (`204`, `404`, `410`, etc.) cierran el progreso y muestran error; la descarga usa timeouts explícitos.
- Encabezados/barras superiores: usar el oscuro `@color/top_app_bar`/`@color/status_bar`; evitar volver a `?attr/colorPrimary` beige-mostaza en toolbars.
- Copia de seguridad: no mostrar bloque explicativo general; mantener explicación dentro de las tarjetas “Exportar copia” e “Importar copia”.
- Preferencias > Orden: interceptar `onDisplayPreferenceDialog` para usar solo bottom sheet Material personalizado, no el diálogo legacy de `ListPreference`; fondo del bottom sheet transparente para evitar esquinas grises cuadradas.
- Evitar meter dependencias grandes salvo necesidad real.

Cómo arrancar una sesión nueva sin arrastrar contexto:
1. Leer este archivo.
2. Inspeccionar el proyecto con:
   `python3 ~/.hermes/skills/jaime/android-apk-factory/scripts/inspect_android_project.py /home/jaime/android-apps/PassWallet`
3. Revisar `git status --short`.
4. Cargar la skill Android solo si se va a tocar código, compilar, generar APK, instalar, firmar, consultar APIs Android/Material/Kotlin o aplicar reglas específicas de PassWallet.
5. Hacer un único cambio del plan, verificar y actualizar este archivo con versión/APK/estado.
