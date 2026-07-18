package com.escalachurch.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.escalachurch.app.ui.stellacore.FourPointStar

/** Generic confirmation modal, e.g. for deleting a scale/doxology/event. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Excluir",
    cancelLabel: String = "Cancelar",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(cancelLabel) }
        }
    )
}

/** Pop-up shown on app open when there's an unseen official change relevant to the user. */
@Composable
fun ChangeNewsDialog(
    title: String,
    message: String,
    onViewScale: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onViewScale) { Text("Ver escala") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Agora não") }
        }
    )
}

/**
 * Fase 11.9B Bloco 8 - CelestialAdminLoginCard. Login dialog used for "Modo administrador" (see
 * AdminSession), backed by real Supabase Auth. Accounts are created by whoever manages the
 * Supabase project (Dashboard -> Authentication -> Add user) - there's no self-serve sign-up here
 * on purpose. Visual pass only: same signIn()/resetPassword() calls as before, no password is ever
 * persisted by this dialog, no change to Supabase Auth itself.
 */
@Composable
fun AdminLoginDialog(
    errorMessage: String? = null,
    isLoading: Boolean = false,
    onConfirm: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit,
    onForgotPassword: ((email: String) -> Unit)? = null,
    forgotPasswordMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var recoveryMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        CelestialFrame(modifier = Modifier.fillMaxWidth(), tone = CelestialTone.ADMIN, cornerRadius = 24.dp) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FourPointStar(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.primary,
                        glowColor = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Modo administrador",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Entre com a conta de administrador para editar dados oficiais.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                AnimatedVisibility(visible = !recoveryMode) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Senha") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = if (passwordVisible) "Ocultar senha" else "Mostrar senha"
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            isError = errorMessage != null,
                            supportingText = errorMessage?.let { { Text(it) } },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                        )
                        if (onForgotPassword != null) {
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { recoveryMode = true }) { Text("Esqueci minha senha") }
                        }
                    }
                }

                AnimatedVisibility(visible = recoveryMode) {
                    Text(
                        forgotPasswordMessage ?: "Enviaremos um link de recuperação para este e-mail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(18.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Voltar ao acesso público") }
                    Spacer(Modifier.width(4.dp))
                    if (recoveryMode) {
                        TextButton(
                            onClick = { onForgotPassword?.invoke(email.trim()) },
                            enabled = email.isNotBlank() && !isLoading
                        ) { Text("Enviar link") }
                    } else {
                        TextButton(
                            onClick = { onConfirm(email.trim(), password) },
                            enabled = email.isNotBlank() && password.length >= 6 && !isLoading
                        ) { Text("Entrar") }
                    }
                    if (isLoading) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}
