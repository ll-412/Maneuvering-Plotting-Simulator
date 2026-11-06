@echo off
cd /d "%~dp0"
python "%~dp0make_license.py"
if errorlevel 1 pause
