package br.edu.utfpr.social_pm46s.data

import android.content.Context
import android.content.SharedPreferences
import br.edu.utfpr.social_pm46s.data.model.UserData

object UserProfileManager {
    private const val PREFS_NAME = "user_profile_prefs"
    private lateinit var sharedPreferences: SharedPreferences

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun key(key: String, userId: String): String = "${key}_$userId"

    fun saveUserData(userId: String, age: Int, gender: String, weight: Float, height: Float) {
        with(sharedPreferences.edit()) {
            putInt(key("user_age", userId), age)
            putString(key("user_gender", userId), gender)
            putFloat(key("user_weight", userId), weight)
            putFloat(key("user_height", userId), height)
            apply()
        }
    }

    fun getUserData(userId: String): UserData {
        val age = sharedPreferences.getInt(key("user_age", userId), 0)
        val gender = sharedPreferences.getString(key("user_gender", userId), "") ?: ""
        val weight = sharedPreferences.getFloat(key("user_weight", userId), 0f)
        val height = sharedPreferences.getFloat(key("user_height", userId), 0f)
        return UserData(age, gender, weight, height)
    }

    fun isProfileComplete(userId: String): Boolean {
        val age = sharedPreferences.getInt(key("user_age", userId), 0)
        val gender = sharedPreferences.getString(key("user_gender", userId), "") ?: ""
        val weight = sharedPreferences.getFloat(key("user_weight", userId), 0f)
        val height = sharedPreferences.getFloat(key("user_height", userId), 0f)
        return age > 0 && gender.isNotBlank() && weight > 0f && height > 0f
    }
} 