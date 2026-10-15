# Tu Mundo de Apps — V1

Catálogo público estilo tienda de aplicaciones, con panel privado para publicar APK, iconos, capturas, descripción, categoría y versiones. Incluye ficha individual con enlace compartible, contador de descargas, valoraciones y comentarios.

## Archivos
Todos los archivos del sitio están en la raíz, sin carpetas. Los archivos subidos por el administrador se guardan en Firebase Storage bajo `appStore/{id}/...`; esas rutas internas no son carpetas del repositorio de GitHub.

## 1. Configurar Firebase
1. En Firebase Console abre el proyecto que destines a Tu Mundo de Apps.
2. Registra una aplicación web y copia la configuración a `firebase-config.js`.
3. En `firebase-config.js`, sustituye `ADMIN_UID` por el UID de tu cuenta administradora de Firebase Authentication.
4. En Authentication habilita **Correo electrónico/contraseña** y **Anónimo** (para enviar opiniones sin obligar a crear una cuenta). Crea la cuenta de administrador y copia su UID.
5. Habilita Firestore Database y Firebase Storage.
6. Como reutilizaremos el proyecto Firebase de CAHESA, NO reemplaces todas las reglas existentes. En Firestore, integra los bloques `match` de `firestore-rules.txt` dentro del bloque `/databases/{database}/documents` que ya existe. En Storage, integra el bloque `match /appStore/...` de `storage-rules.txt` dentro de las reglas existentes. Sustituye `ADMIN_UID_AQUI` por tu UID y conserva intactas las reglas actuales de CAHESA y demás apps. Si prefieres no tocar reglas manualmente, pásame las reglas actuales y preparo una fusión sin borrar las existentes.
7. Revisa la configuración de acceso a Storage/plan de facturación de tu proyecto. Las descargas de APK y capturas consumen almacenamiento y transferencia.

**Importante:** no reutilices reglas permisivas de desarrollo. Estas reglas aíslan las colecciones `appStore_*` de las demás colecciones, pero el contador de descargas es una señal aproximada basada en clics de descarga, no una prueba de que el APK se instaló. Una función backend sería más resistente a manipulación si el sitio crece.

## 2. Administrador
1. En `firebase-config.js`, define `ADMIN_UID` con el UID de la cuenta autorizada.
2. En la web pulsa **Administrar** e inicia sesión con esa cuenta.
3. Publica una app: nombre, slug, categoría, versión, descripción corta y larga, icono, APK y capturas. Para editar, abre la pestaña de aplicaciones publicadas.
4. Para actualizar una app conserva el mismo slug y selecciona el nuevo APK; la ficha y el enlace siguen siendo los mismos.
5. Desmarca “Publicar inmediatamente” para guardar como borrador.

## 3. Enlaces
- Catálogo: URL de GitHub Pages del repositorio.
- Ficha individual: `?app=slug`, por ejemplo `https://USUARIO.github.io/REPOSITORIO/?app=costa`.
- El botón compartir copia el enlace o abre el menú nativo del teléfono.

## 4. GitHub Pages
1. Crea un repositorio nuevo.
2. Sube TODOS los archivos de este ZIP directamente a la raíz del repositorio.
3. En Settings → Pages elige Deploy from a branch, selecciona `main` y `/ (root)`.
4. Espera a que GitHub muestre la URL pública.

## Notas técnicas
- La página requiere Internet para cargar el catálogo, enviar valoraciones y descargar APK. Las aplicaciones publicadas pueden funcionar offline si así se especifica.
- La valoración inicial se calcula con las opiniones publicadas. Se limita a una opinión por UID y app usando el identificador del documento; para controles más fuertes conviene mover la creación de reseñas y los agregados a Cloud Functions.
- El contador representa intentos/clics de descarga que alcanzaron el registro, no instalaciones verificadas. Los navegadores no pueden confirmar que el APK quedó instalado.
- Los APK deben ser tuyos o estar autorizados para distribuirse. Añade política de privacidad/contacto antes de abrirlo al público general.
