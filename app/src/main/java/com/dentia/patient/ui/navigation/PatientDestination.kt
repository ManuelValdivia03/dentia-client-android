package com.dentia.patient.ui.navigation

enum class PatientDestination(
    val route: String,
    val label: String,
    val symbol: String,
    val showInBottomBar: Boolean = true,
) {
    Home(
        route = "home",
        label = "Inicio",
        symbol = "IN",
    ),

    Dentists(
        route = "dentists",
        label = "Dentistas",
        symbol = "DR",
    ),

    Appointments(
        route = "appointments",
        label = "Citas",
        symbol = "CI",
    ),

    Chat(
        route = "chat",
        label = "Chat",
        symbol = "CH",
    ),

    ClinicalFiles(
        route = "clinical-files",
        label = "Archivos",
        symbol = "DOC",
        showInBottomBar = false,
    ),

    More(
        route = "more",
        label = "Más",
        symbol = "MÁS",
    ),
}