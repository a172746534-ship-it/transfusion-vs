package app.transfusionvs.setup

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** One manual step. [intents] are tried in order; an empty list means "no button" (do it by hand). */
data class OemStep(val text: String, val intents: List<Intent> = emptyList())

data class OemGuide(val brand: String, val steps: List<OemStep>)

/**
 * Extra steps for phone brands whose battery savers kill alarm apps. Deep links point to each
 * brand's own settings pages where they exist; they change between OS versions, so every button
 * falls back to the app's info page (SetupChecks.open).
 */
object OemGuides {

    fun forThisPhone(context: Context): OemGuide {
        val id = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        val pkg = context.packageName
        val appInfo = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
        fun component(p: String, c: String) = Intent().setComponent(ComponentName(p, c))

        val recents = OemStep("Lock the app in Recents: open Recents, then long-press (or pull down) its card and tap the lock icon.")

        return when {
            listOf("xiaomi", "redmi", "poco").any { it in id } -> OemGuide(
                "Xiaomi / Redmi / POCO",
                listOf(
                    OemStep(
                        "Autostart → turn ON for Transfusion VS.",
                        listOf(
                            component("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
                            appInfo,
                        ),
                    ),
                    OemStep(
                        "Battery saver → choose \"No restrictions\".",
                        listOf(
                            component("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
                                .putExtra("package_name", pkg).putExtra("package_label", "Transfusion VS"),
                            appInfo,
                        ),
                    ),
                    OemStep(
                        "Other permissions → allow \"Show on Lock screen\" and \"Display pop-up windows while running in the background\".",
                        listOf(
                            Intent("miui.intent.action.APP_PERM_EDITOR")
                                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                                .putExtra("extra_pkgname", pkg),
                            Intent("miui.intent.action.APP_PERM_EDITOR")
                                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                                .putExtra("extra_pkgname", pkg),
                            appInfo,
                        ),
                    ),
                    recents,
                ),
            )

            "samsung" in id -> OemGuide(
                "Samsung",
                listOf(
                    OemStep("App info → Battery → choose \"Unrestricted\".", listOf(appInfo)),
                    OemStep(
                        "Battery → Background usage limits → make sure the app is NOT in \"Sleeping\" or \"Deep sleeping apps\" (add it to \"Never sleeping apps\").",
                        listOf(
                            component("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
                            component("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
                        ),
                    ),
                ),
            )

            listOf("oppo", "realme", "oneplus").any { it in id } -> OemGuide(
                "Oppo / Realme / OnePlus",
                listOf(
                    OemStep("App info → Battery usage → allow background activity / \"Don't optimize\".", listOf(appInfo)),
                    OemStep(
                        "Auto launch (Startup manager) → turn ON for Transfusion VS.",
                        listOf(
                            component("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                            component("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                            component("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
                            component("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
                        ),
                    ),
                    recents,
                ),
            )

            listOf("vivo", "iqoo").any { it in id } -> OemGuide(
                "Vivo / iQOO",
                listOf(
                    OemStep(
                        "Autostart (Background startup) → turn ON for Transfusion VS.",
                        listOf(
                            component("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                            component("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
                        ),
                    ),
                    OemStep("App info → Battery → \"Allow high background power consumption\".", listOf(appInfo)),
                    recents,
                ),
            )

            listOf("infinix", "tecno", "itel").any { it in id } -> OemGuide(
                "Infinix / Tecno",
                listOf(
                    OemStep(
                        "Phone Master → Auto-start management → turn ON for Transfusion VS.",
                        listOf(component("com.transsion.phonemaster", "com.cyin.himgr.autostart.AutoStartActivity")),
                    ),
                    OemStep("App info → Battery → allow background activity / \"No restriction\".", listOf(appInfo)),
                    recents,
                ),
            )

            listOf("huawei", "honor").any { it in id } -> OemGuide(
                "Huawei / Honor",
                listOf(
                    OemStep(
                        "App launch → turn OFF \"Manage automatically\", then turn ON Auto-launch, Secondary launch and Run in background.",
                        listOf(
                            component("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                            component("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                            component("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
                        ),
                    ),
                    recents,
                ),
            )

            else -> OemGuide(
                Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
                listOf(OemStep("App info → Battery → choose \"Unrestricted\" (or \"Don't optimize\").", listOf(appInfo))),
            )
        }
    }
}
