package br.edu.utfpr.social_pm46s.data.model

import com.google.firebase.Timestamp

data class UsuarioGrupo(
    val idUsuario: String = "",
    val idGrupo: String = "",
    val dataEntradaGrupo: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", Timestamp.now())
}