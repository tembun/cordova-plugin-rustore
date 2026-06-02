enum RuStoreStatus {
	SUCCESS = "SUCCESS",
	DENIED = "DENIED",
	ERROR = "ERROR",
}
type StatusObject = { status: RuStoreStatus; msg?: string }
type WithStatus<Type = void> = Type extends void ? StatusObject : StatusObject & Type

enum RuStoreProductType {
	CONSUMABLE = "CONSUMABLE",
	NON_CONSUMABLE = "NON_CONSUMABLE",
	SUBSCRIPTION = "SUBSCRIPTION",
}
export type RuStoreProductData = {
	id: string
	type: RuStoreProductType
	amountLabel: string
	currency: string
	title: string
	price: number
	description?: string
	imageUrl: string
}
export type RuStoreGetProductsResult = Array<RuStoreProductData>

enum RuStorePurchaseType {
	ONE_STEP = "ONE_STEP",
	TWO_STEP = "TWO_STEP",
	UNDEFINED = "UNDEFINED",
}
export type RuStorePurchaseData = {
	orderId?: string
	purchaseId: string
	productId: string
	purchaseType: RuStorePurchaseType
	productType: RuStoreProductType
	invoiceId: string
	quantity: number
	sandbox: boolean
}
export type RuStorePurchaseResult = {
	purchase: RuStorePurchaseData
}

declare class RuStore {
	checkPurchasesAvailability(): Promise<boolean>
	getProducts(productIds: Array<string>): Promise<RuStoreGetProductsResult>
	purchase(productId: string): Promise<WithStatus<RuStorePurchaseResult>>
	checkReviewAvailability(): Promise<boolean>
	requestReview(): Promise<WithStatus>

	RuStoreStatus: typeof RuStoreStatus
	RuStorePurchaseType: typeof RuStorePurchaseType
	RuStoreProductType: typeof RuStoreProductType
}

declare global {
	interface Window {
		ruStore: RuStore
	}
}
