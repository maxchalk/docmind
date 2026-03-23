@echo off
REM Maven Wrapper script for Windows
REM Downloads and runs Maven if not already available

setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.6"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven 3.9.6...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.6/apache-maven-3.9.6-bin.zip' -OutFile '%MAVEN_HOME%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force"
    del "%MAVEN_HOME%\maven.zip"
    for /d %%i in ("%MAVEN_HOME%\apache-maven-*") do (
        xcopy "%%i\*" "%MAVEN_HOME%\" /s /e /y >nul
        rd "%%i" /s /q
    )
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
