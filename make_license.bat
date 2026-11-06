@echo off
chcp 65001 >nul
cd /d "%~dp0"

where python >nul 2>nul
if %errorlevel%==0 (
    python "%~dp0make_license.py"
    exit /b
)

where pythonw >nul 2>nul
if %errorlevel%==0 (
    pythonw "%~dp0make_license.py"
    exit /b
)

echo.
echo   [X] 没找到 Python
echo   请先安装 https://www.python.org/downloads/  并勾选 Add Python to PATH
echo.
pause
