import os
import hashlib
import uuid

class UploadService:
    def __init__(self, max_size_mb=50):
        self.max_size = max_size_mb * 1024 * 1024

    def validar_archivo(self, file_path):
        # Verificar que el archivo exista
        if not os.path.exists(file_path):
            return {"error": "Archivo no encontrado", "status": 404}

        # Verificar extensión
        if not file_path.endswith(".apk"):
            return {"error": "Formato inválido, solo .apk permitido", "status": 400}

        # Verificar tamaño
        if os.path.getsize(file_path) > self.max_size:
            return {"error": "Archivo demasiado grande (>50MB)", "status": 413}

        return {"status": 200}

    def calcular_hash(self, file_path):
        sha256 = hashlib.sha256()
        with open(file_path, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                sha256.update(chunk)
        return sha256.hexdigest()

    def subir(self, file_path):
        validacion = self.validar_archivo(file_path)
        if validacion["status"] != 200:
            return validacion

        try:
            file_hash = self.calcular_hash(file_path)
            job_id = str(uuid.uuid4())
            return {"job_id": job_id, "hash": file_hash, "status": 200}
        except Exception as e:
            return {"error": f"Error al procesar archivo: {e}", "status": 500}
