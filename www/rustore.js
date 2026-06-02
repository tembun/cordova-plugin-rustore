const exec = require("cordova/exec")

;(function() {
	const RuStore = function () {}
	RuStore.prototype.RuStoreStatus = {
		SUCCESS: "SUCCESS",
		DENIED: "DENIED",
		ERROR: "ERROR",
	}
	RuStore.prototype.RuStoreProductType = {
		CONSUMABLE: "CONSUMABLE",
		NON_CONSUMABLE: "NON_CONSUMABLE",
		SUBSCRIPTION: "SUBSCRIPTION",
	}
	RuStore.prototype.RuStorePurchaseType = {
		ONE_STEP: "ONE_STEP",
		TWO_STEP: "TWO_STEP",
		UNDEFINED: "UNDEFINED",
	}
	const RUSTORE_PLUGIN_CLASS = "RuStorePlugin"

	function execPromiseAPI(method, ...args) {
		return new Promise((resolve, reject) => {
			exec(
				(...succArgs) => { resolve(...succArgs) },
				(...errArgs) => { reject(...errArgs) },
				RUSTORE_PLUGIN_CLASS, method,
				[...args],
			)
		})
	}

	RuStore.prototype.checkPurchasesAvailability = function () {
		return execPromiseAPI("checkPurchasesAvailability")
	}
	RuStore.prototype.getProducts = function (productIds) {
		return execPromiseAPI("getProducts", productIds)
	}
	RuStore.prototype.purchase = function (productId) {
		return execPromiseAPI("purchase", productId)
	}

	RuStore.prototype.checkReviewAvailability = function() {
		return execPromiseAPI("checkReviewAvailability")
	}
	RuStore.prototype.requestReview = function () {
		return execPromiseAPI("requestReview")
	}
	module.exports = new RuStore()
})()
