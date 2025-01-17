package com.example.apptienda.ui.components.barcode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun GenerateBarcodesDialog(
    onDismiss: () -> Unit,
    onGenerate: (startId: Int, count: Int) -> Unit
) {
    var startId by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("") }

    // Obtener el contexto fuera del remember
    val context = LocalContext.current
    // Calcular los códigos por página usando remember con el contexto
    val codesPerPage = remember(context) {
        BarcodeGenerator(context).calculateCodesPerPage()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generar Códigos de Barra") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = startId,
                    onValueChange = { startId = it },
                    label = { Text("Número inicial") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = startId.isNotEmpty() && startId.toIntOrNull() == null
                )

                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Cantidad de códigos") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = count.isNotEmpty() && count.toIntOrNull() == null
                )

                Text(
                    text = "Códigos por página: $codesPerPage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startIdInt = startId.toIntOrNull()
                    val countInt = count.toIntOrNull()

                    if (startIdInt != null && countInt != null && countInt > 0) {
                        onGenerate(startIdInt, countInt)
                        onDismiss()
                    }
                },
                enabled = startId.toIntOrNull() != null &&
                        count.toIntOrNull() != null &&
                        count.toIntOrNull() ?: 0 > 0
            ) {
                Text("Generar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}