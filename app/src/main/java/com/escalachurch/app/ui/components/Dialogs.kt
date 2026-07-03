package com.escalachurch.app.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

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
 * Login dialog used for "Modo administrador" (see AdminSession), backed by real Firebase Auth.
 * Accounts are created by whoever manages the Firebase project (Console → Authentication → Add
 * user) - there's no self-serve sign-up here on purpose.
 */
@Composable
fun AdminLoginDialog(
    errorMessage: String? = null,
    onConfirm: (email: String, password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modo administrador") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("Entre com a conta de administrador para editar dados oficiais.")
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(email.trim(), password) }, enabled = email.isNotBlank() && password.length >= 6) {
                Text("Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
