@echo off
rem Genera la aplicacion lista para distribuir. Ejecutar desde esta carpeta.
rem   1) Compila y crea el jar con Maven.
rem   2) Crea una carpeta portable con el runtime de Java incluido (Laptop Explorer.exe).
rem   3) Si WiX Toolset 3.x esta instalado, crea ademas el instalador .exe.
rem Requiere JAVA_HOME y MAVEN_HOME (ver mvn.cmd). WiX se busca en WIX_HOME, en WIX
rem (la variable que crea su instalador), en %USERPROFILE%\tools\wix311 o en el PATH.
setlocal
cd /d "%~dp0"

if not exist "%JAVA_HOME%\bin\jpackage.exe" (
  echo No se encontro jpackage. Define la variable de entorno JAVA_HOME con la carpeta del JDK 21.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"
if not defined WIX_HOME if exist "%USERPROFILE%\tools\wix311\candle.exe" set "WIX_HOME=%USERPROFILE%\tools\wix311"
if defined WIX set "PATH=%WIX%bin;%PATH%"
if defined WIX_HOME set "PATH=%WIX_HOME%;%PATH%"

call "%~dp0mvn.cmd" -q -DskipTests clean package || exit /b 1

if exist dist rmdir /s /q dist
if exist installer rmdir /s /q installer
mkdir dist
copy target\laptop-explorer-1.0.0.jar dist\ >nul

set MODS=java.se,jdk.unsupported,jdk.jfr,jdk.crypto.ec,jdk.localedata
set "APP=Laptop Explorer"
set "VERSION=1.0.0"
set "VENDOR=Anthony, Mathias y Julio"
rem Identificador fijo del producto: permite que una version nueva reemplace a la instalada
set "UPGRADE_UUID=1a7c9f57-4ae2-40de-82fa-a6cbeedf208e"

echo.
echo === Version portable (carpeta %APP% con %APP%.exe) ===
jpackage --type app-image --name "%APP%" --app-version %VERSION% ^
  --vendor "%VENDOR%" --copyright "Copyright 2026 %VENDOR%" --description "%APP%" ^
  --input dist --main-jar laptop-explorer-%VERSION%.jar --dest installer ^
  --add-modules %MODS% --java-options "-Dfile.encoding=UTF-8" || exit /b 1

where candle.exe >nul 2>nul
if %errorlevel%==0 (
  echo.
  echo === Instalador .exe ===
  jpackage --type exe --name "%APP%" --app-version %VERSION% ^
    --vendor "%VENDOR%" --copyright "Copyright 2026 %VENDOR%" --description "%APP%" ^
    --input dist --main-jar laptop-explorer-%VERSION%.jar --dest installer ^
    --add-modules %MODS% --java-options "-Dfile.encoding=UTF-8" ^
    --win-shortcut --win-menu --win-menu-group "%APP%" --win-dir-chooser --win-per-user-install ^
    --win-upgrade-uuid %UPGRADE_UUID% || exit /b 1
) else (
  echo.
  echo Aviso: no se encontro WiX Toolset 3.x, asi que solo se creo la version portable.
  echo Para generar tambien el instalador .exe, instala WiX 3.11 o define WIX_HOME con su carpeta.
)
echo.
echo Listo. Revisa la carpeta installer\
endlocal
