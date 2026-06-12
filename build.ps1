$ErrorActionPreference = "Stop"

$androidStudioJdk = "C:\Program Files\Android\Android Studio\jbr"
$androidSdk = "C:\Users\manit\AppData\Local\Android\Sdk"

if (-not (Test-Path "$androidStudioJdk\bin\java.exe")) {
    throw "No se encontro el JDK incluido con Android Studio en: $androidStudioJdk"
}

if (-not (Test-Path $androidSdk)) {
    throw "No se encontro el Android SDK en: $androidSdk"
}

$env:JAVA_HOME = $androidStudioJdk
$env:ANDROID_HOME = $androidSdk
$env:ANDROID_SDK_ROOT = $androidSdk

Write-Host "Compilando Dentia Android con el JDK de Android Studio..."
& "$PSScriptRoot\gradlew.bat" assembleDebug

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$apk = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"
Write-Host ""
Write-Host "Compilacion terminada."
Write-Host "APK: $apk"
