/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import org.mozilla.fenix.FeatureFlags
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.Event
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.requireComponents
import org.mozilla.fenix.ext.settings
import org.mozilla.fenix.ext.showToolbar
import org.mozilla.fenix.utils.view.addToRadioGroup

/**
 * Lets the user customize the UI.
 */

@Suppress("TooManyFunctions")
class CustomizationFragment : PreferenceFragmentCompat() {
    private lateinit var radioLightTheme: RadioButtonPreference
    private lateinit var radioDarkTheme: RadioButtonPreference
    private lateinit var radioAutoBatteryTheme: RadioButtonPreference
    private lateinit var radioFollowDeviceTheme: RadioButtonPreference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.customization_preferences, rootKey)

        requirePreference<SwitchPreference>(R.string.pref_key_strip_url).apply {
            isChecked = context.settings().shouldStripUrl

            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
    }

    override fun onResume() {
        super.onResume()
        showToolbar(getString(R.string.preferences_customize))
        setupPreferences()
    }

    private fun setupPreferences() {
        bindFollowDeviceTheme()
        bindDarkTheme()
        bindLightTheme()
        bindAutoBatteryTheme()
        setupRadioGroups()
        setupToolbarCategory()
        setupHomeCategory()
        setupAddonsCustomizationCategory()
        setupTabTrayDirectionCategory()
        setupTabTrayNewTabType()
    }

    private fun setupRadioGroups() {
        addToRadioGroup(
            radioLightTheme,
            radioDarkTheme,
            if (SDK_INT >= Build.VERSION_CODES.P) {
                radioFollowDeviceTheme
            } else {
                radioAutoBatteryTheme
            }
        )
    }

    private fun bindLightTheme() {
        radioLightTheme = requirePreference(R.string.pref_key_light_theme)
        radioLightTheme.onClickListener {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    @SuppressLint("WrongConstant")
    // Suppressing erroneous lint warning about using MODE_NIGHT_AUTO_BATTERY, a likely library bug
    private fun bindAutoBatteryTheme() {
        radioAutoBatteryTheme = requirePreference(R.string.pref_key_auto_battery_theme)
        radioAutoBatteryTheme.onClickListener {
            setNewTheme(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
        }
    }

    private fun bindDarkTheme() {
        radioDarkTheme = requirePreference(R.string.pref_key_dark_theme)
        radioDarkTheme.onClickListener {
            requireContext().components.analytics.metrics.track(
                Event.DarkThemeSelected(
                    Event.DarkThemeSelected.Source.SETTINGS
                )
            )
            setNewTheme(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun bindFollowDeviceTheme() {
        radioFollowDeviceTheme = requirePreference(R.string.pref_key_follow_device_theme)
        if (SDK_INT >= Build.VERSION_CODES.P) {
            radioFollowDeviceTheme.onClickListener {
                setNewTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    private fun setNewTheme(mode: Int) {
        if (AppCompatDelegate.getDefaultNightMode() == mode) return
        AppCompatDelegate.setDefaultNightMode(mode)
        activity?.recreate()
        with(requireComponents.core) {
            engine.settings.preferredColorScheme = getPreferredColorScheme()
        }
        requireComponents.useCases.sessionUseCases.reload.invoke()
    }

    private fun setupToolbarCategory() {
        val topPreference = requirePreference<RadioButtonPreference>(R.string.pref_key_toolbar_top)
        topPreference.onClickListener {
            requireContext().components.analytics.metrics.track(Event.ToolbarPositionChanged(
                Event.ToolbarPositionChanged.Position.TOP
            ))
        }

        val bottomPreference = requirePreference<RadioButtonPreference>(R.string.pref_key_toolbar_bottom)
        bottomPreference.onClickListener {
            requireContext().components.analytics.metrics.track(Event.ToolbarPositionChanged(
                Event.ToolbarPositionChanged.Position.BOTTOM
            ))
        }

        val toolbarPosition = requireContext().settings().toolbarPosition
        topPreference.setCheckedWithoutClickListener(toolbarPosition == ToolbarPosition.TOP)
        bottomPreference.setCheckedWithoutClickListener(toolbarPosition == ToolbarPosition.BOTTOM)

        addToRadioGroup(topPreference, bottomPreference)
    }

    private fun setupHomeCategory() {
        requirePreference<PreferenceCategory>(R.string.pref_home_category).apply {
            isVisible = FeatureFlags.topFrecentSite
        }
        requirePreference<SwitchPreference>(R.string.pref_key_enable_top_frecent_sites).apply {
            isVisible = FeatureFlags.topFrecentSite
            isChecked = context.settings().showTopFrecentSites
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
    }

    private fun setupAddonsCustomizationCategory() {
        requirePreference<EditTextPreference>(R.string.pref_key_addons_custom_account).apply {
            text = context.settings().customAddonsAccount
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        requirePreference<EditTextPreference>(R.string.pref_key_addons_custom_collection).apply {
            text = context.settings().customAddonsCollection
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
    }

    private fun setupTabTrayDirectionCategory() {
        val alwaysTop = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_always_top).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        val alwaysBottom = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_always_bottom).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        val sameDirection = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_same_direction).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        val oppositeDirection = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_opposite_direction).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        val settings = requireContext().settings()
        alwaysTop.setCheckedWithoutClickListener(settings.tabTrayAlwaysTop)
        alwaysBottom.setCheckedWithoutClickListener(settings.tabTrayAlwaysBottom)
        sameDirection.setCheckedWithoutClickListener(settings.tabTraySameDirection)
        oppositeDirection.setCheckedWithoutClickListener(settings.tabTrayOppositeDirection)

        addToRadioGroup(sameDirection, oppositeDirection, alwaysTop, alwaysBottom)
    }

    private fun setupTabTrayNewTabType() {
        val fab = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_new_tab_fab).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }
        val bar = requirePreference<RadioButtonPreference>(R.string.pref_key_tab_tray_new_tab_bar).apply {
            onPreferenceChangeListener = SharedPreferenceUpdater()
        }

        val settings = requireContext().settings()
        fab.setCheckedWithoutClickListener(settings.useNewTabFab)
        bar.setCheckedWithoutClickListener(settings.useNewTabBar)
        addToRadioGroup(fab, bar)
    }
}
