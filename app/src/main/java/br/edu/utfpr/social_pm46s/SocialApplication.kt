package br.edu.utfpr.social_pm46s

import android.app.Application
import br.edu.utfpr.social_pm46s.data.UserProfileManager
import com.google.firebase.FirebaseApp

/**
 * Classe Application personalizada para inicializar o Firebase
 * Seguindo as melhores práticas do Android
 */
class SocialApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        UserProfileManager.init(applicationContext)

        // Inicializa o Firebase
        FirebaseApp.initializeApp(this)
        
        // Log para debug
        android.util.Log.d("SocialApplication", "Firebase inicializado com sucesso!")
    }
} 