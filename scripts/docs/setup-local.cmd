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

echo ========================================
echo  Configurando entorno local del proyecto
echo ========================================
echo Entorno virtual compartido: %VENV_DIR%

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
    echo 1. Creando entorno virtual compartido en %VENV_DIR% ...
    %PYTHON% -m venv "%VENV_DIR%"
    if errorlevel 1 (
        echo [Error] No se pudo crear el entorno virtual.
        exit /b 1
    )
) else (
    echo.
    echo 1. El entorno virtual compartido ya existe.
)

echo.
echo 2. Instalando dependencias de Python ...
"%VENV_PY%" -m pip install --upgrade pip
if errorlevel 1 exit /b 1
"%VENV_PY%" -m pip install -r "%PY_DIR%\requirements.txt"
if errorlevel 1 exit /b 1

"%VENV_PY%" -c "import weasyprint" >nul 2>nul
if errorlevel 1 (
    echo.
    echo [Advertencia] WeasyPrint se instalo, pero no pudo importarse.
    echo Probablemente falten librerias del sistema requeridas por WeasyPrint.
)

echo.
echo 3. Instalando Mermaid CLI local en bin\ con pnpm ...
set "PNPM_CMD="

where pnpm >nul 2>nul
if not errorlevel 1 (
    set "PNPM_CMD=pnpm"
    goto pnpm_listo
)

where corepack >nul 2>nul
if errorlevel 1 goto pnpm_no_disponible

echo    pnpm no esta instalado; intentando activarlo con corepack ...
call corepack enable pnpm >nul 2>nul
where pnpm >nul 2>nul
if not errorlevel 1 (
    set "PNPM_CMD=pnpm"
    goto pnpm_listo
)
set "PNPM_CMD=corepack"
goto pnpm_listo

:pnpm_no_disponible
echo [Advertencia] No se encontro pnpm.
echo Mermaid CLI no se instalara localmente. Instala pnpm con:
echo   corepack enable pnpm
echo o, si no tienes corepack:
echo   npm install -g pnpm
goto fin_mermaid

:pnpm_listo
pushd "%ROOT%\bin"
if "%PNPM_CMD%"=="corepack" (
    call corepack pnpm install
) else (
    call pnpm install
)
if errorlevel 1 (
    popd
    exit /b 1
)
popd

if exist "%ROOT%\bin\node_modules\.bin\puppeteer.cmd" (
    echo.
    echo 4. Verificando navegador para Puppeteer ...
    pushd "%ROOT%\bin"
    call "%ROOT%\bin\node_modules\.bin\puppeteer.cmd" browsers install chrome
    if errorlevel 1 (
        echo [Advertencia] No se pudo descargar Chrome automaticamente.
        echo Si Mermaid falla, define PUPPETEER_EXECUTABLE_PATH con la ruta de un navegador.
    )
    popd
)

:fin_mermaid

echo.
echo ========================================
echo  Entorno local listo
echo ========================================
echo Procesa todos los documentos con:
echo   scripts\docs\procesar-docs-local.cmd
echo.
echo O un documento especifico con:
echo   scripts\docs\procesar-docs-local.cmd docs\markdown\Proyecto_A