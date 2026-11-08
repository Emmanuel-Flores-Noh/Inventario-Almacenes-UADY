@echo off
echo ========================================
echo  1. Preparando imagen de Docker...
echo ========================================
REM Construye la imagen. Si ya existe, Docker usara la cache.
docker build -t docs-processor ./tools

echo.
echo ========================================
echo  2. Procesando diagramas y documentos...
echo ========================================
REM Ejecuta el contenedor montando el directorio actual (usando %cd% en Windows)
docker run --rm -v "%cd%/docs:/docs" docs-processor /docs

echo.
echo Proceso finalizado!
