@echo off
REM Maven Wrapper script for Windows
REM Downloads Maven if needed and runs it using the project's wrapper config

setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MAVEN_WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"
set "MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9"

if not exist "%MAVEN_HOME%\apache-maven-3.9.9\bin\mvn.cmd" (
    echo Downloading Maven...
    mkdir "%MAVEN_HOME%" 2>nul
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%MAVEN_HOME%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%MAVEN_HOME%\maven.zip' -DestinationPath '%MAVEN_HOME%' -Force"
    del "%MAVEN_HOME%\maven.zip"
)

"%MAVEN_HOME%\apache-maven-3.9.9\bin\mvn.cmd" %*
