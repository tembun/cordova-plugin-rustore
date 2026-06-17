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
type RuStoreProductData = {
	id: string
	type: RuStoreProductType
	amountLabel: string
	currency: string
	title: string
	price: number
	description?: string
	imageUrl: string
}
type RuStoreGetProductsResult = Array<RuStoreProductData>

enum RuStorePurchaseType {
	ONE_STEP = "ONE_STEP",
	TWO_STEP = "TWO_STEP",
	UNDEFINED = "UNDEFINED",
}
type RuStorePurchaseData = {
	orderId?: string
	purchaseId: string
	productId: string
	purchaseType: RuStorePurchaseType
	productType: RuStoreProductType
	invoiceId: string
	quantity: number
	sandbox: boolean
}
type RuStorePurchaseResult = {
	purchase: RuStorePurchaseData
}

enum RuStoreUpdateAvailability {
	AVAILABLE = "AVAILABLE",
	UNAVAILABLE = "UNAVAILABLE",
	IN_PROGRESS = "IN_PROGRESS",
}

declare class RuStore {
	checkPurchasesAvailability(): Promise<boolean>
	getProducts(productIds: Array<string>): Promise<RuStoreGetProductsResult>
	purchase(productId: string): Promise<WithStatus<RuStorePurchaseResult>>
	RuStorePurchaseType: typeof RuStorePurchaseType
	RuStoreProductType: typeof RuStoreProductType

	checkReviewAvailability(): Promise<boolean>
	requestReview(): Promise<WithStatus>

	checkUpdateAvailability(): Promise<RuStoreUpdateAvailability>
	installUpdate(): Promise<boolean>
	RuStoreUpdateAvailability: typeof RuStoreUpdateAvailability

	RuStoreStatus: typeof RuStoreStatus
}

declare global {
	interface Window {
		ruStore: RuStore
	}
}

export {}
