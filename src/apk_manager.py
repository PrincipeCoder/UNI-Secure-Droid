# -*- coding: utf-8 -*-

"""
Módulo para gestionar la subida de archivos APK a un servidor.

Este módulo define la clase `APKManager`, que facilita el envío de archivos APK
a un endpoint de servidor específico mediante una petición POST.
"""

import requests

class APKManager:
    """
    Clase para gestionar la subida de archivos APK.
    """

    def __init__(self):
        """
        Inicializa el gestor de APKs.
        """
        pass

    def upload_apk(self, file_path, server_url):
        """
        Sube un archivo APK a un servidor.

        Args:
            file_path (str): La ruta local del archivo APK que se va a subir.
            server_url (str): La URL del servidor donde se subirá el archivo.

        Returns:
            bool: True si la subida fue exitosa, False en caso contrario.
        """
        try:
            # Abre el archivo en modo de lectura binaria ('rb').
            # Es importante usar 'rb' para archivos no textuales como los APK.
            with open(file_path, 'rb') as f:
                # Prepara el archivo para ser enviado en la petición POST.
                # 'file' es el nombre del campo que el servidor esperará.
                files = {'file': (file_path, f, 'application/vnd.android.package-archive')}

                # Realiza la petición POST al servidor con el archivo.
                # Se incluye un timeout para evitar que la aplicación se quede
                # colgada indefinidamente en caso de problemas de red.
                response = requests.post(server_url, files=files, timeout=60)

                # Comprueba si la petición fue exitosa (código de estado 2xx).
                response.raise_for_status()

                # Imprime la respuesta del servidor para depuración.
                print(f"Respuesta del servidor: {response.json()}")

                # Devuelve True si la subida fue exitosa.
                return True

        except FileNotFoundError:
            # Maneja el caso en que el archivo no se encuentra en la ruta especificada.
            print(f"Error: El archivo no fue encontrado en '{file_path}'")
            return False
        except requests.exceptions.RequestException as e:
            # Maneja errores de red (e.g., sin conexión, DNS no resuelve).
            print(f"Error de conexión: {e}")
            return False
        except Exception as e:
            # Captura cualquier otro error inesperado.
            print(f"Ocurrió un error inesperado: {e}")
            return False