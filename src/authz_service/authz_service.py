import hashlib

class AuthZService:
    def __init__(self):
        # Usuarios de prueba con contraseñas hasheadas
        self.users = {
            "juan": {"password": self._hash("1234"), "roles": ["analyst"]},
            "maria": {"password": self._hash("abcd"), "roles": ["user"]},
        }

    def _hash(self, text: str) -> str:
        return hashlib.sha256(text.encode()).hexdigest()

    def login(self, username: str, password: str) -> dict:
        user = self.users.get(username)
        if not user:
            return {"error": "Usuario no encontrado", "status": 401}

        if user["password"] != self._hash(password):
            return {"error": "Credenciales inválidas", "status": 401}

        return {
            "access_token": f"fake-jwt-{username}",
            "roles": user["roles"],
            "status": 200
        }