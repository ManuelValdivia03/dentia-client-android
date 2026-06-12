# Dentia Android

Cliente Android nativo de Dentia enfocado exclusivamente en pacientes.

## Etapa 1

- Proyecto Android con Kotlin y Jetpack Compose.
- Identidad visual de Dentia con Material 3.
- Navegacion principal del paciente.
- Pantallas base: Inicio, Dentistas, Citas y Mas.
- Componentes reutilizables para tarjetas, estados y acciones.
- Datos de muestra aislados de la futura capa de red.

## Etapa 2

- Inicio de sesion para pacientes.
- Registro y verificacion de correo.
- Recuperacion y cambio de contrasena.
- Sesion persistente con datos cifrados mediante Android Keystore.
- Renovacion automatica de token usando la cookie segura del backend.
- Perfil real del paciente y cierre de sesion.
- API configurada en `https://api.dentia-app.me`.

## Etapa 3

- Logo oficial reutilizado desde el cliente web.
- Directorio real de dentistas priorizados.
- Busqueda por nombre y especialidad.
- Consulta de disponibilidad por fecha.
- Creacion de solicitudes de cita.
- Listado real de citas y estados.
- Cancelacion de citas pendientes o confirmadas.
- Inicio actualizado con la proxima cita real.

## Etapa 4

- Icono de la aplicacion basado en el logo oficial.
- Perfil profesional del dentista.
- Promedio y comentarios recientes de pacientes.
- Reprogramacion de citas con disponibilidad real.
- Valoracion de citas completadas de 1 a 5.

## Etapa 5

- Historial de citas completadas.
- Consulta de recetas y apertura de PDF.
- Listado de archivos clinicos del paciente.
- Carga validada de PDF, imagenes y videos de hasta 10 MB.
- Apertura y eliminacion de archivos clinicos.

## Etapa 6

- Bandeja de conversaciones del paciente.
- Chat disponible con dentistas de citas confirmadas o completadas.
- Envio de mensajes de texto.
- Adjuntos JPG, PNG, WEBP, PDF, MP4 y WEBM de hasta 50 MB.
- Descarga y apertura segura de archivos recibidos.

## Etapa 7

- Perfil del paciente integrado con la sesion.
- Edicion de nombre completo.
- Fotografia de perfil JPG, PNG o WEBP de hasta 5 MB.
- Vista previa y carga de la fotografia actual.
- Correo y tipo de cuenta visibles como datos protegidos.

## Etapa 8

- Interfaz fijada en español de México.
- Estados heredados de citas normalizados y traducidos.
- Errores técnicos conocidos convertidos a mensajes claros en español.
- Fotografías reales de dentistas con iniciales como respaldo.
- Filtro del directorio por especialidad.

## Abrir el proyecto

Abre esta carpeta directamente con Android Studio. El proyecto usa el JDK
incluido con Android Studio y Android SDK 36.

Para compilar desde PowerShell, escribe solamente este comando desde la
carpeta del proyecto:

```powershell
.\build.ps1
```

No copies el texto `PS C:\...>` que aparece antes del comando en PowerShell.

Si PowerShell bloquea el script por su politica de ejecucion, usa:

```powershell
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

La primera compilacion requiere que Android SDK Platform 36 y Android SDK
Build-Tools 36 esten instalados y sus licencias aceptadas desde:

`Android Studio > Tools > SDK Manager`.

## Siguientes etapas

1. Autenticacion y persistencia segura de sesion.
2. Directorio real de dentistas y agendamiento.
3. Administracion y valoracion de citas.
El flujo del paciente queda alineado con las funciones disponibles en el cliente web.
