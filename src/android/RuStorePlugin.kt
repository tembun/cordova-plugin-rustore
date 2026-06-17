package org.apache.cordova.plugin

import org.apache.cordova.plugin.ui.ProgressDialogController

import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin

import org.json.JSONArray
import org.json.JSONObject

import android.app.Activity
import android.os.Handler
import android.os.Looper

import ru.rustore.sdk.appupdate.manager.RuStoreAppUpdateManager
import ru.rustore.sdk.appupdate.manager.factory.RuStoreAppUpdateManagerFactory
import ru.rustore.sdk.appupdate.model.AppUpdateInfo
import ru.rustore.sdk.appupdate.model.AppUpdateOptions
import ru.rustore.sdk.appupdate.model.AppUpdateType
import ru.rustore.sdk.appupdate.model.InstallStatus
import ru.rustore.sdk.appupdate.model.UpdateAvailability

import ru.rustore.sdk.pay.RuStorePayClient
import ru.rustore.sdk.pay.model.PreferredPurchaseType
import ru.rustore.sdk.pay.model.ProductId
import ru.rustore.sdk.pay.model.ProductPurchaseParams
import ru.rustore.sdk.pay.model.ProductType
import ru.rustore.sdk.pay.model.PurchaseAvailabilityResult
import ru.rustore.sdk.pay.model.PurchaseType
import ru.rustore.sdk.pay.model.RuStorePaymentException

import ru.rustore.sdk.review.RuStoreReviewManager
import ru.rustore.sdk.review.RuStoreReviewManagerFactory

enum class PurchaseStatus {
	SUCCESS,
	DENIED,
	ERROR,
}

enum class ReviewStatus {
	SUCCESS,
	ERROR,
}

const val BYTES_IN_MB = 1024.0 * 1024.0

class RuStorePlugin : CordovaPlugin() {
	private lateinit var reviewManager: RuStoreReviewManager
	private lateinit var updateManager: RuStoreAppUpdateManager
	private var lastUpdateInfo: AppUpdateInfo? = null
	private var progressDialog: ProgressDialogController? = null

	override fun pluginInitialize() {
		super.pluginInitialize()
		reviewManager = RuStoreReviewManagerFactory.create(cordova.activity)
		updateManager = RuStoreAppUpdateManagerFactory.create(cordova.activity)
		progressDialog = ProgressDialogController(cordova.activity)
	}

	private fun checkPurchasesAvailability(ctx: CallbackContext) {
		RuStorePayClient.instance.getPurchaseInteractor().getPurchaseAvailability()
			.addOnSuccessListener { result ->
				when (result) {
				is PurchaseAvailabilityResult.Available -> {
					ctx.success(true)
				}
				is PurchaseAvailabilityResult.Unavailable -> {
					ctx.success(false)
				}
				}
			}
			.addOnFailureListener { throwable ->
				ctx.error("Failed to check purchases availability: ${throwable.toString()}")
			}
	}

	private fun getProducts(productIds: List<String>, ctx: CallbackContext) {
		RuStorePayClient.instance.getProductInteractor().getProducts(productIds.map { ProductId(it) })
			.addOnSuccessListener { products ->
				val resultProducts = products
				val resultProductsJson = JSONArray()
				resultProducts.forEach {
					val productJson = JSONObject()
					productJson.put("id", it.productId.value)
					val typeStr = when (it.type) {
					ProductType.CONSUMABLE_PRODUCT -> "CONSUMABLE"
					ProductType.NON_CONSUMABLE_PRODUCT -> "NON_CONSUMABLE"
					ProductType.SUBSCRIPTION -> "SUBSCRIPTION"
					}
					productJson.put("type", typeStr)
					productJson.put("amountLabel", it.amountLabel.value)
					productJson.put("price", it.price?.value ?: 0)
					productJson.put("currency", it.currency.value)
					productJson.put("title", it.title.value)
					it.description?.let {
						productJson.put("description", it.value)
					}
					productJson.put("imageUrl", it.imageUrl.value)
					resultProductsJson.put(productJson)
				}
				ctx.success(resultProductsJson)
			}
			.addOnFailureListener { throwable ->
				ctx.error("Failed to get products: ${throwable.toString()}")
			}
	}

