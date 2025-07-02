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

        FirebaseApp.initializeApp(this)
        
        android.util.Log.d("SocialApplication", "Firebase inicializado com sucesso!")
    }
} 