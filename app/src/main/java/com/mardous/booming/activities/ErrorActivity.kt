/*
 * Copyright (c) 2024 Christians Martínez Alvarado
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mardous.booming.activities

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.core.content.getSystemService
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import com.mardous.booming.App
import com.mardous.booming.R
import com.mardous.booming.activities.base.AbsThemeActivity
import com.mardous.booming.databinding.ActivityErrorBinding
import com.mardous.booming.extensions.applyWindowInsets
import com.mardous.booming.extensions.fileProviderAuthority
import com.mardous.booming.extensions.files.asFormattedFileTime
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.extensions.showToast
import com.mardous.booming.ui.screens.about.ISSUE_TRACKER_LINK
import java.io.File

/**
 * @author Christians M. A. (mardous)
 */
class ErrorActivity : AbsThemeActivity() {

    private var _binding: ActivityErrorBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = CustomActivityOnCrash.getConfigFromIntent(intent)
        if (config == null) {
            finish()
            return
        }
        val errorReport = CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        val nameFromTime = System.currentTimeMillis().asFormattedFileTime()
        val errorReportFile = File(App.appContext.filesDir, "Crash_${nameFromTime}.log")
        if (!errorReportFile.exists() || errorReportFile.delete()) {
            errorReportFile.writeText(errorReport)
        }

        _binding = ActivityErrorBinding.inflate(layoutInflater)
        binding.root.applyWindowInsets(top = true, left = true, right = true, bottom = true)
        binding.errorReportText.text = errorReport
        binding.openGithub.setOnClickListener {
            openGithub(errorReport)
        }
        binding.sendReport.setOnClickListener {
            sendFile(errorReportFile)
        }
        binding.restartApp.setOnClickListener {
            CustomActivityOnCrash.restartApplication(this, config)
        }
        setContentView(binding.root)
    }

    private fun openGithub(report: String) {
        val clipboardManager = getSystemService<ClipboardManager>()
        val clipData = ClipData.newPlainText(getString(R.string.uncaught_error_report), report)
        clipboardManager?.setPrimaryClip(clipData)
        openUrl(CREATE_ISSUE_URL)
        showToast(R.string.uncaught_error_report_copied)
    }

    private fun sendFile(file: File) {
        val intent = ShareCompat.IntentBuilder(this)
            .setSubject("${getString(R.string.app_name)} - crash log")
            .setText("Please, add a description of the problem")
            .setType("*/*")
            .setStream(FileProvider.getUriForFile(this, fileProviderAuthority, file))
            .setChooserTitle(R.string.uncaught_error_send)
            .createChooserIntent()

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val CREATE_ISSUE_URL = "$ISSUE_TRACKER_LINK/new?template=bug_report.yaml"
    }
}
