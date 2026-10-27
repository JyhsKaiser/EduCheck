package com.jyhs.educheck.data.firebase

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jyhs.educheck.core.common.MotivoError
import com.jyhs.educheck.core.common.Resultado
import com.jyhs.educheck.data.local.prefs.SesionPrefs
import com.jyhs.educheck.domain.model.Rol
import com.jyhs.educheck.domain.model.Sesion
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Login con Firebase Authentication. El rol se lee del documento `usuarios/{uid}` de Firestore
 * (la caché offline de Firestore y DataStore permiten reabrir la app sin conexión).
 * Más adelante el backend podrá fijar el rol también como *custom claim*.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: SesionPrefs,
) : AuthRepository {

    override val sesion: Flow<Sesion?> = prefs.sesion

    override suspend fun restaurarSesion() {
        if (auth.currentUser == null) prefs.limpiar()
    }

    override suspend fun iniciarSesion(email: String, clave: String): Resultado<Sesion> {
        return try {
            val usuario = auth.signInWithEmailAndPassword(email, clave).await().user
            if (usuario == null) {
                Resultado.Error(MotivoError.CREDENCIALES)
            } else {
                val perfil = firestore.collection(FirestoreColecciones.USUARIOS).document(usuario.uid).get().await()
                val rol = perfil.getString(FirestoreCampos.ROL)?.let { nombre -> Rol.entries.firstOrNull { it.name == nombre } }
                if (rol == null) {
                    auth.signOut()
                    Resultado.Error(MotivoError.SIN_PERFIL)
                } else if (perfil.getBoolean(FirestoreCampos.ACTIVO) != true) {
                    auth.signOut()
                    Resultado.Error(MotivoError.CUENTA_INACTIVA)
                } else {
                    val nueva = Sesion(
                        uid = usuario.uid,
                        email = usuario.email.orEmpty(),
                        rol = rol,
                        plantelId = perfil.getString(FirestoreCampos.PLANTEL_ID),
                    )
                    prefs.guardar(nueva)
                    Resultado.Ok(nueva)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            Resultado.Error(MotivoError.SIN_RED)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Resultado.Error(MotivoError.CREDENCIALES)
        } catch (e: FirebaseAuthInvalidUserException) {
            Resultado.Error(MotivoError.CREDENCIALES)
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "Firestore: " + e.code, e)
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                auth.signOut()
                Resultado.Error(MotivoError.SIN_RED)
            } else {
                Resultado.Error(MotivoError.DESCONOCIDO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al iniciar sesión", e)
            Resultado.Error(MotivoError.DESCONOCIDO)
        }
    }

    override suspend fun cerrarSesion() {
        auth.signOut()
        prefs.limpiar()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}
