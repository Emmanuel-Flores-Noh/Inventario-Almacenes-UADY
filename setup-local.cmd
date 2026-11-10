@echo off
setlocal
set "ROOT=%~dp0"
cd /d "%ROOT%"

set "VENV_DIR=%ROOT%.venv"
set "VENV_PY=%VENV_DIR%\Scripts\python.exe"

echo ========================================
echo  Configurando entorno local del proyecto
echo ========================================

where py >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set "PYTHON=py -3"
) else (
    set "PYTHON=python"
)

%PYTHON% --version >nul 2>nul
if errorlevel 1 (
    echo [Error] No se encontro Python 3. Instalalo y vuelve a intentarlo.
    exit /b 1
)

if not exist "%VENV_DIR%" (
    echo.
    echo 1. Creando entorno virtual en .venv ...
    %PYTHON% -m venv "%VENV_DIR%"
    if errorlevel 1 (
        echo [Error] No se pudo crear el entorno virtual.
        exit /b 1
    )
) else (
    echo.
    echo 1. El entorno virtual .venv ya existe.
)

echo.
echo 2. Instalando dependencias de Python ...
"%VENV_PY%" -m pip install --upgrade pip
if errorlevel 1 exit /b 1
"%VENV_PY%" -m pip install -r "%ROOT%requirements.txt"
if errorlevel 1 exit /b 1

"%VENV_PY%" -c "import weasyprint" >nul 2>nul
if errorlevel 1 (
    echo.
    echo [Advertencia] WeasyPrint se instalo, pero no pudo importarse.
    echo Probablemente falten librerias del sistema requeridas por WeasyPrint.
)

echo.
echo 3. Instalando Mermaid CLI local en bin\ ...
where npm >nul 2>nul
if errorlevel 1 (
    echo [Advertencia] No se encontro Node.js/npm.
    echo Mermaid CLI no se instalara localmente. Instala Node.js y vuelve a ejecutar este script,
    echo o instala Mermaid globalmente con: npm install -g @mermaid-js/mermaid-cli
) else (
    call npm install --prefix "%ROOT%bin" --no-audit --no-fund
    if errorlevel 1 exit /b 1

    if exist "%ROOT%bin\node_modules\.bin\puppeteer.cmd" (
        echo.
        echo 4. Verificando navegador para Puppeteer ...
        pushd "%ROOT%bin"
        call "%ROOT%bin\node_modules\.bin\puppeteer.cmd" browsers install chrome
        if errorlevel 1 (
            echo [Advertencia] No se pudo descargar Chrome automaticamente.
            echo Si Mermaid falla, define PUPPETEER_EXECUTABLE_PATH con la ruta de un navegador.
        )
        popd
    )
)

echo.
echo ========================================
echo  Entorno local listo
echo ========================================
echo Procesa todos los documentos con:
echo   procesar-docs-local.cmd
echo.
echo O un documento especifico con:
echo   procesar-docs-local.cmd docs\markdown\Proyecto_A
