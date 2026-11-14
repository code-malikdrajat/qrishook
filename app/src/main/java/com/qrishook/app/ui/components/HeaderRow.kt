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
        QRISHookTextField(
            value = key,
            onValueChange = onKeyChange,
            label = stringResource(R.string.header_key),
            modifier = Modifier.weight(1f),
        )
        QRISHookTextField(
            value = value,
            onValueChange = onValueChange,
            label = stringResource(R.string.header_value),
            modifier = Modifier.weight(1f),
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
    var key by remember { mutableStateOf("Content-Type") }
    var value by remember { mutableStateOf("application/json") }
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