	private fun purchase(productId: String, ctx: CallbackContext) {
		val purchaseParams = ProductPurchaseParams(
			productId = ProductId(productId),
			orderId = null,
			quantity = null,
			developerPayload = null,
			appUserId = null,
			appUserEmail = null,
		)

		val resultJson = JSONObject()
		val purchaseDataJson = JSONObject()
		RuStorePayClient.instance.getPurchaseInteractor().purchase(
			params = purchaseParams,
			preferredPurchaseType = PreferredPurchaseType.ONE_STEP,
		)
			.addOnSuccessListener { result ->
				result.orderId?.let {
					purchaseDataJson.put("orderId", it.value)
				}
				purchaseDataJson.put("purchaseId", result.purchaseId.value)
				purchaseDataJson.put("productId", result.productId.value)
				val purchaseTypeStr = when (result.purchaseType) {
				PurchaseType.TWO_STEP -> "TWO_STEP"
				PurchaseType.ONE_STEP -> "ONE_STEP"
				PurchaseType.UNDEFINED -> "UNDEFINED"
				}
				purchaseDataJson.put("purchaseType", purchaseTypeStr)
				val productTypeStr = when (result.productType) {
				ProductType.CONSUMABLE_PRODUCT -> "CONSUMABLE"
				ProductType.NON_CONSUMABLE_PRODUCT -> "NON_CONSUMABLE"
				ProductType.SUBSCRIPTION -> "SUBSCRIPTION"
				}
				purchaseDataJson.put("productType", productTypeStr)
				purchaseDataJson.put("invoiceId", result.invoiceId.value)
				purchaseDataJson.put("quantity", result.quantity.value)
				purchaseDataJson.put("sandbox", result.sandbox)

				resultJson.put("status", PurchaseStatus.SUCCESS.name)
				resultJson.put("purchase", purchaseDataJson)
				ctx.success(resultJson)
			}
			.addOnFailureListener { throwable ->
				when (throwable) {
					is RuStorePaymentException.ProductPurchaseCancelled -> {
						resultJson.put("status", PurchaseStatus.DENIED.name)
						resultJson.put("purchase", null)
						ctx.success(resultJson)
					}
					else -> {
						resultJson.put("status", PurchaseStatus.ERROR.name)
						resultJson.put("purchase", null)
						resultJson.put("msg", "Error during the purchase: ${throwable.toString()}")
						ctx.error(resultJson)
					}
				}
			}
	}

	private fun checkReviewAvailability(ctx: CallbackContext) {
		reviewManager.requestReviewFlow()
			.addOnSuccessListener {
				ctx.success(true)
			}
			.addOnFailureListener {
				ctx.success(false)
			}
	}

	private fun requestReview(ctx: CallbackContext) {
		val resultJson = JSONObject()
		reviewManager.requestReviewFlow()
			.addOnSuccessListener { reviewInfo ->
				reviewManager.launchReviewFlow(reviewInfo)
					.addOnSuccessListener {
						resultJson.put("status", ReviewStatus.SUCCESS.name)
						ctx.success(resultJson)
					}
					.addOnFailureListener { throwable ->
						resultJson.put("status", ReviewStatus.ERROR.name)
						resultJson.put("msg", throwable.toString())
						ctx.error(resultJson)
					}
			}
			.addOnFailureListener { throwable ->
				resultJson.put("status", ReviewStatus.ERROR.name)
				resultJson.put("msg", throwable.toString())
				ctx.error(resultJson)
			}
	}

	private fun checkUpdateAvailability(ctx: CallbackContext) {
		updateManager.getAppUpdateInfo()
			.addOnSuccessListener{ updateInfo ->
				lastUpdateInfo = updateInfo
				val result = when (updateInfo.updateAvailability) {
				UpdateAvailability.UNKNOWN,
				UpdateAvailability.UPDATE_NOT_AVAILABLE -> "UNAVAILABLE"
				/*
				 * UPDATE_AVAILABLE and installStatus combinations:
				 *     DOWNLOADED
				 *       User has already downloaded the file for update,
				 *       but cancelled the installation of it. It means
				 *       that the whole process is still in progress.
				 *     UNKNOWN
				 *       The downloading was started, but user stopped it.
				 *       Treat this as termination of the update process.
				 */
				UpdateAvailability.UPDATE_AVAILABLE -> {
					when (updateInfo.installStatus) {
					InstallStatus.DOWNLOADED,
					InstallStatus.INSTALLING -> "IN_PROGRESS"
					else -> "AVAILABLE"
					}
				}
				UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> "IN_PROGRESS"
				else -> "UNAVAILABLE"
				}
				ctx.success(result)
			}
			.addOnFailureListener {
				ctx.success("UPDATE_NOT_AVAILABLE")
			}
	}

