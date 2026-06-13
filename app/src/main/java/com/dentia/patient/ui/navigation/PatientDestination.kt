package com.dentia.patient.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import com.dentia.patient.R

enum class PatientDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null,
    val iconResId: Int? = null,
    val showInBottomBar: Boolean = true,
) {
    Home(
        route = "home",
        label = "Inicio",
        icon = Icons.Rounded.Home,
    ),

    Dentists(
        route = "dentists",
        label = "Dentistas",
        iconResId = R.drawable.dentistry,
    ),

    Appointments(
        route = "appointments",
        label = "Citas",
        icon = Icons.Rounded.CalendarMonth,
    ),

    Chat(
        route = "chat",
        label = "Chat",
        icon = Icons.AutoMirrored.Rounded.Chat,
    ),

    ClinicalFiles(
        route = "clinical-files",
        label = "Archivos",
        icon = Icons.Rounded.CalendarMonth,
        showInBottomBar = false,
    ),

    More(
        route = "more",
        label = "Más",
        icon = Icons.Rounded.MoreHoriz,
    ),
}