import requests
import time
import os

print("=" * 60)
print("  DEMOSTRACION - UNI-Secure-Droid Sprint 3")
print("  Sistema de Analisis Estatico y Dinamico de APKs")
print("=" * 60)
print()

# Buscar APK
print("[1] Preparando APK para analisis...")
apk_file = None
test_paths = [
    "tests/test_app.apk",
    "sprint2_app/app/build/outputs/apk/debug/app-debug.apk",
]

for path in test_paths:
    if os.path.exists(path):
        apk_file = path
        print(f"   ✓ APK encontrado: {apk_file}")
        break

if not apk_file:
    print("   ⚠ No se encontró APK real. Usando APK de prueba...")
    apk_file = "test_dummy.apk"
    with open(apk_file, "wb") as f:
        # Crear un ZIP válido mínimo
        f.write(b"PK\x03\x04" + b"\x00" * 100)
    print(f"   ✓ APK de prueba creado: {apk_file}")
    print("   ℹ Nota: Este APK generará un error esperado (no es válido)")

print()

# Subir APK
print("[2] Subiendo APK al servidor...")
try:
    with open(apk_file, "rb") as f:
        files = {"file": (os.path.basename(apk_file), f, "application/vnd.android.package-archive")}
        response = requests.post("http://localhost:8000/analyze", files=files, timeout=5)
    
    if response.status_code == 202:
        job_id = response.json()["job_id"]
        print(f"   ✓ Job creado exitosamente")
        print(f"   ℹ Job ID: {job_id}")
        print()
        
        # Consultar estado
        print("[3] Monitoreando el analisis...")
        print("   " + "-" * 50)
        
        for i in range(20):
            time.sleep(1)
            try:
                status_response = requests.get(f"http://localhost:8000/status/{job_id}", timeout=5)
                data = status_response.json()
                status = data.get("status", "unknown")
                
                # Mostrar progreso
                if i == 0:
                    print(f"   [{i+1:2d}s] Estado: {status:12s} | Esperando worker...")
                else:
                    print(f"   [{i+1:2d}s] Estado: {status:12s}", end="")
                    
                    if status == "processing":
                        print(" | Analizando APK...")
                    elif status == "pending":
                        print(" | En cola...")
                    elif status == "completed":
                        print(" | Completado!")
                    elif status == "error":
                        print(" | Error detectado")
                    else:
                        print()
                
                if status == "completed":
                    print("   " + "-" * 50)
                    print()
                    print("ANALISIS COMPLETADO EXITOSAMENTE")
                    print()
                    print("Resultados del Analisis Estatico:")
                    features = data.get('features', {})
                    print(f"   • Package: {features.get('package_name', 'N/A')}")
                    print(f"   • Activity Principal: {features.get('main_activity', 'N/A')}")
                    print(f"   • Permisos detectados: {len(features.get('permissions', []))}")
                    print(f"   • APIs sospechosas: {len(features.get('api_calls', []))}")
                    print(f"   • URLs encontradas: {len(features.get('urls', []))}")
                    
                    if features.get('permissions'):
                        print()
                        print("   Permisos (primeros 5):")
                        for perm in features.get('permissions', [])[:5]:
                            print(f"      - {perm}")
                    
                    break
                    
                elif status == "error":
                    print("   " + "-" * 50)
                    print()
                    error_msg = data.get('error_details', 'Desconocido')
                    
                    if "not a zip file" in error_msg or "corrupto" in error_msg:
                        print("ERROR ESPERADO: APK de prueba no valido")
                        print()
                        print("DEMOSTRACION EXITOSA:")
                        print("   - El sistema recibio el archivo correctamente")
                        print("   - El worker proceso la tarea")
                        print("   - El analisis se ejecuto (fallo por APK invalido)")
                        print("   - El error fue manejado correctamente")
                        print()
                        print("Para un analisis completo, use un APK real valido")
                    else:
                        print(f"ERROR: {error_msg}")
                    
                    break
                    
            except requests.exceptions.Timeout:
                print(f"   [{i+1:2d}s] ⚠️  Timeout en consulta")
            except Exception as e:
                print(f"   [{i+1:2d}s] ❌ Error: {e}")
                break
        else:
            print("   " + "-" * 50)
            print()
            print("Timeout: El analisis esta tomando mas tiempo del esperado")
            
    else:
        print(f"   Error HTTP {response.status_code}")
        print(f"   Detalle: {response.text}")
        
except requests.exceptions.ConnectionError:
    print("   No se pudo conectar al servidor")
    print("   Asegurate de que el servidor este corriendo en http://localhost:8000")
    print()
    print("   Para iniciar el servidor:")
    print("   1. Terminal 1: cd StaticAnalyzer && celery -A analyzer worker --loglevel=info --pool=solo")
    print("   2. Terminal 2: cd StaticAnalyzer && uvicorn main:app --host 0.0.0.0 --port 8000")
except Exception as e:
    print(f"   Error inesperado: {e}")

print()
print("=" * 60)
print("  FIN DE LA DEMOSTRACIÓN")
print("=" * 60)
