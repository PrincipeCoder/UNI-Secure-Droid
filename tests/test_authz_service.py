import unittest
from src.authz_service import AuthZService

class TestAuthZService(unittest.TestCase):
    def setUp(self):
        # Inicializa el servicio AuthZService antes de cada prueba
        self.auth = AuthZService()

    def test_login_correcto(self):
        # Prueba 1: Login correcto para un 'analyst'
        res = self.auth.login("juan", "1234")
        self.assertEqual(res["status"], 200)
        self.assertIn("access_token", res)
        self.assertEqual(res["roles"], ["analyst"])

    def test_login_user_correcto(self):
        # Prueba 2: Login correcto para un 'user'
        res = self.auth.login("maria", "abcd")
        self.assertEqual(res["status"], 200)
        self.assertIn("access_token", res)
        self.assertEqual(res["roles"], ["user"])

    def test_password_incorrecto(self):
        # Prueba 3: Contraseña incorrecta
        res = self.auth.login("juan", "wrong")
        self.assertEqual(res["status"], 401)
        self.assertEqual(res["error"], "Credenciales inválidas")

    def test_usuario_no_existe(self):
        # Prueba 4: Usuario no existente
        res = self.auth.login("pedro", "1234")
        self.assertEqual(res["status"], 401)
        self.assertEqual(res["error"], "Usuario no encontrado")

if __name__ == "__main__":
    unittest.main()