package com.qrishook.app.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun PlaceholderChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = text, style = MaterialTheme.typography.labelLarge) },
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderChipPreview() {
    MaterialTheme { PlaceholderChip(text = "{{title}}", onClick = {}) }
}
