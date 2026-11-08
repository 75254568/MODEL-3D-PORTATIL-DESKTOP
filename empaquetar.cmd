@echo off
rem Genera la aplicacion lista para distribuir. Ejecutar desde esta carpeta.
rem   1) Compila y crea el jar con Maven.
rem   2) Crea una carpeta portable con el runtime de Java incluido (no requiere Java instalado).
rem   3) Si WiX Toolset 3.x esta instalado, crea ademas el instalador .exe.
setlocal
set TOOLS=C:\Users\MATHIAS\.gemini\antigravity\scratch\webview-3d-test\tools
if not exist "%TOOLS%\jdk-21" (
  echo Ajusta la variable TOOLS: no se encontro el JDK 21 en %TOOLS%
  exit /b 1
)
set JAVA_HOME=%TOOLS%\jdk-21
set PATH=%JAVA_HOME%\bin;%TOOLS%\maven\bin;%~dp0tools\wix311\bin;%PATH%

call "%TOOLS%\maven\bin\mvn.cmd" -q -DskipTests clean package || exit /b 1

if exist dist rmdir /s /q dist
if exist installer rmdir /s /q installer
mkdir dist
copy target\laptop-studio-3d-1.0.0.jar dist\ >nul

set MODS=java.se,jdk.unsupported,jdk.jfr,jdk.crypto.ec,jdk.localedata

echo.
echo === Version portable (carpeta con LaptopStudio3D.exe) ===
jpackage --type app-image --name LaptopStudio3D --app-version 1.0.0 ^
  --input dist --main-jar laptop-studio-3d-1.0.0.jar --dest installer ^
  --add-modules %MODS% --java-options "-Dfile.encoding=UTF-8" || exit /b 1

where candle.exe >nul 2>nul
if %errorlevel%==0 (
  echo.
  echo === Instalador .exe ===
  jpackage --type exe --name LaptopStudio3D --app-version 1.0.40 ^
    --input dist --main-jar laptop-studio-3d-1.0.0.jar --dest installer ^
    --add-modules %MODS% --java-options "-Dfile.encoding=UTF-8" ^
    --win-shortcut --win-menu --win-dir-chooser --win-per-user-install || exit /b 1
) else (
  echo.
  echo Aviso: WiX Toolset 3.x no esta instalado, asi que solo se creo la version portable.
  echo Instala WiX 3.11 y vuelve a ejecutar este archivo para obtener el instalador .exe.
)
echo.
echo Listo. Revisa la carpeta installer\
endlocal




























