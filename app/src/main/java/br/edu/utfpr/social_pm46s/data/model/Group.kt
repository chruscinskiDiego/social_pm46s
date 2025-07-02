package br.edu.utfpr.social_pm46s.data.model

data class Group(
    val id: String = "",
    val nomeGrupo: String = "",
    val tipoGrupo: String = "publico",
    val codigoConvite: String? = null,
    val pontuacaoGrupoAcumulada: Double = 0.0,
    val imagemUrl: String? = null
) {

    constructor() : this("", "", "publico", null, 0.0, null)
}