@echo off
rem Ejecuta Maven. Si Maven no esta instalado, lo descarga una sola vez en %USERPROFILE%\.m2\wrapper.
rem Uso: mvnw.cmd spring-boot:run
setlocal
set MVN_VERSION=3.9.11

where mvn >nul 2>nul
if not errorlevel 1 goto usarMvn

set "BASE=%USERPROFILE%\.m2\wrapper"
set "DIR=%BASE%\apache-maven-%MVN_VERSION%"
if exist "%DIR%\bin\mvn.cmd" goto ejecutar

echo Descargando Maven %MVN_VERSION% (solo la primera vez)...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol='Tls12'; New-Item -ItemType Directory -Force -Path '%BASE%' | Out-Null; Invoke-WebRequest -UseBasicParsing -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MVN_VERSION%/apache-maven-%MVN_VERSION%-bin.zip' -OutFile '%BASE%\maven.zip'; Expand-Archive -Force '%BASE%\maven.zip' '%BASE%'; Remove-Item '%BASE%\maven.zip'"
if errorlevel 1 exit /b 1

:ejecutar
call "%DIR%\bin\mvn.cmd" %*
exit /b %ERRORLEVEL%

:usarMvn
call mvn %*
exit /b %ERRORLEVEL%
