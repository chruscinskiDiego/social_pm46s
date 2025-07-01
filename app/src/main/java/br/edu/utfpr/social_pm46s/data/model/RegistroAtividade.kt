package br.edu.utfpr.social_pm46s.data.model

import com.google.firebase.Timestamp

data class RegistroAtividade(
    val id: String = "",
    val idUsuario: String = "",
    val nivelAtividadeDetectado: Double = 0.0,
    val dataHoraMedicao: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", 0.0, Timestamp.now())
}