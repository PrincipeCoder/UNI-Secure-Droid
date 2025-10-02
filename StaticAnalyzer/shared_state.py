# /StaticAnalyzer/shared_state.py

# Simulación de una MetadataDB: usaremos un diccionario de Python.
# En producción, esto sería una base de datos como PostgreSQL o MongoDB.
# Al moverlo a su propio módulo, rompemos la dependencia circular
# entre main.py y analyzer.py.
JOBS_DB = {}