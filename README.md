# UNI-Secure-Droid
[![Docker Pulls](https://img.shields.io/docker/pulls/principecoder/uni-secure-droid?logo=docker&style=flat-square)](https://hub.docker.com/r/principecoder/uni-secure-droid)

Aplicación para dispositivos Android orientada a la detección de malware. Combina análisis estático y dinámico e integra técnicas de inteligencia artificial para mejorar la detección.
---

## Tabla de contenidos

- [Descripción](#descripción)
- [Características](#características)
- [Requisitos](#requisitos)
- [Instalación rápida](#instalación-rápida)
- [Configuración (API Key)](#configuración-api-key)
- [Uso en Android Studio](#uso-en-android-studio)
- [Imagen Docker](#imagen-docker)
- [Contribuir](#contribuir)
- [Integrantes](#integrantes)
- [Licencia](#licencia)

---

## Descripción

UNI-Secure-Droid es un proyecto para la detección de aplicaciones Android maliciosas utilizando análisis estático y dinámico junto con modelos de aprendizaje automático.
## Características

- Análisis estático de APKs
- Recolección de métricas dinámicas en emulador o dispositivo
- Integración para enriquecer conjuntos de características destinados a modelos de ML

## Requisitos

- **Android Studio:** Koala, Jellyfish o superior
- **Android SDK:** minSdk 24, targetSdk 34
- **Java JDK:** 17 (para Gradle)
- **Python 3** (si se usan scripts auxiliares)
- Conexión a Internet para dependencias y APIs externas (p. ej. VirusTotal)

## Instalación rápida

1. Clona el repositorio:

```bash
git clone https://github.com/TU_USUARIO/UNI-SecureDroid.git
cd UNI-SecureDroid
```

2. (Opcional) Usar la imagen Docker pública:

```bash
docker pull principecoder/uni-secure-droid
```

3. Abre el proyecto en Android Studio.

## Configuración (API Key)

La API Key de VirusTotal NO debe incluirse en el repositorio.

1. Crea (o edita) `local.properties` en la raíz del proyecto.
2. Añade la línea (reemplaza por tu clave real):

```properties
VIRUSTOTAL_KEY=TU_CLAVE_DE_VIRUSTOTAL
```

3. Asegúrate de que `local.properties` está en `.gitignore`.

## Uso en Android Studio

1. Abre Android Studio → `Open` → selecciona la carpeta del proyecto.
2. Espera a que Gradle sincronice e indexe.
3. Rellena `VIRUSTOTAL_KEY` en `local.properties`.
4. Pulsa `Sync Project with Gradle Files`.
5. Si hay errores de compilación (p. ej. BuildConfig en rojo), ve a `Build` → `Make Project`.
6. Conecta un dispositivo o inicia un emulador y pulsa `Run` (▶).

## Imagen Docker

La imagen pública contiene Ubuntu, Python y OpenJDK; revisa la página en Docker Hub:

https://hub.docker.com/r/principecoder/uni-secure-droid

## Contribuir

- Abre un *issue* para discutir mejoras o bugs.
- Haz un fork y envía un *pull request* con tus cambios.

Incluye descripciones claras y pruebas mínimas cuando sea posible.

## Integrantes

- Torres Reyes Sebastian David
- Smith Tay Carlos Alejandro
- Benites Marín Martín Alberto
- Príncipe Ostos Anghelo Kenedy

## Licencia

Este repositorio no especifica una licencia en el README. Si deseas, puedo añadir un archivo `LICENSE` (por ejemplo MIT) y actualizar el README.

---

Si quieres que haga el commit con el mensaje `fix(readme): ordenar y mejorar formato para GitHub` o que añada una `LICENSE`, dímelo y lo hago.
# UNI-Secure-Droid
Aplicación para dispositivos Android de detección de malware, llamado UNI-Secure Droid, que combine un análisis estático y dinámico. Este sistema usará un conjunto de características más completo y aplicará técnicas avanzadas de inteligencia artificial para superar las limitaciones actuales. 

## Integrantes
- Torres Reyes Sebastian David  
- Smith Tay Carlos Alejandro  
- Benites Marín Martín Alberto  
- Príncipe Ostos Anghelo Kenedy  

## Enlace a la imagen en Docker Hub
[https://hub.docker.com/r/principecoder/uni-secure-droid](https://hub.docker.com/r/principecoder/uni-secure-droid)

La imagen base incluye los siguientes componentes instalados:

- Ubuntu (última versión estable)  
- Python 3 (con `python3` y `pip3`)  
- Enlaces simbólicos para que `python` y `pip` apunten a las versiones 3  
- OpenJDK 11 (Java Development Kit)  
- Dependencias esenciales del sistema

> El directorio de trabajo por defecto dentro del contenedor es **/app**.

## 🛠️ Requisitos Técnicos

Para desplegar y compilar este proyecto, necesitas:

- **Android Studio:** Versión Koala, Jellyfish o superior.
- **Android SDK:** Min SDK 24 (Android 7.0) - Target SDK 34.
- **Java JDK:** Versión 17 (configurada en Gradle).
- **Conexión a Internet:** Para descargar dependencias y consultar la API de VirusTotal.

## ⚙️ Configuración para Desarrolladores (¡IMPORTANTE!)

Como medida de seguridad, la **API Key de VirusTotal no está incluida en el repositorio**. Para compilar la app, cada desarrollador debe configurar su entorno local:

### 1. Clonar el Repositorio
```bash
    git clone [https://github.com/TU_USUARIO/UNI-SecureDroid.git](https://github.com/TU_USUARIO/UNI-SecureDroid.git)

### 2: Abrir en Android Studio

Abre Android Studio, selecciona Open y busca la carpeta clonada. Espera a que termine la indexación.

Paso 3: Configurar Secretos (API Key)

1. El proyecto utiliza el archivo local.properties para inyectar la clave de seguridad sin exponerla en el código.
2. En la vista de proyecto (izquierda), busca el archivo local.properties en la carpeta raíz.
3. Si no existe, créalo.

Abre el archivo y pega la siguiente línea al final:

VIRUSTOTAL_KEY=AQUI_PEGA_TU_CLAVE_DE_VIRUSTOTAL

Paso 4: Sincronizar y Ejecutar

Haz clic en el botón "Sync Project with Gradle Files" (Icono de Elefante 🐘).
Si ves BuildConfig en rojo en el código, ve al menú Build > Make Project.
Conecta tu dispositivo Android (o usa un emulador) y dale al botón Run (▶).