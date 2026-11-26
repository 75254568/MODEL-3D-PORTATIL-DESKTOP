@echo off
rem Compila y abre Laptop Explorer con Maven.
rem Requiere las variables de entorno JAVA_HOME y MAVEN_HOME (ver mvn.cmd).
cd /d "%~dp0"
rem Consola en UTF-8 para que los mensajes con tildes se lean bien
chcp 65001 >nul

call "%~dp0mvn.cmd" spring-boot:run
if errorlevel 1 (
  echo.
  echo No se pudo iniciar la aplicacion. Revisa el mensaje de arriba.
  pause
)
