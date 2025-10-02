# /StaticAnalyzer/utils/timeout.py

import signal

class TimeoutException(Exception):
    pass

def _timeout_handler(signum, frame):
    """Función que se llamará cuando la alarma del sistema suene."""
    raise TimeoutException()

class Timeout:
    """
    Un gestor de contexto para ejecutar un bloque de código con un límite de tiempo.
    Uso:
        with Timeout(seconds=5):
            hacer_algo_largo()
    """
    def __init__(self, seconds):
        self.seconds = int(seconds)

    def __enter__(self):
        if self.seconds > 0:
            # Establece el manejador para la señal de alarma (SIGALRM)
            signal.signal(signal.SIGALRM, _timeout_handler)
            # Programa la alarma para que suene después de N segundos
            signal.alarm(self.seconds)

    def __exit__(self, type, value, traceback):
        if self.seconds > 0:
            # Desactiva la alarma al salir del bloque 'with'
            signal.alarm(0)
