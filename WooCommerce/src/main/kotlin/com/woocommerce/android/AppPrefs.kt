@file:Suppress("SameParameterValue")

package com.woocommerce.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.woocommerce.android.AppPrefs.DeletablePrefKey.DATABASE_DOWNGRADED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.IMAGE_OPTIMIZE_ENABLED
import com.woocommerce.android.AppPrefs.DeletablePrefKey.STATS_WIDGET_COLOR_MODE
import com.woocommerce.android.AppPrefs.DeletablePrefKey.STATS_WIDGET_SELECTED_SITE_ID
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.PreferenceUtils
import com.woocommerce.android.util.ThemeOption
import com.woocommerce.android.util.ThemeOption.DEFAULT
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T

// Guaranteed to hold a reference to the application context, which is safe
@SuppressLint("StaticFieldLeak")
object AppPrefs {
    interface PrefKey

    private lateinit var context: Context

    /**
     * Application related preferences. When the user logs out, these preferences are erased.
     */
    private enum class DeletablePrefKey : PrefKey {
        SUPPORT_EMAIL,
        SUPPORT_NAME,
        IS_USING_V4_API,
        HAS_UNSEEN_REVIEWS,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME,
        SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM,
        LOGIN_SITE_ADDRESS,
        DATABASE_DOWNGRADED,
        SHOULD_DISPLAY_V4_STATS_AVAILABILITY_BANNER,
        SHOULD_DISPLAY_V4_STATS_REVERTED_BANNER,
        IS_V4_STATS_UI_ENABLED,
        IS_PRODUCTS_FEATURE_ENABLED,
        LOGIN_USER_BYPASSED_JETPACK_REQUIRED,
        SELECTED_ORDER_LIST_TAB_POSITION,
        IMAGE_OPTIMIZE_ENABLED,
        SELECTED_APP_THEME,

        // Widget settings
        STATS_WIDGET_SELECTED_SITE_ID,
        STATS_WIDGET_COLOR_MODE
    }

    /**
     * These preferences won't be deleted when the user disconnects.
     * They should be used for device specific or user-independent preferences.
     */
    private enum class UndeletablePrefKey : PrefKey {
        // The last stored versionCode of the app
        LAST_APP_VERSION_CODE,
        // Whether or not automatic crash reporting is enabled
        ENABLE_CRASH_REPORTING,
        // Enable notifications for new orders
        NOTIFS_ORDERS_ENABLED,
        // Enable notifications for new reviews
        NOTIFS_REVIEWS_ENABLED,
        // Play cha-ching sound on new order notifications
        NOTIFS_ORDERS_CHA_CHING_ENABLED,
        // Number of times the "mark all notifications read" icon was tapped
        NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN,
        // The app update for this version was cancelled by the user
        CANCELLED_APP_VERSION_CODE,
        // Application permissions
        ASKED_PERMISSION_CAMERA
    }

    fun init(context: Context) {
        AppPrefs.context = context.applicationContext
    }

    fun getLastAppVersionCode(): Int {
        return getDeletableInt(UndeletablePrefKey.LAST_APP_VERSION_CODE)
    }

    fun setLastAppVersionCode(versionCode: Int) {
        setDeletableInt(UndeletablePrefKey.LAST_APP_VERSION_CODE, versionCode)
    }

    fun getCancelledAppVersionCode(): Int {
        return getDeletableInt(UndeletablePrefKey.CANCELLED_APP_VERSION_CODE)
    }

    fun setCancelledAppVersionCode(versionCode: Int) {
        setDeletableInt(UndeletablePrefKey.CANCELLED_APP_VERSION_CODE, versionCode)
    }

    fun setSupportEmail(email: String?) {
        if (!email.isNullOrEmpty()) {
            setString(DeletablePrefKey.SUPPORT_EMAIL, email)
        } else {
            remove(DeletablePrefKey.SUPPORT_EMAIL)
        }
    }

    fun getSupportEmail() = getString(DeletablePrefKey.SUPPORT_EMAIL)

    fun hasSupportEmail() = getSupportEmail().isNotEmpty()

