package com.dentia.patient.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dentia.patient.ui.components.DentiaCard
import com.dentia.patient.ui.theme.DentiaMuted
import com.dentia.patient.ui.theme.DentiaPrimary
import com.dentia.patient.ui.theme.DentiaSuccess
import com.dentia.patient.R

@Composable
fun AuthFlow(
    state: AuthUiState,
    onShowPage: (AuthPage, String) -> Unit,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit,
    onForgotPassword: (String) -> Unit,
    onResetPassword: (String, String, String) -> Unit,
) {
    when (state.page) {
        AuthPage.Login -> LoginScreen(
            state = state,
            onLogin = onLogin,
            onRegister = { onShowPage(AuthPage.Register, "") },
            onForgot = { onShowPage(AuthPage.ForgotPassword, it) },
        )
        AuthPage.Register -> RegisterScreen(
            state = state,
            onRegister = onRegister,
            onBack = { onShowPage(AuthPage.Login, it) },
        )
        AuthPage.VerifyEmail -> VerifyEmailScreen(
            state = state,
            onVerify = onVerify,
            onResend = onResend,
            onBack = { onShowPage(AuthPage.Login, it) },
        )
        AuthPage.ForgotPassword -> ForgotPasswordScreen(
            state = state,
            onSubmit = onForgotPassword,
            onBack = { onShowPage(AuthPage.Login, it) },
        )
        AuthPage.ResetPassword -> ResetPasswordScreen(
            state = state,
            onSubmit = onResetPassword,
            onRequestAnother = { onShowPage(AuthPage.ForgotPassword, it) },
        )
    }
}

@Composable
fun SessionLoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DentiaMark()
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Preparando tu sesión", color = DentiaMuted)
        }
    }
}

@Composable
private fun LoginScreen(
    state: AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit,
    onForgot: (String) -> Unit,
) {
    var email by remember { mutableStateOf(state.email) }
    var password by remember { mutableStateOf("") }
    LaunchedEffect(state.email) { if (state.email.isNotBlank()) email = state.email }

    AuthContainer(
        title = "Bienvenido de nuevo",
        subtitle = "Accede a tus citas y a tu información dental.",
        state = state,
    ) {
        EmailField(email, { email = it })
        PasswordField("Contraseña", password, { password = it })
        TextButton(
            onClick = { onForgot(email) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("Olvidé mi contraseña")
        }
        SubmitButton(
            text = "Iniciar sesión",
            loadingText = "Entrando...",
            isLoading = state.isSubmitting,
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = { onLogin(email, password) },
        )
        TextButton(onClick = onRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Crear una cuenta de paciente")
        }
    }
}

@Composable
private fun RegisterScreen(
    state: AuthUiState,
    onRegister: (String, String, String) -> Unit,
    onBack: (String) -> Unit,
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(state.email) }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val validPassword = isValidPassword(password)

    AuthContainer(
        title = "Crea tu cuenta",
        subtitle = "Registro exclusivo para pacientes.",
        state = state,
    ) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nombre completo") },
            singleLine = true,
        )
        EmailField(email, { email = it })
        PasswordField("Contraseña", password, { password = it })
        PasswordField("Confirmar contraseña", confirmation, { confirmation = it })
        Text(
            "Usa al menos 8 caracteres, mayúscula, minúscula y número.",
            color = if (password.isBlank() || validPassword) DentiaMuted else MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        SubmitButton(
            text = "Crear cuenta",
            loadingText = "Creando...",
            isLoading = state.isSubmitting,
            enabled = fullName.trim().length >= 3 &&
                email.isNotBlank() &&
                validPassword &&
                password == confirmation,
            onClick = { onRegister(fullName, email, password) },
        )
        TextButton(
            onClick = { onBack(email) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ya tengo una cuenta")
        }
    }
}

