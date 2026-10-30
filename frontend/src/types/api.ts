// The shapes of the API, copied from the backend DTOs. Field names and enum codes are the ones the API uses.

/** A wall-clock date and time without zone, as `YYYY-MM-DDTHH:mm:ss`. It is read in the event time zone. */
export type LocalDateTime = string

/** A date as `YYYY-MM-DD`. */
export type LocalDate = string

export type EventStatus = 'RASCUNHO' | 'ABERTO' | 'ENCERRADO' | 'REALIZADO' | 'CANCELADO'
export type EventType = 'BREAKFAST' | 'LUNCH' | 'SNACK' | 'GET_TOGETHER' | 'OTHER'
export type MeasureUnit = 'UNIT' | 'PORTION' | 'GRAM' | 'MILLILITER'
export type Allergen = 'GLUTEN' | 'LACTOSE' | 'PEANUT' | 'TREE_NUTS' | 'SHELLFISH' | 'FISH' | 'EGG' | 'SOY'
export type FoodCharacteristic = 'ANIMAL_MEAT' | 'ADDED_SUGAR' | 'HIGH_SODIUM'
export type DietaryRestriction =
  | 'LACTOSE_INTOLERANCE'
  | 'CELIAC_GLUTEN_FREE'
  | 'PEANUT_TREE_NUT_ALLERGY'
  | 'SHELLFISH_ALLERGY'
  | 'FISH_ALLERGY'
  | 'EGG_ALLERGY'
  | 'SOY_ALLERGY'
  | 'DIABETIC_NO_SUGAR'
  | 'HYPERTENSIVE_LOW_SODIUM'
  | 'VEGETARIAN'
  | 'VEGAN'
export type EventReadiness = 'NO_ITEMS' | 'CRITICAL' | 'IN_PROGRESS' | 'MANDATORY_SECURED' | 'COMPLETE'
export type AlertLevel = 'CRITICAL' | 'WARNING' | 'INFO'

// Events

export interface FoodEvent {
  id: number
  name: string
  description: string | null
  type: EventType
  dateTime: LocalDateTime
  changeDeadline: LocalDateTime
  timeZone: string
  status: EventStatus
  deadlineExpired: boolean
  acceptsContributions: boolean
  allowedTransitions: EventStatus[]
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}

export interface EventFilters {
  status?: EventStatus
  type?: EventType
  from?: LocalDate
  until?: LocalDate
}

/** `timeZone` and `status` are optional: the event starts as RASCUNHO in the server default time zone. */
export interface CreateEventRequest {
  name: string
  description?: string | null
  type: EventType
  dateTime: LocalDateTime
  changeDeadline: LocalDateTime
  timeZone?: string | null
  status?: EventStatus | null
}

/** `timeZone` is optional: if missing, the event keeps its current time zone. */
export interface UpdateEventRequest {
  name: string
  description?: string | null
  type: EventType
  dateTime: LocalDateTime
  changeDeadline: LocalDateTime
  timeZone?: string | null
}

/** `newChangeDeadline` only applies when opening the event. */
export interface ChangeStatusRequest {
  newStatus: EventStatus
  newChangeDeadline?: LocalDateTime | null
}

// Menu

export interface Category {
  id: number
  name: string
  unit: MeasureUnit
  goalQuantity: number
  totalItems: number
  plannedQuantity: number
  contributedQuantity: number
}

export interface CategoryRequest {
  name: string
  unit: MeasureUnit
  goalQuantity: number
}

export interface Item {
  id: number
  name: string
  categoryId: number
  categoryName: string
  unit: MeasureUnit
  requiredQuantity: number
  mandatory: boolean
  contributedQuantity: number
  pendingQuantity: number
  coveredPercentage: number
  allergens: Allergen[]
  characteristics: FoodCharacteristic[]
}

export interface CreateItemRequest {
  name: string
  categoryId: number
  requiredQuantity: number
  mandatory?: boolean
  allergens?: Allergen[]
  characteristics?: FoodCharacteristic[]
}

/** The item never changes category, so there is no `categoryId` here. */
export interface UpdateItemRequest {
  name: string
  requiredQuantity: number
  mandatory?: boolean
  allergens?: Allergen[]
  characteristics?: FoodCharacteristic[]
}

