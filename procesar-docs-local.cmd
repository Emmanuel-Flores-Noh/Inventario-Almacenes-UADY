@echo off
setlocal
set "ROOT=%~dp0"
set "VENV_PY=%ROOT%.venv\Scripts\python.exe"

if "%~1"=="" (
    set "TARGET=%ROOT%docs"
) else (
    set "TARGET=%~f1"
)

if not exist "%VENV_PY%" (
    echo [Error] No se encontro el entorno virtual .venv.
    echo Ejecuta primero: setup-local.cmd
    exit /b 1
)

REM Permite encontrar mmdc instalado localmente en bin\ o en bin\node_modules\.bin\
set "PATH=%ROOT%bin;%ROOT%bin\node_modules\.bin;%PATH%"

if exist "%TARGET%\diagramas" (
    echo ========================================
    echo  Procesando diagramas ...
    echo ========================================
    "%VENV_PY%" "%ROOT%tools\procesar_diagramas.py" "%TARGET%"
    if errorlevel 1 exit /b 1
    echo.
)

echo ========================================
echo  Procesando documentos ...
echo ========================================
"%VENV_PY%" "%ROOT%tools\procesar_markdown.py" "%TARGET%"
if errorlevel 1 exit /b 1

echo.
echo Proceso finalizado!