    fun removeSupportEmail() {
        remove(DeletablePrefKey.SUPPORT_EMAIL)
    }

    fun setSupportName(name: String) {
        setString(DeletablePrefKey.SUPPORT_NAME, name)
    }

    fun getSupportName() = getString(DeletablePrefKey.SUPPORT_NAME)

    fun removeSupportName() {
        remove(DeletablePrefKey.SUPPORT_NAME)
    }

    /**
     * Method to check if the v4 stats UI is supported.
     * i.e. if the Woocommerce Admin plugin is installed/active on the site AND
     * if the user has elected to try out the new stats UI
     */
    fun isV4StatsUISupported() = isUsingV4Api() && isV4StatsUIEnabled()

    fun isUsingV4Api() = getBoolean(DeletablePrefKey.IS_USING_V4_API, false)

    fun setIsUsingV4Api(isUsingV4Api: Boolean) = setBoolean(DeletablePrefKey.IS_USING_V4_API, isUsingV4Api)

    /**
     * Flag to check if the user chooses to continue using the old stats, even when wc-admin is available,
     * by clicking the `No thanks` button in the [com.woocommerce.android.ui.mystore.MyStoreStatsAvailabilityCard]
     */
    fun isV4StatsUIEnabled() = getBoolean(DeletablePrefKey.IS_V4_STATS_UI_ENABLED, false)

    fun setIsV4StatsUIEnabled(isV4StatsUIEnabled: Boolean) =
            setBoolean(DeletablePrefKey.IS_V4_STATS_UI_ENABLED, isV4StatsUIEnabled)

    fun setStatsWidgetSelectedSiteId(siteId: Long, appWidgetId: Int) {
        setLong(getSiteIdWidgetKey(appWidgetId), siteId)
    }

    fun getStatsWidgetSelectedSiteId(appWidgetId: Int): Long {
        return getLong(getSiteIdWidgetKey(appWidgetId), -1)
    }

    fun removeStatsWidgetSelectedSiteId(appWidgetId: Int) {
        remove(getSiteIdWidgetKey(appWidgetId))
    }

    private fun getSiteIdWidgetKey(appWidgetId: Int): String {
        return STATS_WIDGET_SELECTED_SITE_ID.name + appWidgetId
    }

    fun setStatsWidgetColorModeId(colorModeId: Int, appWidgetId: Int) {
        setInt(getColorModeIdWidgetKey(appWidgetId), colorModeId)
    }

    fun getStatsWidgetColorModeId(appWidgetId: Int): Int {
        return getInt(getColorModeIdWidgetKey(appWidgetId), -1)
    }

    fun removeStatsWidgetColorModeId(appWidgetId: Int) {
        remove(getColorModeIdWidgetKey(appWidgetId))
    }

    private fun getColorModeIdWidgetKey(appWidgetId: Int): String {
        return STATS_WIDGET_COLOR_MODE.name + appWidgetId
    }

    /**
     * Flag to check products features are enabled
     */
    fun isProductsFeatureEnabled() = getBoolean(DeletablePrefKey.IS_PRODUCTS_FEATURE_ENABLED, false)

    fun setIsProductsFeatureEnabled(isProductsFeatureEnabled: Boolean) =
            setBoolean(DeletablePrefKey.IS_PRODUCTS_FEATURE_ENABLED, isProductsFeatureEnabled)

    /**
     * Flag to check if the user has already chosen to try out or dismissed the new stats.
     * The [com.woocommerce.android.ui.mystore.MyStoreStatsAvailabilityCard]
     * will no longer be displayed if this flag returns false
     */
    fun shouldDisplayV4StatsAvailabilityBanner() =
            getBoolean(DeletablePrefKey.SHOULD_DISPLAY_V4_STATS_AVAILABILITY_BANNER, true)

    fun setShouldDisplayV4StatsAvailabilityBanner(shouldDisplayAvailabilityBanner: Boolean) =
            setBoolean(DeletablePrefKey.SHOULD_DISPLAY_V4_STATS_AVAILABILITY_BANNER, shouldDisplayAvailabilityBanner)

