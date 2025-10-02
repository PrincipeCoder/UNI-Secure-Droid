# Importamos librerías necesarias
import time              # Para medir tiempos (detectar si se excede el límite de 2 segundos)
import os                # Para crear carpetas y manejar rutas de archivos
from io import BytesIO   # Para trabajar con PDFs en memoria
from reportlab.lib.pagesizes import letter  # Tamaño de página del PDF
from reportlab.pdfgen import canvas         # Herramienta para dibujar en PDF (texto, líneas, etc.)

class ReportService:
    def __init__(self, metadata_db, object_store):
        """
         Constructor del servicio.
        Recibe dos dependencias:
        - metadata_db: base de datos con metadatos del análisis.
        - object_store: servicio de almacenamiento de archivos (simulado o real).
        """
        self.metadata_db = metadata_db
        self.object_store = object_store

    def render_pdf(self, entrada: dict) -> bytes:
        """
        Genera un archivo PDF en memoria usando los datos de entrada.
        Devuelve el contenido binario del PDF (bytes).
        """
        # Creamos un buffer en memoria para almacenar el PDF temporalmente
        buffer = BytesIO()

        # Inicializamos un "canvas" (hoja PDF) con tamaño carta
        c = canvas.Canvas(buffer, pagesize=letter)

        # Título principal del reporte
        c.setFont("Helvetica-Bold", 16)
        c.drawString(100, 750, f"Reporte de Análisis #{entrada.get('job_id', 'N/A')}")

        # Datos generales (veredicto, riesgo, familia)
        c.setFont("Helvetica", 12)
        c.drawString(100, 720, f"Veredicto: {entrada.get('verdict', 'N/A')}")
        c.drawString(100, 700, f"Riesgo: {entrada.get('risk', 'N/A')}")
        c.drawString(100, 680, f"Familia: {entrada.get('family', 'N/A')}")

        # Imprimimos las señales más relevantes, una debajo de otra
        y = 650  # posición inicial vertical
        c.drawString(100, y, "Señales más relevantes:")
        for signal in entrada.get("top_signals", []):
            y -= 20  # desplazamos hacia abajo 20px por línea
            c.drawString(120, y, f"- {signal}")

        # Cerramos y guardamos el PDF en el buffer
        c.save()

        # Obtenemos los bytes finales del PDF
        pdf_data = buffer.getvalue()

        # Cerramos el buffer para liberar memoria
        buffer.close()

        # Retornamos el PDF como bytes (contenido binario)
        return pdf_data

    def generate_report(self, entrada: dict) -> dict:
        """
        Genera un reporte (PDF o JSON) según los datos del análisis.
        Incluye manejo de errores, timeout y persistencia local.
        """

        # Extraemos el job_id (identificador del análisis)
        job_id = entrada.get("job_id")

        # Validamos que haya job_id, si no devolvemos error 400
        if not job_id:
            return {"status_code": 400, "error": "job_id requerido"}

        # Buscamos información del job en la base de metadatos (simulada)
        # Si la función get_job no existe, usamos una lambda que devuelve la entrada directamente
        job_info = getattr(self.metadata_db, "get_job", lambda x: entrada)(job_id)

        # Si no se encuentra el job, devolvemos error 404
        if job_info is None:
            return {"status_code": 404, "error": f"Job {job_id} no encontrado"}

        try:
            # Medimos el tiempo de generación del PDF
            start = time.time()

            # Llamamos a la función que crea el PDF
            pdf_data = self.render_pdf(entrada)

            # Calculamos el tiempo que tomó
            elapsed = time.time() - start

            # Si tarda más de 2 segundos, devolvemos JSON en lugar de PDF
            if elapsed > 2:
                return {
                    "status_code": 200,
                    "content_type": "application/json",
                    "data": entrada
                }

            # Nombre del archivo PDF (según el job_id)
            file_name = f"report_{job_id}.pdf"

            # Guardamos el PDF en el object_store (simulado en tests)
            self.object_store.save(file_name, pdf_data)

            # Guardamos también una copia física local (para que el usuario la vea)
            os.makedirs("reports", exist_ok=True)  # crea carpeta si no existe
            local_path = os.path.join("reports", file_name)

            # Abrimos el archivo en modo escritura binaria y guardamos los bytes del PDF
            with open(local_path, "wb") as f:
                f.write(pdf_data)

            # Retornamos la respuesta de éxito
            return {
                "status_code": 200,                # Código HTTP de éxito
                "content_type": "application/pdf", # Tipo de archivo
                "data": entrada                    # Datos analizados (para mostrar/verificar)
            }

        except Exception as e:
            # Si ocurre cualquier error inesperado, devolvemos 500 con mensaje
            return {"status_code": 500, "error": str(e)}
