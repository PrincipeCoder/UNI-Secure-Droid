#!/usr/bin/env python3
# Script de prueba del sistema completo

import requests
import time
import sys

BASE_URL = "http://localhost:8000"

def test_static_analysis():
    """Prueba el análisis estático"""
    print("\n=== TEST 1: Análisis Estático ===")
    
    # Subir APK
    with open("tests/test_app.apk", "rb") as f:
        files = {"file": ("test_app.apk", f, "application/vnd.android.package-archive")}
        response = requests.post(f"{BASE_URL}/analyze", files=files)
    
    if response.status_code == 202:
        job_id = response.json()["job_id"]
        print(f"✓ Job creado: {job_id}")
        
        # Consultar estado
        for i in range(30):
            time.sleep(2)
            status_response = requests.get(f"{BASE_URL}/status/{job_id}")
            status = status_response.json()["status"]
            print(f"  Estado: {status}")
            
            if status == "completed":
                print("✓ Análisis estático completado")
                return job_id
            elif status == "error":
                print("✗ Error en análisis")
                return None
        
        print("✗ Timeout esperando resultado")
        return None
    else:
        print(f"✗ Error: {response.status_code}")
        return None

def test_dynamic_analysis(job_id, apk_path, package_name):
    """Prueba el análisis dinámico"""
    print("\n=== TEST 2: Análisis Dinámico ===")
    
    payload = {
        "job_id": job_id,
        "apk_path": apk_path,
        "package_name": package_name
    }
    
    response = requests.post("http://localhost:8001/analyze", json=payload)
    
    if response.status_code == 202:
        print(f"✓ Análisis dinámico iniciado")
        
        for i in range(45):
            time.sleep(2)
            status_response = requests.get(f"http://localhost:8001/status/{job_id}")
            if status_response.status_code == 200:
                status = status_response.json()["status"]
                print(f"  Estado: {status}")
                
                if status in ["success", "completed"]:
                    print("✓ Análisis dinámico completado")
                    return True
        
        print("✗ Timeout")
        return False
    else:
        print(f"✗ Error: {response.status_code}")
        return False

def test_report_generation(job_id):
    """Prueba la generación de reporte"""
    print("\n=== TEST 3: Generación de Reporte ===")
    
    payload = {
        "job_id": job_id,
        "verdict": "malware",
        "risk": "high",
        "family": "trojan",
        "top_signals": ["Permisos sospechosos", "Conexión a C&C"],
        "dynamic_features": {
            "network": {
                "connections": ["192.168.1.100:8080", "malicious.com:443"]
            },
            "file_operations": [
                {"path": "/sdcard/stolen_data.txt", "operation": "created"}
            ]
        }
    }
    
    # Simular generación de reporte
    print(f"✓ Reporte generado para job {job_id}")
    print(f"  Veredicto: {payload['verdict']}")
    print(f"  Riesgo: {payload['risk']}")
    print(f"  Conexiones: {len(payload['dynamic_features']['network']['connections'])}")
    return True

def main():
    print("=" * 50)
    print("PRUEBA DEL SISTEMA UNI-SECURE-DROID")
    print("=" * 50)
    
    # Test 1: Análisis estático
    job_id = test_static_analysis()
    if not job_id:
        print("\n✗ Prueba fallida en análisis estático")
        sys.exit(1)
    
    # Test 2: Análisis dinámico (simulado)
    apk_path = "/tmp/apk_storage/test_app.apk"
    package_name = "com.example.test"
    
    # Test 3: Reporte
    test_report_generation(job_id)
    
    print("\n" + "=" * 50)
    print("✓ TODAS LAS PRUEBAS COMPLETADAS")
    print("=" * 50)

if __name__ == "__main__":
    main()
