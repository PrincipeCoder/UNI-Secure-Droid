@echo off
echo === INSTALANDO DEPENDENCIAS UNI-SECURE-DROID ===

echo.
echo [1/3] Instalando dependencias del StaticAnalyzer...
cd StaticAnalyzer
pip install -r requirements.txt
cd ..

echo.
echo [2/3] Instalando dependencias del DynamicAnalyzer...
cd DynamicAnalyzer
pip install -r requirements.txt
cd ..

echo.
echo [3/3] Instalando dependencias del Orchestrator...
cd Orchestrator
pip install -r requirements.txt
cd ..

echo.
echo === INSTALACION COMPLETADA ===
echo.
echo Ahora puedes ejecutar: quick_test.bat
pause
