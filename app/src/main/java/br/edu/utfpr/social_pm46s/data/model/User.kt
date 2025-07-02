package br.edu.utfpr.social_pm46s.data.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val nomeUsuario: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val displayName: String = "",
    val nivelAtividadeAcumulado: Double = 0.0,
    val dataCadastro: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", "", "", 0.0, Timestamp.now())
}