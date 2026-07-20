@echo off
cd /d "%~dp0"
call gradlew.bat runClient --no-daemon --console=plain

