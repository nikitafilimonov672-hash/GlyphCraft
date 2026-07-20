@echo off
cd /d "%~dp0"
call gradlew.bat runVanillaClient --no-daemon --console=plain

