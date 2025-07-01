package br.edu.utfpr.social_pm46s.data.model.social

data class Challenge(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val target: Int = 0,
    val type: String = "",
    val active: Boolean = true,
    val userProgress: Int = 0
)