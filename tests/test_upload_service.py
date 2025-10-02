import unittest
import os
import shutil
from src.upload_service.upload_service import UploadService

# Directorio temporal para archivos de prueba
TEMP_DIR = "temp_test_files"
FAKE_APK_PATH = os.path.join(TEMP_DIR, "fake.apk")
LARGE_APK_PATH = os.path.join(TEMP_DIR, "large.apk")
FAKE_TXT_PATH = os.path.join(TEMP_DIR, "fake.txt")


class TestUploadService(unittest.TestCase):
    def setUp(self):
        # 1. Configuración del servicio y variables
        self.service = UploadService(max_size_mb=50)

        # Crear directorio temporal si no existe
        os.makedirs(TEMP_DIR, exist_ok=True)

        # 2. Crear archivos de prueba
        # Archivo válido (tamaño mínimo)
        with open(FAKE_APK_PATH, "wb") as f:
            f.write(b'PK\x03\x04' + b'\x00' * 100)  # Simula una estructura de zip/apk

        # Archivo de extensión incorrecta (usado en una prueba)
        with open(FAKE_TXT_PATH, "w") as f:
            f.write("contenido")

        # Archivo demasiado grande (50 MB + 1 byte)
        # Usamos el tamaño en bytes: 50 * 1024 * 1024 + 1
        large_size = 50 * 1024 * 1024 + 1
        with open(LARGE_APK_PATH, "wb") as f:
            # Creamos un archivo vacío pero con el tamaño correcto
            f.truncate(large_size)

    def tearDown(self):
        # Limpieza después de cada prueba
        shutil.rmtree(TEMP_DIR)

    def test_archivo_valido(self):
        # Verifica que un archivo .apk válido y pequeño se procese correctamente.
        result = self.service.subir(FAKE_APK_PATH)
        self.assertEqual(result["status"], 200)
        self.assertIn("job_id", result)
        self.assertIn("hash", result)
        self.assertEqual(result["hash"], "c8eaa8e6cf537f4a4d33e57617703411eade8345cf710b7313530595a28971b9")

    def test_archivo_invalido_extension(self):
        # Verifica el rechazo por extensión incorrecta.
        result = self.service.subir(FAKE_TXT_PATH)
        self.assertEqual(result["status"], 400)
        self.assertIn("Formato inválido", result["error"])

    def test_archivo_inexistente(self):
        # Verifica el rechazo por archivo no encontrado.
        result = self.service.subir(os.path.join(TEMP_DIR, "no_existe.apk"))
        self.assertEqual(result["status"], 404)
        self.assertIn("Archivo no encontrado", result["error"])

    def test_archivo_demasiado_grande(self):
        # CRÍTICO: Verifica el rechazo por exceder el límite de 50MB.
        result = self.service.subir(LARGE_APK_PATH)
        self.assertEqual(result["status"], 413)
        self.assertIn("demasiado grande", result["error"])


if __name__ == "__main__":
    unittest.main()