    /**
     * Flag to check if the v4 stats API is no longer supported for a site AND if the user has already dismissed the
     * [com.woocommerce.android.ui.mystore.MyStoreStatsRevertedNoticeCard]
     */
    fun shouldDisplayV4StatsRevertedBanner() =
            getBoolean(DeletablePrefKey.SHOULD_DISPLAY_V4_STATS_REVERTED_BANNER, false)

    fun setShouldDisplayV4StatsRevertedBanner(shouldDisplayV4StatsRevertedBanner: Boolean) =
            setBoolean(DeletablePrefKey.SHOULD_DISPLAY_V4_STATS_REVERTED_BANNER, shouldDisplayV4StatsRevertedBanner)

    fun isCrashReportingEnabled(): Boolean {
        // default to False for debug builds
        val default = !BuildConfig.DEBUG
        return getBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, default)
    }

    fun setCrashReportingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.ENABLE_CRASH_REPORTING, enabled)
    }

    fun isOrderNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, true)

    fun setOrderNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_ENABLED, enabled)
    }

    fun isReviewNotificationsEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, true)

    fun setReviewNotificationsEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_REVIEWS_ENABLED, enabled)
    }

    fun isOrderNotificationsChaChingEnabled() = getBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, true)

    fun setOrderNotificationsChaChingEnabled(enabled: Boolean) {
        setBoolean(UndeletablePrefKey.NOTIFS_ORDERS_CHA_CHING_ENABLED, enabled)
    }

    fun getHasUnseenReviews() = getBoolean(DeletablePrefKey.HAS_UNSEEN_REVIEWS, false)

    fun setHasUnseenReviews(hasUnseen: Boolean) {
        setBoolean(DeletablePrefKey.HAS_UNSEEN_REVIEWS, hasUnseen)
    }

    fun getNumTimesMarkAllReadSnackShown(): Int =
            getInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, 0)

    fun incNumTimesMarkAllReadSnackShown() {
        val numTimesShown = getNumTimesMarkAllReadSnackShown() + 1
        setInt(UndeletablePrefKey.NUM_TIMES_MARK_ALL_NOTIFS_READ_SNACK_SHOWN, numTimesShown)
    }

    fun getSelectedShipmentTrackingProviderName(): String =
            getString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME)

    fun setSelectedShipmentTrackingProviderName(providerName: String) {
        setString(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_NAME, providerName)
    }

    fun getIsSelectedShipmentTrackingProviderCustom(): Boolean =
            getBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, false)

    fun setIsSelectedShipmentTrackingProviderNameCustom(isCustomProvider: Boolean) {
        setBoolean(DeletablePrefKey.SELECTED_SHIPMENT_TRACKING_PROVIDER_IS_CUSTOM, isCustomProvider)
    }

    fun setLoginSiteAddress(loginSiteAddress: String) {
        setString(DeletablePrefKey.LOGIN_SITE_ADDRESS, loginSiteAddress)
    }

    fun getLoginSiteAddress() = getString(DeletablePrefKey.LOGIN_SITE_ADDRESS)

    fun removeLoginSiteAddress() {
        remove(DeletablePrefKey.LOGIN_SITE_ADDRESS)
    }

    fun setLoginUserBypassedJetpackRequired(bypassedLogin: Boolean = true) {
        setBoolean(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED, bypassedLogin)
    }

    fun getLoginUserBypassedJetpackRequired() =
            getBoolean(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED, false)

    fun removeLoginUserBypassedJetpackRequired() {
        remove(DeletablePrefKey.LOGIN_USER_BYPASSED_JETPACK_REQUIRED)
    }

    fun getDatabaseDowngraded() = getBoolean(DATABASE_DOWNGRADED, false)

    fun setDatabaseDowngraded(value: Boolean) {
        setBoolean(DATABASE_DOWNGRADED, value)
    }

    fun setSelectedOrderListTab(selectedOrderListTabPosition: Int) {
        setInt(DeletablePrefKey.SELECTED_ORDER_LIST_TAB_POSITION, selectedOrderListTabPosition)
    }

    fun getSelectedOrderListTabPosition() =
            getInt(DeletablePrefKey.SELECTED_ORDER_LIST_TAB_POSITION, -1)

    /**
     * Checks if the user has a saved order list tab position yet. If no position has been saved,
     * then the value will be the default of -1.
     *
     * @return True if the saved position is not the default -1, else false
     */
    fun hasSelectedOrderListTabPosition() = getSelectedOrderListTabPosition() > -1

    fun getImageOptimizationEnabled() = getBoolean(IMAGE_OPTIMIZE_ENABLED, true)

    fun setImageOptimizationEnabled(enabled: Boolean) {
        setBoolean(IMAGE_OPTIMIZE_ENABLED, enabled)
    }

    fun getAppTheme(): ThemeOption =
            ThemeOption.valueOf(getString(DeletablePrefKey.SELECTED_APP_THEME, DEFAULT.toString()))

    fun setAppTheme(theme: ThemeOption) {
        setString(DeletablePrefKey.SELECTED_APP_THEME, theme.toString())
    }

    /**
     * Remove all user-related preferences.
     */
    fun reset() {
        val editor = getPreferences().edit()
        DeletablePrefKey.values().forEach { editor.remove(it.name) }
        editor.remove(SelectedSite.SELECTED_SITE_LOCAL_ID)
        editor.apply()
    }

    private fun getInt(key: PrefKey, default: Int = 0) =
            PreferenceUtils.getInt(getPreferences(), key.toString(), default)

    private fun getInt(key: String, default: Int = 0) =
        PreferenceUtils.getInt(getPreferences(), key, default)

    private fun setInt(key: PrefKey, value: Int) =
            PreferenceUtils.setInt(getPreferences(), key.toString(), value)

    private fun setInt(key: String, value: Int) =
        PreferenceUtils.setInt(getPreferences(), key, value)

    private fun getString(key: PrefKey, defaultValue: String = ""): String {
        return PreferenceUtils.getString(getPreferences(), key.toString(), defaultValue)?.let {
            it
        } ?: defaultValue
    }

    private fun setString(key: PrefKey, value: String) =
            PreferenceUtils.setString(getPreferences(), key.toString(), value)

    private fun setLong(key: String, value: Long) =
        PreferenceUtils.setLong(getPreferences(), key, value)

    private fun getLong(key: String, default: Long = 0L) =
        PreferenceUtils.getLong(getPreferences(), key, default)

    fun getBoolean(key: PrefKey, default: Boolean) =
            PreferenceUtils.getBoolean(getPreferences(), key.toString(), default)

    fun setBoolean(key: PrefKey, value: Boolean = false) =
            PreferenceUtils.setBoolean(getPreferences(), key.toString(), value)

    private fun getPreferences() = PreferenceManager.getDefaultSharedPreferences(context)

    private fun remove(key: PrefKey) {
        getPreferences().edit().remove(key.toString()).apply()
    }

    private fun remove(key: String) {
        getPreferences().edit().remove(key).apply()
    }

    fun exists(key: PrefKey) = getPreferences().contains(key.toString())

    /**
     * Methods used to store values in SharedPreferences that are not backed up
     * when app is installed/uninstalled. Currently, only used for storing appVersionCode.
     * We might want to migrate this to it's own class if we are to use this for other
     * attributes as well.
     */
    private fun getDeletableInt(key: PrefKey, default: Int = 0) =
            PreferenceUtils.getInt(getDeleteablePreferences(), key.toString(), default)

    private fun setDeletableInt(key: PrefKey, value: Int) =
            PreferenceUtils.setInt(getDeleteablePreferences(), key.toString(), value)

    private fun getDeleteablePreferences(): SharedPreferences {
        return context.getSharedPreferences(
                "${context.packageName}_deletable_preferences",
                Context.MODE_PRIVATE
        )
    }

    /*
     * key in shared preferences which stores a boolean telling whether the app has already
     * asked for the passed permission
     */
    fun getPermissionAskedKey(permission: String): PrefKey? {
        when (permission) {
            android.Manifest.permission.CAMERA ->
                return UndeletablePrefKey.ASKED_PERMISSION_CAMERA
            else -> {
                WooLog.w(T.UTILS, "No key for requested permission: $permission")
                return null
            }
        }
    }
}
