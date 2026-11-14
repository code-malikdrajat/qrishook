package com.qrishook.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qrishook.app.R
import com.qrishook.app.ui.theme.Spacing

/** Baris header ringkas seperti editor MacroDroid: key / value / hapus. */
@Composable
fun HeaderRow(
    key: String,
    value: String,
    onKeyChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = key,
            onValueChange = onKeyChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.header_key)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small,
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(R.string.header_value)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            shape = MaterialTheme.shapes.small,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.cd_delete_header),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeaderRowPreview() {
    var key by remember { mutableStateOf("x-secret-token") }
    var value by remember { mutableStateOf("sec_xxx") }
    MaterialTheme {
        HeaderRow(
            key = key,
            value = value,
            onKeyChange = { key = it },
            onValueChange = { value = it },
            onDelete = {},
        )
    }
}