@Composable
private fun VerifyEmailScreen(
    state: AuthUiState,
    onVerify: (String, String) -> Unit,
    onResend: (String) -> Unit,
    onBack: (String) -> Unit,
) {
    var email by remember { mutableStateOf(state.email) }
    var code by remember { mutableStateOf("") }

    AuthContainer(
        title = "Verifica tu correo",
        subtitle = "Ingresa el código de 6 dígitos que enviamos a tu correo.",
        state = state,
    ) {
        EmailField(email, { email = it })
        CodeField(code, { code = it })
        SubmitButton(
            text = "Verificar correo",
            loadingText = "Verificando...",
            isLoading = state.isSubmitting,
            enabled = email.isNotBlank() && code.length == 6,
            onClick = { onVerify(email, code) },
        )
        OutlinedButton(
            onClick = { onResend(email) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSubmitting && email.isNotBlank(),
        ) {
            Text("Reenviar código")
        }
        TextButton(onClick = { onBack(email) }, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio de sesión")
        }
    }
}

@Composable
private fun ForgotPasswordScreen(
    state: AuthUiState,
    onSubmit: (String) -> Unit,
    onBack: (String) -> Unit,
) {
    var email by remember { mutableStateOf(state.email) }

    AuthContainer(
        title = "Recupera tu cuenta",
        subtitle = "Te enviaremos un código para cambiar tu contraseña.",
        state = state,
    ) {
        EmailField(email, { email = it })
        SubmitButton(
            text = "Enviar código",
            loadingText = "Enviando...",
            isLoading = state.isSubmitting,
            enabled = email.isNotBlank(),
            onClick = { onSubmit(email) },
        )
        TextButton(onClick = { onBack(email) }, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al inicio de sesión")
        }
    }
}

@Composable
private fun ResetPasswordScreen(
    state: AuthUiState,
    onSubmit: (String, String, String) -> Unit,
    onRequestAnother: (String) -> Unit,
) {
    var email by remember { mutableStateOf(state.email) }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    val validPassword = isValidPassword(password)

    AuthContainer(
        title = "Nueva contraseña",
        subtitle = "Ingresa el código recibido y elige tu nueva contraseña.",
        state = state,
    ) {
        EmailField(email, { email = it })
        CodeField(code, { code = it })
        PasswordField("Nueva contraseña", password, { password = it })
        PasswordField("Confirmar contraseña", confirmation, { confirmation = it })
        SubmitButton(
            text = "Cambiar contraseña",
            loadingText = "Actualizando...",
            isLoading = state.isSubmitting,
            enabled = email.isNotBlank() &&
                code.length == 6 &&
                validPassword &&
                password == confirmation,
            onClick = { onSubmit(email, code, password) },
        )
        TextButton(
            onClick = { onRequestAnother(email) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Solicitar otro código")
        }
    }
}

@Composable
private fun AuthContainer(
    title: String,
    subtitle: String,
    state: AuthUiState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFDDF6F3), MaterialTheme.colorScheme.background),
                ),
            )
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 20.dp, vertical = 48.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DentiaMark()
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            subtitle,
            color = DentiaMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(28.dp))
        DentiaCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                content = content,
            )
        }
        state.errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        state.successMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = DentiaSuccess, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DentiaMark() {
    Image(
        painter = painterResource(R.drawable.dentia_logo),
        contentDescription = "Dentia",
        modifier = Modifier
            .fillMaxWidth(0.62f)
            .height(132.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun EmailField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Correo electrónico") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
    )
}

@Composable
private fun CodeField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { next ->
            onValueChange(next.filter(Char::isDigit).take(6))
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Código de 6 dígitos") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
    )
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
    )
}

@Composable
private fun SubmitButton(
    text: String,
    loadingText: String,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled && !isLoading,
    ) {
        Text(if (isLoading) loadingText else text)
    }
}

private fun isValidPassword(password: String): Boolean =
    password.length >= 8 &&
        password.any(Char::isUpperCase) &&
        password.any(Char::isLowerCase) &&
        password.any(Char::isDigit)
