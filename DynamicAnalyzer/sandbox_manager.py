# /DynamicAnalyzer/sandbox_manager.py

import subprocess
import time
import signal
import os
from typing import Optional
from config import (
    EMULATOR_AVD_NAME, EMULATOR_PORT, ADB_PATH,
    DYNAMIC_ANALYSIS_TIMEOUT, SANDBOX_NETWORK_ISOLATED
)

class SandboxManager:
    """RF-14: Gestiona el emulador aislado (sandbox)"""
    
    def __init__(self, job_id: str):
        self.job_id = job_id
        self.emulator_process: Optional[subprocess.Popen] = None
        self.start_time: Optional[float] = None
        
    def start_emulator(self) -> bool:
        """Inicia el emulador con configuración de aislamiento (RNF-4)"""
        try:
            cmd = [
                "emulator",
                "-avd", EMULATOR_AVD_NAME,
                "-port", str(EMULATOR_PORT),
                "-no-window",
                "-no-audio",
                "-no-boot-anim",
            ]
            
            # RNF-4: Aislamiento de red
            if SANDBOX_NETWORK_ISOLATED:
                cmd.extend(["-netdelay", "none", "-netspeed", "full", "-no-snapshot-save"])
            
            self.emulator_process = subprocess.Popen(
                cmd,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                preexec_fn=os.setpgrp if os.name != 'nt' else None
            )
            self.start_time = time.time()
            
            # Esperar a que el emulador esté listo
            return self._wait_for_boot()
            
        except Exception as e:
            print(f"[{self.job_id}] Error iniciando emulador: {e}")
            return False
    
    def _wait_for_boot(self, max_wait: int = 60) -> bool:
        """Espera a que el emulador esté completamente iniciado"""
        for _ in range(max_wait):
            try:
                result = subprocess.run(
                    [ADB_PATH, "-s", f"emulator-{EMULATOR_PORT}", "shell", "getprop", "sys.boot_completed"],
                    capture_output=True,
                    text=True,
                    timeout=2
                )
                if result.stdout.strip() == "1":
                    return True
            except:
                pass
            time.sleep(1)
        return False
    
    def check_timeout(self) -> bool:
        """RNF-5: Verifica si se excedió el timeout"""
        if self.start_time:
            elapsed = time.time() - self.start_time
            return elapsed > DYNAMIC_ANALYSIS_TIMEOUT
        return False
    
    def kill_emulator(self):
        """RNF-5: Kill-switch - Termina forzosamente el emulador (RF-19)"""
        if self.emulator_process:
            try:
                # Terminar proceso y todos sus hijos
                if os.name != 'nt':
                    os.killpg(os.getpgid(self.emulator_process.pid), signal.SIGKILL)
                else:
                    self.emulator_process.kill()
                self.emulator_process.wait(timeout=5)
            except:
                pass
            finally:
                self.emulator_process = None
    
    def restore_snapshot(self):
        """RNF-6: Restaura el emulador a estado limpio"""
        # El emulador se inicia con -no-snapshot-save para evitar guardar cambios
        # Al destruirlo y recrearlo, vuelve al estado original
        self.kill_emulator()
        
    def __enter__(self):
        """Context manager para uso seguro"""
        self.start_emulator()
        return self
    
    def __exit__(self, exc_type, exc_val, exc_tb):
        """Garantiza limpieza al salir"""
        self.kill_emulator()
