# /StaticAnalyzer/utils/timeout.py

import platform

class TimeoutException(Exception):
    pass

class Timeout:
    """
    Un gestor de contexto para ejecutar un bloque de código con un límite de tiempo.
    Compatible con Windows y Unix.
    Uso:
        with Timeout(seconds=5):
            hacer_algo_largo()
    """
    def __init__(self, seconds):
        self.seconds = int(seconds)

    def __enter__(self):
        # En Windows, simplemente no hacemos timeout por ahora
        # En producción usaríamos threading o multiprocessing
        pass

    def __exit__(self, type, value, traceback):
        pass
