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
 * PIN entry dialog used for "Modo administrador" (see AdminSession). [isSettingNewPin] switches
 * the copy between "criar PIN" (first use) and "digite o PIN" (unlocking).
 */
@Composable
fun AdminPinDialog(
    isSettingNewPin: Boolean,
    errorMessage: String? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isSettingNewPin) "Criar PIN do administrador" else "Modo administrador") },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(
                    if (isSettingNewPin) {
                        "Defina um PIN local para proteger a edição de dados oficiais neste aparelho."
                    } else {
                        "Digite o PIN do administrador para editar dados oficiais."
                    }
                )
                androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { { Text(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pin) }, enabled = pin.length >= 4) {
                Text(if (isSettingNewPin) "Criar" else "Entrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
