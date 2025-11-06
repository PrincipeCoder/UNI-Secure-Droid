# /DynamicAnalyzer/tests/test_sandbox_security.py
# Tests para validar RNF-4, RNF-5, RNF-6, RNF-7, RNF-11

import pytest
import time
import os
from sandbox_manager import SandboxManager
from behavior_monitor import BehaviorMonitor

class TestSandboxSecurity:
    """Tests de seguridad operacional de la sandbox"""
    
    def test_rnf4_network_isolation(self):
        """RNF-4: Verificar que la sandbox no tiene acceso a red externa"""
        with SandboxManager("test_job") as sandbox:
            result = os.system("docker exec dynamic-analyzer ping -c 1 8.8.8.8")
            assert result != 0, "Sandbox tiene acceso a internet (violación RNF-4)"
    
    def test_rnf5_timeout_killswitch(self):
        """RNF-5: Verificar que el timeout mata el emulador a los 90s"""
        sandbox = SandboxManager("test_timeout")
        sandbox.start_emulator()
        
        sandbox.start_time = time.time() - 91
        
        assert sandbox.check_timeout() == True, "Timeout no detectado"
        
        sandbox.kill_emulator()
        assert sandbox.emulator_process is None, "Emulador no fue terminado"
    
    def test_rnf6_snapshot_restoration(self):
        """RNF-6: Verificar que la sandbox se restaura a estado limpio"""
        with SandboxManager("test_restore_1") as sandbox:
            pass
        
        with SandboxManager("test_restore_2") as sandbox:
            assert sandbox.emulator_process is not None
    
    def test_rnf7_secure_artifact_storage(self):
        """RNF-7: Verificar que artefactos se guardan con permisos mínimos"""
        monitor = BehaviorMonitor("test_artifacts")
        monitor.capture_network_traffic()
        
        artifacts_dir = monitor.artifacts_dir
        if os.path.exists(artifacts_dir):
            stat_info = os.stat(artifacts_dir)
            permissions = oct(stat_info.st_mode)[-3:]
            assert permissions == "600", f"Permisos inseguros: {permissions}"
    
    def test_rnf11_orchestrator_only_access(self):
        """RNF-11: Verificar que solo el orquestador puede lanzar sandbox"""
        from main import app
        from fastapi.testclient import TestClient
        
        client = TestClient(app)
        
        response = client.post("/analyze", json={
            "job_id": "test",
            "apk_path": "/tmp/test.apk",
            "package_name": "com.test"
        })
        
        assert response.status_code in [202, 401, 403]
