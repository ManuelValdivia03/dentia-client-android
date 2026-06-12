package com.dentia.patient.ui.navigation

enum class PatientDestination(
    val route: String,
    val label: String,
    val symbol: String,
) {
    Home("home", "Inicio", "IN"),
    Chat("chat", "Chat", "CH"),
    Appointments("appointments", "Citas", "CI"),
    ClinicalFiles("clinical-files", "Archivos", "DOC"),
    More("more", "Más", "MÁS"),
}
