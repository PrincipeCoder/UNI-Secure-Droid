# -*- coding: utf-8 -*-

"""
Script de ejemplo para demostrar el uso de la clase APKManager.

Este script muestra cómo instanciar APKManager y utilizar el método upload_apk
para subir un archivo a un servidor.
"""

from apk_manager import APKManager
import os

def main():
    """
    Función principal que ejecuta el ejemplo de subida de APK.
    """
    # URL del servidor donde se subirá el APK.
    # Esta URL es un ejemplo y debe ser reemplazada por la URL real del servidor.
    server_url = "http://httpbin.org/post"  # Usamos httpbin.org para pruebas

    # Ruta del archivo APK que se va a subir.
    # En este ejemplo, creamos un archivo falso para la demostración.
    fake_apk_path = "dummy.apk"

    # Crear un archivo falso para simular el APK
    if not os.path.exists(fake_apk_path):
        with open(fake_apk_path, "w") as f:
            f.write("Este es un archivo APK de prueba.")

    # Crear una instancia del gestor de APKs.
    apk_uploader = APKManager()

    # Llamar al método para subir el archivo.
    print(f"Intentando subir el archivo '{fake_apk_path}' a '{server_url}'...")
    success = apk_uploader.upload_apk(fake_apk_path, server_url)

    # Informar al usuario sobre el resultado de la operación.
    if success:
        print("El archivo APK se ha subido correctamente.")
    else:
        print("La subida del archivo APK ha fallado.")

    # Eliminar el archivo falso después de la prueba
    if os.path.exists(fake_apk_path):
        os.remove(fake_apk_path)

if __name__ == "__main__":
    main()