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

package com.mardous.booming.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mardous.booming.R
import com.mardous.booming.databinding.DialogUpdateInfoBinding
import com.mardous.booming.extensions.openUrl
import com.mardous.booming.extensions.resources.setMarkdownText
import com.mardous.booming.extensions.showToast
import com.mardous.booming.http.github.GitHubRelease
import com.mardous.booming.viewmodels.update.UpdateViewModel
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class UpdateDialog : BottomSheetDialogFragment(), View.OnClickListener {

    private var _binding: DialogUpdateInfoBinding? = null
    private val binding get() = _binding!!
    private val viewModel by activityViewModel<UpdateViewModel>()

    private var release: GitHubRelease? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        release = viewModel.latestRelease
        if (release != null && release!!.isNewer(requireContext())) {
            _binding = DialogUpdateInfoBinding.inflate(layoutInflater)
            binding.infoAction.setOnClickListener(this)
            binding.downloadAction.setOnClickListener(this)
            binding.versionName.text = release!!.name
            if (release!!.body.isNotEmpty()) {
                binding.versionInfo.setMarkdownText(release!!.body)
            } else {
                binding.versionInfo.isVisible = false
            }
            return BottomSheetDialog(requireContext()).also {
                it.setContentView(binding.root)
            }
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.the_app_is_up_to_date)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        release?.setIgnored()
    }

    override fun onClick(view: View) {
        when (view) {
            binding.infoAction -> {
                release?.let {
                    requireContext().openUrl(it.url)
                }
            }

            binding.downloadAction -> {
                release?.let {
                    viewModel.downloadUpdate(requireContext(), it)
                    showToast(R.string.downloading_update)
                }
                dismiss()
            }
        }
    }
}