// People

/** `id` is the enrollment in this event, and `collaboratorId` is the person. The CPF comes masked. */
export interface Participant {
  id: number
  collaboratorId: number
  name: string
  cpf: string
  restrictions: DietaryRestriction[]
  enrolledAt: LocalDateTime
}

/** `name` and `restrictions` only apply to a new CPF: enrolling a registered person changes nothing. */
export interface EnrollParticipantRequest {
  cpf: string
  name?: string | null
  restrictions?: DietaryRestriction[]
}

/** The CPF comes masked. */
export interface Collaborator {
  id: number
  name: string
  cpf: string
  restrictions: DietaryRestriction[]
}

/** The restrictions are required: an empty list clears them. */
export interface UpdateCollaboratorRequest {
  name: string
  restrictions: DietaryRestriction[]
}

// Contributions

export interface Contribution {
  id: number
  participantId: number
  participantName: string
  itemId: number
  itemName: string
  unit: MeasureUnit
  quantity: number
  note: string | null
  createdAt: LocalDateTime
  updatedAt: LocalDateTime
}

export interface ContributionFilters {
  itemId?: number
  participantId?: number
}

export interface CreateContributionRequest {
  participantId: number
  itemId: number
  quantity: number
  note?: string | null
}

/** The person and the item never change. */
export interface UpdateContributionRequest {
  quantity: number
  note?: string | null
}

// Coverage, dietary analysis and the assistant

export interface CategoryCoverage {
  categoryId: number
  name: string
  unit: MeasureUnit
  goalQuantity: number
  totalItems: number
  requiredQuantity: number
  contributedQuantity: number
  goalDeficit: number
  effectiveTarget: number
  goalReached: boolean
  /** `null` for a category without items and without a goal. */
  percentage: number | null
  mandatoryPercentage: number
  optionalPercentage: number
}

export interface Coverage {
  readiness: EventReadiness
  /** `null` when no category has items or a goal. */
  overallPercentage: number | null
  mandatoryPercentage: number
  optionalPercentage: number
  totalItems: number
  categories: CategoryCoverage[]
}

export interface UnsafeItem {
  itemId: number
  itemName: string
  conflictingAllergens: Allergen[]
  conflictingCharacteristics: FoodCharacteristic[]
}

/** Items not listed in `unsafeItems` are safe for the person. */
export interface ParticipantDiet {
  participantId: number
  name: string
  restrictions: DietaryRestriction[]
  totalItems: number
  safeItems: number
  unsafeItems: UnsafeItem[]
}

export interface RestrictionSummary {
  restriction: DietaryRestriction
  description: string
  peopleCount: number
  compatibleItems: number
  totalItems: number
}

export interface DietaryAlert {
  level: AlertLevel
  message: string
  /** `null` for an alert about the whole event. */
  participantId: number | null
}

export interface DietaryAnalysis {
  totalItems: number
  participants: ParticipantDiet[]
  restrictions: RestrictionSummary[]
  alerts: DietaryAlert[]
}

export interface Suggestion {
  itemId: number
  itemName: string
  categoryName: string
  unit: MeasureUnit
  requiredQuantity: number
  contributedQuantity: number
  pendingQuantity: number
  coveragePercentage: number
  suggestedQuantity: number
  /** From 0 to 100: 100 is an item that scores on every rule that applies to the person. */
  priority: number
  /** `null` when the person has no dietary restrictions. */
  safeForParticipant: boolean | null
  reasons: string[]
}

export interface Summary {
  text: string
}

// Domain options

export interface Option<Code extends string = string> {
  code: Code
  description: string
}

export interface DomainOptions {
  eventTypes: Option<EventType>[]
  eventStatuses: Option<EventStatus>[]
  measureUnits: Option<MeasureUnit>[]
  allergens: Option<Allergen>[]
  foodCharacteristics: Option<FoodCharacteristic>[]
  dietaryRestrictions: Option<DietaryRestriction>[]
  eventReadiness: Option<EventReadiness>[]
}

// Errors

export interface FieldError {
  field: string
  message: string
}

/** Every API error comes in this shape (RFC 7807). `errors` only comes with a validation error. */
export interface ProblemDetail {
  status: number
  title?: string
  detail?: string
  errors?: FieldError[]
}
