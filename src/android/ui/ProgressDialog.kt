package org.apache.cordova.plugin.ui

import android.app.Activity
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView

class ProgressDialogController(private val activity: Activity) {
	private var dialog: AlertDialog? = null
	private var progressBar: ProgressBar? = null
	private var progressText: TextView? = null
	private var sizeText: TextView? = null

	val isShown: Boolean
	    get() {
		    return dialog?.isShowing == true
	    }

	fun show() {
		activity.runOnUiThread {
			val layoutId = activity.resources.getIdentifier(
				"progress_dialog",
				"layout",
				activity.packageName
			)
			val view = LayoutInflater.from(activity).inflate(layoutId, null)
			progressBar = view.findViewById(
				activity.resources.getIdentifier(
					"progressBar",
					"id",
					activity.packageName
				)
			)
			progressText = view.findViewById(
				activity.resources.getIdentifier(
					"progressText",
					"id",
					activity.packageName
				)
			)
			sizeText = view.findViewById(
				activity.resources.getIdentifier(
					"sizeText",
					"id",
					activity.packageName
				)
			)
			dialog = AlertDialog.Builder(activity)
				.setView(view)
				.setCancelable(false)
				.create()
			dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
			dialog?.show()
		}
	}

	fun update(percent: Int, downloadedMb: Double, totalMb: Double) {
		if (!isShown) {
			show()
		}
		activity.runOnUiThread {
			progressBar?.progress = percent
			progressText?.text = "$percent%"
			sizeText?.text =
			String.format("%.1f МБ из %.1f МБ", downloadedMb, totalMb)
		}
	}

	fun complete() {
		activity.runOnUiThread {
			progressText?.text = "Установка..."
			sizeText?.text = ""
		}
	}

	fun hide() {
		activity.runOnUiThread {
			dialog?.dismiss()
			dialog = null
		}
	}
}
