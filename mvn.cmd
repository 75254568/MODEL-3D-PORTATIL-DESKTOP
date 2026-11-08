@echo off
rem Usa el JDK 21 y Maven portatiles (sin instalar nada global). Ajusta TOOLS si los mueves.
set TOOLS=C:\Users\MATHIAS\.gemini\antigravity\scratch\webview-3d-test\tools
set JAVA_HOME=%TOOLS%\jdk-21
set PATH=%JAVA_HOME%\bin;%TOOLS%\maven\bin;%PATH%
call "%TOOLS%\maven\bin\mvn.cmd" %*
