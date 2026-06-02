package org.apache.cordova.plugin

import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CallbackContext

import org.json.JSONArray
import org.json.JSONObject

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

class RuStorePlugin : CordovaPlugin() {
	private lateinit var reviewManager: RuStoreReviewManager

	override fun pluginInitialize() {
		super.pluginInitialize()
		reviewManager = RuStoreReviewManagerFactory.create(cordova.activity)
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
				val purchaseId = result.purchaseId
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
			.addOnFailureListener { throwable ->
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
		else -> return false
		}
		return true
	}
}
