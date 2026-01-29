@echo off
SETLOCAL EnableDelayedExpansion

:: ===========================================================================
:: CONFIGURACIÓN
:: ===========================================================================
SET "KAFKA_DIR=C:\kafka\kafka_2.13-4.1.1"

TITLE Instalador Maestro - Sistema de Impresion (V4 Final)

echo =================================================================
echo   INICIANDO DESPLIEGUE (MODO KRAFT - FIX WINDOWS)
echo =================================================================

:: ---------------------------------------------------------------------------
:: PASO 1: VERIFICACIONES
:: ---------------------------------------------------------------------------
echo.
echo [1/4] Verificando entorno...

java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Java no detectado.
    pause
    EXIT /B 1
)

IF NOT EXIST "%KAFKA_DIR%\bin\windows\kafka-server-start.bat" (
    echo [ERROR] No encuentro Kafka en: "%KAFKA_DIR%"
    pause
    EXIT /B 1
)

call mvn -version >nul 2>&1
IF %ERRORLEVEL% EQU 0 (
    echo   - Maven detectado. Compilando...
    call mvn clean package -DskipTests
) ELSE (
    echo   - [!] Maven NO detectado.
    set /p user_ide="   > Estas usando Eclipse con Maven incluido? (s/n): "
    if /i "!user_ide!"=="s" (
        echo   - [OK] Saltando compilacion.
    ) else (
        echo   [ERROR] Necesitas Maven.
        pause
        EXIT /B 1
    )
)

:: ---------------------------------------------------------------------------
:: PASO 2: LIMPIEZA Y FORMATEO
:: ---------------------------------------------------------------------------
echo.
echo [2/4] Preparando almacenamiento...

:: Definimos la configuración FUERA de bloques complejos para asegurar que la variable es estable
SET "CONF_KRAFT=%KAFKA_DIR%\config\kraft\server.properties"
SET "CONF_STD=%KAFKA_DIR%\config\server.properties"

IF EXIST "%CONF_KRAFT%" (
    SET "KAFKA_CONFIG=%CONF_KRAFT%"
) ELSE (
    SET "KAFKA_CONFIG=%CONF_STD%"
)
echo   - Usando config: %KAFKA_CONFIG%

:: Limpieza de carpetas basura
IF EXIST "C:\tmp\kraft-combined-logs" rmdir /s /q "C:\tmp\kraft-combined-logs" >nul 2>&1
IF EXIST "%tmp%\kraft-combined-logs" rmdir /s /q "%tmp%\kraft-combined-logs" >nul 2>&1
IF EXIST "C:\tmp\kafka-logs" rmdir /s /q "C:\tmp\kafka-logs" >nul 2>&1

:: UUID y Formateo
for /f "tokens=*" %%i in ('call "%KAFKA_DIR%\bin\windows\kafka-storage.bat" random-uuid') do set UUID=%%i
echo   - UUID: %UUID%

call "%KAFKA_DIR%\bin\windows\kafka-storage.bat" format -t %UUID% -c "%KAFKA_CONFIG%" --standalone
IF %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Fallo el formateo.
    pause
    EXIT /B 1
)

:: ---------------------------------------------------------------------------
:: PASO 3: ARRANCAR KAFKA (FIX COMILLAS)
:: ---------------------------------------------------------------------------
echo.
echo [3/4] Arrancando Servidor Kafka...

:: TRUCO: Usamos 'cmd /k call' para forzar una consola estándar que sepa manejar las rutas
:: Expandimos la variable KAFKA_CONFIG aqui mismo (%...%) para que pase como texto plano
start "Kafka Server" cmd /k call "%KAFKA_DIR%\bin\windows\kafka-server-start.bat" "%KAFKA_CONFIG%"

echo   - Esperando 20 segundos...
timeout /t 20 /nobreak > NUL

:: ---------------------------------------------------------------------------
:: PASO 4: CREAR TOPICS
:: ---------------------------------------------------------------------------
echo.
echo [4/4] Creando Topics...

:: Verificacion de puerto 9092
netstat -an | find "9092" >nul
IF %ERRORLEVEL% NEQ 0 (
    echo [PELIGRO] Kafka no parece estar escuchando en el puerto 9092.
    echo Revisa la ventana "Kafka Server" para ver si hay errores Java.
)

call "%KAFKA_DIR%\bin\windows\kafka-topics.bat" --create --topic docs-entrada --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1 --if-not-exists
call "%KAFKA_DIR%\bin\windows\kafka-topics.bat" --create --topic docs-bn --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1 --if-not-exists
call "%KAFKA_DIR%\bin\windows\kafka-topics.bat" --create --topic docs-color --bootstrap-server localhost:9092 --partitions 2 --replication-factor 1 --if-not-exists

echo.
echo =================================================================
echo    SISTEMA LISTO
echo =================================================================
pause