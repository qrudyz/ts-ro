package com.rudyunguru.trucks.core.catalog

import com.rudyunguru.trucks.core.model.CargoKind
import com.rudyunguru.trucks.core.model.TrailerKind
import com.rudyunguru.trucks.core.model.TrailerModel

/**
 * The six trailer types required by the design. Every trailer has its own procedural mesh variant
 * (curtains, refrigeration unit, tank barrel, flat deck, container frame, low bed).
 */
object TrailerCatalog {

    val TRAILERS: List<TrailerModel> = listOf(
        TrailerModel(
            id = "krone_profiliner",
            name = "Krone Profi Liner",
            kind = TrailerKind.CURTAINSIDER,
            priceEuro = 24_500, capacityTons = 24.0, tareWeightKg = 6_800, axleCount = 3,
            allowedCargo = listOf(CargoKind.FOOD, CargoKind.GRAIN, CargoKind.TIMBER, CargoKind.CONSTRUCTION),
            meshVariant = 0,
            description = "Semiremorcă cu prelată, încărcare laterală - cel mai versatil trailer din flotă.",
            maintenanceCostEuro = 320,
        ),
        TrailerModel(
            id = "schmitz_skocool",
            name = "Schmitz Cargobull S.KO Cool",
            kind = TrailerKind.REFRIGERATED,
            priceEuro = 48_900, capacityTons = 21.0, tareWeightKg = 8_400, axleCount = 3,
            allowedCargo = listOf(CargoKind.REFRIGERATED, CargoKind.FOOD),
            meshVariant = 1,
            description = "Grup frigorific cu temperatură controlată pentru marfă perisabilă.",
            maintenanceCostEuro = 620,
        ),
        TrailerModel(
            id = "wielton_cistern30",
            name = "Wielton Cistern 30",
            kind = TrailerKind.TANKER,
            priceEuro = 55_400, capacityTons = 26.0, tareWeightKg = 7_600, axleCount = 3,
            allowedCargo = listOf(CargoKind.CONSTRUCTION, CargoKind.FOOD),
            meshVariant = 2,
            description = "Cisternă cu trei compartimente pentru lichide și pompă de descărcare.",
            maintenanceCostEuro = 700,
        ),
        TrailerModel(
            id = "kogel_flatbed",
            name = "Kögel Flatbed",
            kind = TrailerKind.FLATBED,
            priceEuro = 19_800, capacityTons = 25.0, tareWeightKg = 6_200, axleCount = 3,
            allowedCargo = listOf(CargoKind.CONSTRUCTION, CargoKind.MACHINERY, CargoKind.TIMBER),
            meshVariant = 3,
            description = "Platformă deschisă cu stâlpi rabatabili pentru materiale lungi.",
            maintenanceCostEuro = 250,
        ),
        TrailerModel(
            id = "krone_boxliner",
            name = "Krone Box Liner",
            kind = TrailerKind.CONTAINER,
            priceEuro = 31_200, capacityTons = 28.0, tareWeightKg = 5_400, axleCount = 3,
            allowedCargo = listOf(CargoKind.CONTAINERS),
            meshVariant = 4,
            description = "Șasiu cu gât de lebădă și pini twist-lock pentru containere ISO 20/40.",
            maintenanceCostEuro = 300,
        ),
        TrailerModel(
            id = "schwarzmuller_lowloader",
            name = "Schwarzmüller Low Loader",
            kind = TrailerKind.LOW_LOADER,
            priceEuro = 68_700, capacityTons = 32.0, tareWeightKg = 11_200, axleCount = 4,
            allowedCargo = listOf(CargoKind.MACHINERY, CargoKind.CONSTRUCTION),
            meshVariant = 5,
            description = "Trailer coborât extensibil pentru utilaje grele și transporturi agabaritice.",
            maintenanceCostEuro = 880,
        ),
    )

    private val byId: Map<String, TrailerModel> = TRAILERS.associateBy { it.id }

    fun byId(id: String): TrailerModel = byId[id] ?: byId.getValue(TRAILERS.first().id)

    fun byIdOrNull(id: String): TrailerModel? = byId[id]

    fun cheapest(): TrailerModel = TRAILERS.minBy { it.priceEuro }

    /** Trailers that may legally haul the given cargo - used by the contract validator. */
    fun compatible(cargo: CargoKind): List<TrailerModel> = TRAILERS.filter { it.canHaul(cargo) }

    /**
     * Specialised cargo requires a matching trailer; everything else can be hauled by any trailer
     * that lists the cargo in [TrailerModel.allowedCargo].
     */
    fun requiredKind(cargo: CargoKind): TrailerKind? = when (cargo) {
        CargoKind.REFRIGERATED -> TrailerKind.REFRIGERATED
        CargoKind.CONTAINERS -> TrailerKind.CONTAINER
        CargoKind.MACHINERY -> TrailerKind.LOW_LOADER
        CargoKind.TIMBER -> TrailerKind.FLATBED
        else -> null
    }
}
