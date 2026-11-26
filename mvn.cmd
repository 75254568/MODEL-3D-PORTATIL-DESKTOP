@echo off
rem Ejecuta Maven con el JDK 21.
rem Usa las variables de entorno JAVA_HOME (carpeta del JDK 21) y MAVEN_HOME (carpeta de Apache Maven).
setlocal
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo No se encontro el JDK. Define la variable de entorno JAVA_HOME con la carpeta del JDK 21.
  exit /b 1
)
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  echo No se encontro Maven. Define la variable de entorno MAVEN_HOME con la carpeta de Apache Maven.
  exit /b 1
)
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /r /c:"version .21\." >nul
if errorlevel 1 echo Aviso: JAVA_HOME no apunta a un JDK 21. El proyecto esta configurado para Java 21.
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"
call "%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal & exit /b %errorlevel%
