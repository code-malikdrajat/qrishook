package com.qrishook.app.navigation

/** Navigation routes — UI_SPEC section 12. */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SETTINGS = "settings"

    // wizard?automationId={id} — absent id = create, present = edit
    const val WIZARD_ARG_ID = "automationId"
    const val WIZARD = "wizard?$WIZARD_ARG_ID={$WIZARD_ARG_ID}"
    fun wizard(automationId: Long? = null): String =
        if (automationId == null) "wizard" else "wizard?$WIZARD_ARG_ID=$automationId"

    const val AUTOMATION_ARG_ID = "id"
    const val AUTOMATION = "automation/{$AUTOMATION_ARG_ID}"
    fun automation(id: Long): String = "automation/$id"

    // logs?automationId={id} — absent id = all logs
    const val LOGS = "logs?$WIZARD_ARG_ID={$WIZARD_ARG_ID}"
    fun logs(automationId: Long? = null): String =
        if (automationId == null) "logs" else "logs?$WIZARD_ARG_ID=$automationId"

    const val LOG_ARG_ID = "id"
    const val LOG = "log/{$LOG_ARG_ID}"
    fun log(id: Long): String = "log/$id"
}
