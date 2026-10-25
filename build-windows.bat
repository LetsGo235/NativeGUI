@echo off
setlocal
cd /d "%~dp0"
where gradle >nul 2>nul
if %errorlevel%==0 (
    gradle --no-daemon build
) else (
    echo Gradle not found. Installing Gradle 9.2.1 locally using the official distribution.
    if not exist .tools mkdir .tools
    if not exist ".tools\gradle-9.2.1\bin\gradle.bat" (
        powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-9.2.1-bin.zip' -OutFile '.tools\gradle.zip'; Expand-Archive -Path '.tools\gradle.zip' -DestinationPath '.tools' -Force; Remove-Item '.tools\gradle.zip'"
        if errorlevel 1 exit /b 1
    )
    call ".tools\gradle-9.2.1\bin\gradle.bat" --no-daemon build
)
if errorlevel 1 exit /b 1
echo.
echo Build complete. Find your installable mod JAR in build\libs\
endlocal
