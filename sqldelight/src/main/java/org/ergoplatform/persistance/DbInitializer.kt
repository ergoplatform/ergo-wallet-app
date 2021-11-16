package org.ergoplatform.persistance

import com.squareup.sqldelight.db.SqlDriver
import org.ergoplatform.utils.LogUtils


object DbInitializer {
    fun initDbSchema(driver: SqlDriver) {
        val currentVer: Int = getVersion(driver)
        val latestVersion = AppDatabase.Schema.version
        if (currentVer == 0) {
            AppDatabase.Schema.create(driver)
            setVersion(driver, latestVersion)
            LogUtils.logDebug("Database", "init: created tables, setVersion to $latestVersion")
        } else if (latestVersion > currentVer) {
            AppDatabase.Schema.migrate(driver, currentVer, latestVersion)
            setVersion(driver, latestVersion)
            LogUtils.logDebug("Database", "migrated from $currentVer to $latestVersion")
        } else {
            LogUtils.logDebug("Database", "initialized, already on latest schemea $latestVersion")
        }
    }

    private fun getVersion(driver: SqlDriver): Int {
        val sqlCursor = driver.executeQuery(null, "PRAGMA user_version;", 0, null);
        return sqlCursor.use { sqlCursor.getLong(0)!!.toInt() }
    }

    private fun setVersion(driver: SqlDriver, version: Int) {
        driver.execute(null, String.format("PRAGMA user_version = %d;", version), 0, null)
    }
}