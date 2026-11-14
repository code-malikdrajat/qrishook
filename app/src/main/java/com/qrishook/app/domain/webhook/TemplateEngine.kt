package com.qrishook.app.domain.webhook

/**
 * Render placeholder `{{key}}` dari [context]. Key yang tidak ada dibiarkan apa adanya.
 * [escapeJson] dipakai untuk payload JSON supaya kutip/newline dari isi notifikasi tidak merusak body.
 */
object TemplateEngine {
    private val placeholder = Regex("""\{\{\s*([^{}]+?)\s*\}\}""")

    fun render(
        template: String,
        context: Map<String, String>,
        escapeJson: Boolean = false,
    ): String = placeholder.replace(template) { match ->
        val value = context[match.groupValues[1]] ?: return@replace match.value
        if (escapeJson) escapeJsonValue(value) else value
    }

    private fun escapeJsonValue(value: String): String = buildString(value.length + 8) {
        value.forEach { c ->
            when (c) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
        }
    }
}
