@echo off
setlocal
set "ROOT=%~dp0..\.."
cd /d "%ROOT%"

if "%SHARED_VENV_DIR%"=="" (
    set "VENV_DIR=%USERPROFILE%\.venv"
) else (
    set "VENV_DIR=%SHARED_VENV_DIR%"
)
set "VENV_PY=%VENV_DIR%\Scripts\python.exe"
set "PY_DIR=%ROOT%\scripts\docs\procesador"

if "%~1"=="" (
    set "TARGET=%ROOT%\docs"
) else (
    set "TARGET=%~f1"
)

if not exist "%VENV_PY%" (
    echo [Error] No se encontro el entorno virtual compartido en %VENV_DIR%.
    echo Ejecuta primero: scripts\docs\setup-local.cmd
    exit /b 1
)

REM Permite encontrar mmdc instalado localmente en bin\ o en bin\node_modules\.bin\
set "PATH=%ROOT%\bin;%ROOT%\bin\node_modules\.bin;%PATH%"

if exist "%TARGET%\diagramas" (
    echo ========================================
    echo  Procesando diagramas ...
    echo ========================================
    "%VENV_PY%" "%PY_DIR%\procesar_diagramas.py" "%TARGET%"
    if errorlevel 1 exit /b 1
    echo.
)

echo ========================================
echo  Procesando documentos ...
echo ========================================
"%VENV_PY%" "%PY_DIR%\procesar_markdown.py" "%TARGET%"
if errorlevel 1 exit /b 1

echo.
echo Proceso finalizado!