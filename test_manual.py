import requests
import time

print("=== PRUEBA MANUAL DEL SISTEMA ===\n")

# Crear un APK de prueba simple (archivo dummy)
print("[1] Creando APK de prueba...")
with open("test_dummy.apk", "wb") as f:
    f.write(b"PK\x03\x04" + b"\x00" * 100)  # Firma básica de ZIP/APK
print("✓ APK creado: test_dummy.apk\n")

# Subir APK
print("[2] Subiendo APK al servidor...")
try:
    with open("test_dummy.apk", "rb") as f:
        files = {"file": ("test_dummy.apk", f, "application/vnd.android.package-archive")}
        response = requests.post("http://localhost:8000/analyze", files=files)
    
    if response.status_code == 202:
        job_id = response.json()["job_id"]
        print(f"✓ Job creado: {job_id}\n")
        
        # Consultar estado
        print("[3] Consultando estado del análisis...")
        for i in range(15):
            time.sleep(2)
            status_response = requests.get(f"http://localhost:8000/status/{job_id}")
            data = status_response.json()
            status = data.get("status", "unknown")
            
            print(f"  [{i+1}] Estado: {status}")
            
            if status == "completed":
                print("\n✓ ANÁLISIS COMPLETADO")
                print(f"\nResultados:")
                print(f"  - Package: {data.get('features', {}).get('package_name', 'N/A')}")
                print(f"  - Permisos: {len(data.get('features', {}).get('permissions', []))}")
                print(f"  - APIs: {len(data.get('features', {}).get('api_calls', []))}")
                break
            elif status == "error":
                print(f"\n✗ ERROR: {data.get('error_details', 'Desconocido')}")
                break
        else:
            print("\n⚠ Timeout esperando resultado")
    else:
        print(f"✗ Error HTTP {response.status_code}: {response.text}")
        
except requests.exceptions.ConnectionError:
    print("✗ No se pudo conectar al servidor")
    print("  Asegúrate de que el servidor esté corriendo en http://localhost:8000")
except Exception as e:
    print(f"✗ Error: {e}")

print("\n=== FIN DE LA PRUEBA ===")
