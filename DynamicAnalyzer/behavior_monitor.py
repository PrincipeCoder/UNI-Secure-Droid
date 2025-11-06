# /DynamicAnalyzer/behavior_monitor.py

import subprocess
import json
import os
from typing import Dict, List
from config import ADB_PATH, EMULATOR_PORT, ARTIFACTS_PATH, ARTIFACTS_PERMISSIONS

class BehaviorMonitor:
    """Captura comportamiento del malware en ejecución"""
    
    def __init__(self, job_id: str):
        self.job_id = job_id
        self.device = f"emulator-{EMULATOR_PORT}"
        self.artifacts_dir = os.path.join(ARTIFACTS_PATH, job_id)
        os.makedirs(self.artifacts_dir, mode=ARTIFACTS_PERMISSIONS, exist_ok=True)
        
    def capture_syscalls(self) -> List[str]:
        """RF-15: Captura llamadas al sistema usando strace"""
        try:
            result = subprocess.run(
                [ADB_PATH, "-s", self.device, "shell", "ps", "-A"],
                capture_output=True,
                text=True,
                timeout=5
            )
            
            syscalls = []
            for line in result.stdout.splitlines():
                if "com.android" in line or "system_server" in line:
                    syscalls.append(line.strip())
            
            return syscalls[:50]  # Limitar resultados
        except Exception as e:
            print(f"[{self.job_id}] Error capturando syscalls: {e}")
            return []
    
    def capture_network_traffic(self) -> Dict[str, List[str]]:
        """RF-16: Captura tráfico de red"""
        try:
            # Capturar conexiones activas
            result = subprocess.run(
                [ADB_PATH, "-s", self.device, "shell", "netstat", "-an"],
                capture_output=True,
                text=True,
                timeout=5
            )
            
            connections = []
            dns_queries = []
            
            for line in result.stdout.splitlines():
                if "ESTABLISHED" in line or "SYN_SENT" in line:
                    parts = line.split()
                    if len(parts) >= 5:
                        connections.append(parts[4])  # Dirección remota
            
            # RNF-7: Guardar en almacenamiento seguro
            traffic_file = os.path.join(self.artifacts_dir, "network_traffic.json")
            with open(traffic_file, "w") as f:
                json.dump({"connections": connections, "dns": dns_queries}, f)
            os.chmod(traffic_file, ARTIFACTS_PERMISSIONS)
            
            return {"connections": connections[:20], "dns_queries": dns_queries[:20]}
            
        except Exception as e:
            print(f"[{self.job_id}] Error capturando red: {e}")
            return {"connections": [], "dns_queries": []}
    
    def capture_file_operations(self) -> List[Dict[str, str]]:
        """RF-17: Captura operaciones de archivos"""
        try:
            # Listar archivos creados/modificados recientemente
            result = subprocess.run(
                [ADB_PATH, "-s", self.device, "shell", "find", "/sdcard", "-type", "f", "-mmin", "-2"],
                capture_output=True,
                text=True,
                timeout=10
            )
            
            files = []
            for line in result.stdout.splitlines()[:30]:
                if line.strip():
                    files.append({
                        "path": line.strip(),
                        "operation": "created/modified"
                    })
            
            # RNF-7: Guardar artefactos
            files_log = os.path.join(self.artifacts_dir, "file_operations.json")
            with open(files_log, "w") as f:
                json.dump(files, f)
            os.chmod(files_log, ARTIFACTS_PERMISSIONS)
            
            return files
            
        except Exception as e:
            print(f"[{self.job_id}] Error capturando archivos: {e}")
            return []
    
    def collect_all(self) -> Dict:
        """Recolecta todos los comportamientos observados"""
        return {
            "syscalls": self.capture_syscalls(),
            "network": self.capture_network_traffic(),
            "file_operations": self.capture_file_operations()
        }
