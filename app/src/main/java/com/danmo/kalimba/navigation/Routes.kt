// 文件名: navigation/Routes.kt
package com.danmo.kalimba.navigation

sealed class Screen(val route: String) {
    data object Main : Screen("main")
    data object Practice : Screen("practice")
    data object NmnEdit : Screen("nmn_edit?sheetId={sheetId}") {
        fun createRoute(sheetId: Long? = null) =
            if (sheetId != null) "nmn_edit?sheetId=$sheetId" else "nmn_edit"
    }
    data object SheetList : Screen("sheet_list")
    data object Settings : Screen("settings")
}