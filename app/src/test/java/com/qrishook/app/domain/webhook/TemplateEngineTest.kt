package com.qrishook.app.domain.webhook

import org.junit.Assert.assertEquals
import org.junit.Test

class TemplateEngineTest {
    private val context = mapOf(
        "title" to "QRIS Diterima",
        "text" to "Anda menerima Rp 50.000",
        "app" to "com.gojek.app",
    )

    @Test
    fun basicReplace() {
        assertEquals("QRIS Diterima", TemplateEngine.render("{{title}}", context))
    }

    @Test
    fun multiplePlaceholders() {
        assertEquals(
            "com.gojek.app: QRIS Diterima - Anda menerima Rp 50.000",
            TemplateEngine.render("{{app}}: {{title}} - {{text}}", context),
        )
    }

    @Test
    fun missingKeyIsLeftAsIs() {
        assertEquals("Halo {{unknown}}", TemplateEngine.render("Halo {{unknown}}", context))
    }

    @Test
    fun emptyContextLeavesTemplate() {
        assertEquals(
            """{"event":"transaction","title":"{{title}}"}""",
            TemplateEngine.render("""{"event":"transaction","title":"{{title}}"}""", emptyMap()),
        )
    }

    @Test
    fun whitespaceInsideBracesIsTrimmed() {
        assertEquals("QRIS Diterima", TemplateEngine.render("{{  title  }}", context))
    }

    @Test
    fun jsonTemplateEscapesInjectedValues() {
        val escaped = TemplateEngine.render(
            template = """{"event":"transaction","text":"{{text}}"}""",
            context = mapOf("text" to "Dia bilang \"halo\"\nbaris 2"),
            escapeJson = true,
        )
        assertEquals("""{"event":"transaction","text":"Dia bilang \"halo\"\nbaris 2"}""", escaped)
    }

    @Test
    fun nonJsonTemplateStaysRaw() {
        assertEquals(
            "line1\nline2",
            TemplateEngine.render("{{text}}", mapOf("text" to "line1\nline2")),
        )
    }
}
