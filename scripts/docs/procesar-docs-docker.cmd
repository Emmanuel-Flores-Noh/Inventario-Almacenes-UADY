@echo off
setlocal
set "ROOT=%~dp0..\.."
cd /d "%ROOT%"

echo ========================================
echo  1. Preparando imagen de Docker...
echo ========================================
REM Construye la imagen. Si ya existe, Docker usara la cache.
docker build -t docs-processor "%ROOT%\scripts\docs\procesador"

echo.
echo ========================================
echo  2. Procesando diagramas y documentos...
echo ========================================
REM Ejecuta el contenedor montando el directorio actual
docker run --rm -v "%ROOT%\docs:/docs" docs-processor /docs

echo.
echo Proceso finalizado!