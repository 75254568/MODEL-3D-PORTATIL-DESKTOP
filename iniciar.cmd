@echo off
rem Inicia Laptop Explorer con doble clic. Usa el JDK 25 de este equipo (el proyecto pide 21, pero no esta instalado).
rem Si instalas un JDK 21, puedes quitar JAVA_HOME, las dos opciones -D y usar solo: mvn spring-boot:run
cd /d "%~dp0"
set JAVA_HOME=C:\Users\julio\.jdks\openjdk-25.0.1
set PATH=%JAVA_HOME%\bin;%PATH%
rem Ruta completa: el mvn.cmd de esta carpeta apunta a otra maquina y taparia al Maven real
call "C:\ProgramData\chocolatey\lib\maven\apache-maven-3.9.14\bin\mvn.cmd" -Dlombok.version=1.18.42 -Dmaven.compiler.proc=full spring-boot:run
if errorlevel 1 (
  echo.
  echo No se pudo iniciar la aplicacion. Revisa el mensaje de arriba.
  pause
)
