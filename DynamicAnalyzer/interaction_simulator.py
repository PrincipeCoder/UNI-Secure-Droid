# /DynamicAnalyzer/interaction_simulator.py

import subprocess
import time
import random
from config import ADB_PATH, EMULATOR_PORT

class InteractionSimulator:
    """RF-18: Simula interacción del usuario con la app"""
    
    def __init__(self, job_id: str, package_name: str):
        self.job_id = job_id
        self.package_name = package_name
        self.device = f"emulator-{EMULATOR_PORT}"
    
    def launch_app(self) -> bool:
        """Lanza la aplicación en el emulador"""
        try:
            subprocess.run(
                [ADB_PATH, "-s", self.device, "shell", "monkey", "-p", self.package_name, "-c", "android.intent.category.LAUNCHER", "1"],
                capture_output=True,
                timeout=10
            )
            time.sleep(2)
            return True
        except Exception as e:
            print(f"[{self.job_id}] Error lanzando app: {e}")
            return False
    
    def simulate_taps(self, count: int = 10):
        """Simula toques aleatorios en la pantalla"""
        for i in range(count):
            x = random.randint(100, 900)
            y = random.randint(200, 1800)
            try:
                subprocess.run(
                    [ADB_PATH, "-s", self.device, "shell", "input", "tap", str(x), str(y)],
                    capture_output=True,
                    timeout=2
                )
                time.sleep(0.5)
            except:
                pass
    
    def simulate_swipes(self, count: int = 5):
        """Simula deslizamientos en la pantalla"""
        for i in range(count):
            x1, y1 = random.randint(100, 500), random.randint(500, 1500)
            x2, y2 = random.randint(100, 500), random.randint(500, 1500)
            try:
                subprocess.run(
                    [ADB_PATH, "-s", self.device, "shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2)],
                    capture_output=True,
                    timeout=2
                )
                time.sleep(1)
            except:
                pass
    
    def simulate_back_button(self):
        """Simula presionar el botón atrás"""
        try:
            subprocess.run(
                [ADB_PATH, "-s", self.device, "shell", "input", "keyevent", "4"],
                capture_output=True,
                timeout=2
            )
        except:
            pass
    
    def run_interaction_sequence(self):
        """Ejecuta una secuencia completa de interacciones"""
        if not self.launch_app():
            return
        
        time.sleep(3)
        self.simulate_taps(8)
        self.simulate_swipes(3)
        self.simulate_back_button()
        time.sleep(2)
        self.simulate_taps(5)