	private fun installUpdate(ctx: CallbackContext) {
		updateManager.completeUpdate(AppUpdateOptions.Builder().appUpdateType(AppUpdateType.FLEXIBLE).build())
			.addOnSuccessListener {
				ctx.success(true)
			}
			.addOnFailureListener {
				ctx.success(false)
			}
	}

	private fun registerUpdateListener(ctx: CallbackContext) {
		updateManager.registerListener { state ->
			when (state.installStatus) {
			InstallStatus.DOWNLOADED -> {
				/*
				 * Even though official documentation says that we're
				 * free to completeUpdate() when we've received DOWNLOADED,
				 * in practice this is not the case.
				 * In reality, at the point when we've received DOWNLOADED,
				 * lastUpdateInfo.installStatus is UNKNOWN. If we would try
				 * to refresh it via getAppUpdateInfo(), its value would be 4.
				 * Now, the 4 itself is not documented even in the official
				 * docs (0-5 except 4 are documented), but I looked into SDK
				 * files and found that it is INSTALLING. In any case, both
				 * UNKNOWN and INSTALLING are inappropriate statuses for
				 * completeUpdate() (it will simply throw an error). It
				 * doesn't quite make sense, but I believe that a sort of
				 * race condition happens here.
				 * The only way I found to work this around is to
				 * completeUpdate() after a short delay. This works and
				 * after that delay completeUpdate() does what it should.
				 * But the value of 1000 is the very minimum that worked
				 * for me - less than that, and it won't work.
				 */
				Handler(Looper.getMainLooper()).postDelayed({
					progressDialog?.hide()
					installUpdate(ctx)
				}, 1000)
			}
			InstallStatus.DOWNLOADING -> {
				val totalBytes = state.totalBytesToDownload
				val bytesDownloaded = state.bytesDownloaded
				val mbDownloaded = bytesDownloaded / BYTES_IN_MB
				val totalMb = totalBytes / BYTES_IN_MB
				val percent = (state.bytesDownloaded * 100 / state.totalBytesToDownload).toInt()
				progressDialog?.update(percent, mbDownloaded, totalMb)
			}
			InstallStatus.FAILED -> {
				progressDialog?.hide()
				ctx.success(false)
			}
			InstallStatus.DOWNLOAD_INTERRUPTED -> {
				progressDialog?.hide()
				ctx.success(false)
			}
			}
		}
	}

	private fun downloadAndInstallUpdate(ctx: CallbackContext) {
		val updateInfo = lastUpdateInfo
		if (updateInfo == null) {
			ctx.success(false)
			return
		}
		if (updateInfo.updateAvailability != UpdateAvailability.UPDATE_AVAILABLE && updateInfo.updateAvailability != UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
			return
		}
		if (updateInfo.installStatus == InstallStatus.DOWNLOADED) {
			installUpdate(ctx)
			return
		}
		registerUpdateListener(ctx)
		if (updateInfo.installStatus == InstallStatus.DOWNLOADING ||
		    updateInfo.installStatus == InstallStatus.INSTALLING) {
			return
		}
		updateManager.startUpdateFlow(updateInfo, AppUpdateOptions.Builder().appUpdateType(AppUpdateType.FLEXIBLE).build())
			.addOnSuccessListener { resultCode ->
				if (resultCode != Activity.RESULT_OK) {
					progressDialog?.hide()
					ctx.success(false)
				}
			}
			.addOnFailureListener {
				progressDialog?.hide()
				ctx.success(false)
			}
	}

	override fun execute(action: String, args: JSONArray, ctx: CallbackContext): Boolean {
		when (action) {
		"checkPurchasesAvailability" -> {
			checkPurchasesAvailability(ctx)
		}
		"getProducts" -> {
			val productIdsJson = args.getJSONArray(0)
			val productIds = List<String>(productIdsJson.length()) {
				productIdsJson.getString(it)
			}
			getProducts(productIds, ctx)
		}
		"purchase" -> {
			val productId = args.getString(0)
			purchase(productId, ctx)
		}
		"checkReviewAvailability" -> {
			checkReviewAvailability(ctx)
		}
		"requestReview" -> {
			requestReview(ctx)
		}
		"checkUpdateAvailability" -> {
			checkUpdateAvailability(ctx)
		}
		"installUpdate" -> {
			downloadAndInstallUpdate(ctx)
		}
		else -> return false
		}
		return true
	}
}
