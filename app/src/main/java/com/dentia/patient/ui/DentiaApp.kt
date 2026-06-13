package com.dentia.patient.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dentia.patient.ui.auth.AuthFlow
import com.dentia.patient.ui.auth.AuthViewModel
import com.dentia.patient.ui.auth.SessionLoadingScreen
import com.dentia.patient.ui.auth.SessionState
import com.dentia.patient.ui.navigation.PatientDestination
import com.dentia.patient.ui.patient.PatientViewModel
import com.dentia.patient.ui.screens.AppointmentsScreen
import com.dentia.patient.ui.screens.ChatScreen
import com.dentia.patient.ui.screens.ClinicalFilesScreen
import com.dentia.patient.ui.screens.DentistsScreen
import com.dentia.patient.ui.screens.HistoryScreen
import com.dentia.patient.ui.screens.HomeScreen
import com.dentia.patient.ui.screens.MoreScreen
import com.dentia.patient.ui.screens.ProfileScreen
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.NavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DentiaApp() {
    val authViewModel: AuthViewModel = viewModel()
    val state = authViewModel.uiState

    when (val session = state.session) {
        SessionState.Loading -> SessionLoadingScreen()

        SessionState.SignedOut -> AuthFlow(
            state = state,
            onShowPage = authViewModel::showPage,
            onLogin = authViewModel::login,
            onRegister = authViewModel::register,
            onVerify = authViewModel::verifyEmail,
            onResend = authViewModel::resendVerificationCode,
            onForgotPassword = authViewModel::requestPasswordReset,
            onResetPassword = authViewModel::resetPassword,
        )

        is SessionState.SignedIn -> PatientApp(
            user = session.user,
            authState = state,
            onClearProfileMessages = authViewModel::clearProfileMessages,
            onUpdateProfile = authViewModel::updateProfile,
            onLogout = authViewModel::logout,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatientApp(
    user: com.dentia.patient.data.model.AuthUser,
    authState: com.dentia.patient.ui.auth.AuthUiState,
    onClearProfileMessages: () -> Unit,
    onUpdateProfile: (String, android.net.Uri?, () -> Unit) -> Unit,
    onLogout: () -> Unit,
) {
    val historyRoute = "history"
    val dentistsRoute = PatientDestination.Dentists.route
    val profileRoute = "profile"

    val patientViewModel: PatientViewModel = viewModel()
    val patientState = patientViewModel.uiState

    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = backStackEntry?.destination?.route

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar {
                PatientDestination.entries
                    .filter { it.showInBottomBar }
                    .forEach { destination ->
                        val selected = currentRoute == destination.route

                        NavigationBarItem(
                            selected = selected,
                            onClick = { navigateToTopLevel(destination.route) },
                            icon = {
                                when {
                                    destination.icon != null -> {
                                        Icon(
                                            imageVector = destination.icon,
                                            contentDescription = destination.label,
                                        )
                                    }

                                    destination.iconResId != null -> {
                                        Icon(
                                            painter = painterResource(id = destination.iconResId),
                                            contentDescription = destination.label,
                                        )
                                    }
                                }
                            },
                            label = { Text(destination.label) },
                        )
                    }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = PatientDestination.Home.route,
            modifier = Modifier,
        ) {
            composable(PatientDestination.Home.route) {
                HomeScreen(
                    contentPadding = contentPadding,
                    patientName = user.displayName,
                    appointments = patientState.appointments,
                    dentists = patientState.dentists,
                    dentistPhotos = patientState.dentistPhotos,
                    onOpenDentists = {
                        navigateToTopLevel(PatientDestination.Dentists.route)
                    },
                    onOpenAppointments = {
                        navigateToTopLevel(PatientDestination.Appointments.route)
                    },
                    onOpenHistory = {
                        navController.navigate(historyRoute)
                    },
                    onOpenClinicalFiles = {
                        navController.navigate(PatientDestination.ClinicalFiles.route)
                    },
                )
            }

            composable(PatientDestination.Dentists.route) {
                DentistsScreen(
                    contentPadding = contentPadding,
                    state = patientState,
                    onRetry = patientViewModel::loadDentists,
                    onOpenSchedule = patientViewModel::clearAvailability,
                    onOpenProfile = patientViewModel::loadDentistRatings,
                    onCloseProfile = patientViewModel::clearDentistRatings,
                    onLoadAvailability = patientViewModel::loadAvailability,
                    onCreateAppointment = { dentist, slot, reason, notes, onSuccess ->
                        patientViewModel.createAppointment(
                            dentistId = dentist.domainId,
                            startAt = slot.startAt,
                            endAt = slot.endAt,
                            reason = reason,
                            notes = notes,
                            onSuccess = onSuccess,
                        )
                    },
                )
            }

            composable(PatientDestination.Appointments.route) {
                AppointmentsScreen(
                    contentPadding = contentPadding,
                    state = patientState,
                    onRetry = patientViewModel::loadAppointments,
                    onCancel = patientViewModel::cancelAppointment,
                    onOpenReschedule = patientViewModel::clearAvailability,
                    onLoadAvailability = patientViewModel::loadAvailability,
                    onReschedule = { appointment, slot, onSuccess ->
                        patientViewModel.rescheduleAppointment(
                            id = appointment.id,
                            startAt = slot.startAt,
                            endAt = slot.endAt,
                            onSuccess = onSuccess,
                        )
                    },
                    onRate = { appointment, score, comment, onSuccess ->
                        patientViewModel.rateAppointment(
                            id = appointment.id,
                            score = score,
                            comment = comment,
                            onSuccess = onSuccess,
                        )
                    },
                )
            }

            composable(PatientDestination.Chat.route) {
                ChatScreen(
                    contentPadding = contentPadding,
                    patientId = user.domainId.orEmpty(),
                    state = patientState,
                    onBack = { navController.popBackStack() },
                    onLoadConversations = patientViewModel::loadConversations,
                    onCreateConversation = patientViewModel::createConversation,
                    onOpenConversation = patientViewModel::openConversation,
                    onCloseConversation = patientViewModel::closeConversation,
                    onRefreshMessages = patientViewModel::loadMessages,
                    onSendMessage = patientViewModel::sendChatMessage,
                    onDownloadAttachment = patientViewModel::downloadChatAttachment,
                )
            }

            composable(PatientDestination.More.route) {
                MoreScreen(
                    contentPadding = contentPadding,
                    patientName = user.displayName,
                    patientEmail = user.email,
                    onHistory = { navController.navigate(historyRoute) },
                    onClinicalFiles = {
                        navController.navigate(PatientDestination.ClinicalFiles.route)
                    },
                    onDentists = {
                        navigateToTopLevel(dentistsRoute)
                    },
                    onProfile = {
                        onClearProfileMessages()
                        navController.navigate(profileRoute)
                    },
                    onLogout = onLogout,
                )
            }

            composable(historyRoute) {
                HistoryScreen(
                    contentPadding = contentPadding,
                    state = patientState,
                    onBack = { navController.popBackStack() },
                    onLoadPrescriptions = patientViewModel::loadPrescriptions,
                    onDownloadPrescription = patientViewModel::downloadPrescription,
                )
            }

            composable(PatientDestination.ClinicalFiles.route) {
                ClinicalFilesScreen(
                    contentPadding = contentPadding,
                    state = patientState,
                    onBack = { navController.popBackStack() },
                    onLoad = patientViewModel::loadClinicalFiles,
                    onUpload = patientViewModel::uploadClinicalFile,
                    onDownload = patientViewModel::downloadClinicalFile,
                    onDelete = patientViewModel::deleteClinicalFile,
                )
            }

            composable(profileRoute) {
                ProfileScreen(
                    contentPadding = contentPadding,
                    user = user,
                    profilePhotoBytes = authState.profilePhotoBytes,
                    isLoadingPhoto = authState.isLoadingProfilePhoto,
                    isSubmitting = authState.isSubmitting,
                    errorMessage = authState.errorMessage,
                    successMessage = authState.successMessage,
                    onBack = { navController.popBackStack() },
                    onClearMessages = onClearProfileMessages,
                    onSave = onUpdateProfile,
                )
            }
        }
    }
}