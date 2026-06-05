@echo off
cd /d "%~dp0"

echo Looking for file: PostgresGUI.jar

if exist PostgresGUI.jar (
    echo Found PostgresGUI.jar, starting application...
    start javaw -jar PostgresGUI.jar
) else (
    echo Error: PostgresGUI.jar not found in current directory
    pause
)