package com.rudyunguru.trucks.core.catalog

import com.rudyunguru.trucks.core.model.CargoKind
import com.rudyunguru.trucks.core.model.City
import com.rudyunguru.trucks.core.model.CityTier
import com.rudyunguru.trucks.core.model.DepotKind

/**
 * The playable map of the MVP: 16 Romanian cities connected by a simplified, original road network.
 *
 * Only publicly known geographic coordinates are used - the road network, city layouts and every 3D
 * asset in the game are generated from scratch by this project.
 *
 * Table format (one city per row, pipe separated):
 * `id|name|lat|lon|county|tier|population|flags|depots`
 * where `flags` is a 4 letter string standing for truckDealer, trailerDealer, service, garageDealer
 * (`D` = yes, `-` = no) and `depots` is a comma separated list of [DepotKind] names.
 */
object CityCatalog {

    private val RAW: List<String> = listOf(
        "bucuresti|București|44.4268|26.1025|B|METROPOLIS|1716000|DDDD|LOGISTICS_HUB,WAREHOUSE,FACTORY,TERMINAL",
        "ploiesti|Ploiești|44.9366|26.0129|PH|CITY|180000|-DDD|WAREHOUSE,FACTORY,LOGISTICS_HUB",
        "brasov|Brașov|45.6579|25.6012|BV|CITY|237000|DDDD|FACTORY,WAREHOUSE,SAWMILL",
        "sibiu|Sibiu|45.7983|24.1256|SB|CITY|147000|D-DD|WAREHOUSE,FACTORY,LOGISTICS_HUB",
        "cluj|Cluj-Napoca|46.7712|23.6236|CJ|METROPOLIS|286000|D-DD|LOGISTICS_HUB,FACTORY,TERMINAL",
        "bacau|Bacău|46.5670|26.9146|BC|CITY|136000|-DDD|FACTORY,WAREHOUSE,SAWMILL",
        "iasi|Iași|47.1585|27.6014|IS|METROPOLIS|271000|D-DD|WAREHOUSE,LOGISTICS_HUB,FARM",
        "constanta|Constanța|44.1598|28.6348|CT|CITY|263000|DDDD|PORT,LOGISTICS_HUB,TERMINAL",
        "craiova|Craiova|44.3302|23.7949|DJ|CITY|234000|D-DD|FACTORY,WAREHOUSE,FARM",
        "timisoara|Timișoara|45.7489|21.2087|TM|METROPOLIS|250000|DDDD|FACTORY,LOGISTICS_HUB,WAREHOUSE",
        "oradea|Oradea|47.0465|21.9189|BH|CITY|183000|--DD|FACTORY,WAREHOUSE",
        "arad|Arad|46.1866|21.3123|AR|CITY|145000|-DDD|FACTORY,WAREHOUSE",
        "pitesti|Pitești|44.8565|24.8692|AG|CITY|141000|--DD|FACTORY,WAREHOUSE,QUARRY",
        "galati|Galați|45.4353|28.0080|GL|CITY|217000|--DD|PORT,FACTORY,QUARRY",
        "targumures|Târgu Mureș|46.5386|24.5573|MS|TOWN|116000|--DD|FARM,SAWMILL,WAREHOUSE",
        "suceava|Suceava|47.6635|26.2594|SV|TOWN|92000|--DD|SAWMILL,FARM,WAREHOUSE",
    )

    val CITIES: List<City> = RAW.map { parse(it) }

    private val byId: Map<String, City> = CITIES.associateBy { it.id }

    /** City where the player always starts (cheap first truck + first contracts). */
    const val START_CITY_ID: String = "bucuresti"

    private fun parse(row: String): City {
        val f = row.split('|')
        require(f.size == 9) { "Invalid city row: $row" }
        val flags = f[7]
        return City(
            id = f[0], name = f[1], lat = f[2].toDouble(), lon = f[3].toDouble(),
            county = f[4], tier = CityTier.valueOf(f[5]), population = f[6].toInt(),
            hasTruckDealer = flags[0] == 'D',
            hasTrailerDealer = flags[1] == 'D',
            hasService = flags[2] == 'D',
            hasGarageDealer = flags[3] == 'D',
            depots = f[8].split(',').map { DepotKind.valueOf(it) },
        )
    }

    fun byId(id: String): City = byId[id] ?: byId.getValue(START_CITY_ID)

    fun byIdOrNull(id: String): City? = byId[id]

    fun name(id: String): String = byIdOrNull(id)?.name ?: id

    fun withTruckDealer(): List<City> = CITIES.filter { it.hasTruckDealer }

    fun withTrailerDealer(): List<City> = CITIES.filter { it.hasTrailerDealer }

    fun withService(): List<City> = CITIES.filter { it.hasService }

    fun citiesSupplying(cargo: CargoKind): List<City> =
        CITIES.filter { city -> city.depots.any { it.cargo.contains(cargo) } }

    fun isKnown(id: String): Boolean = byId.containsKey(id)

    /** Nearest city to a world-space position (used by the GPS / minimap). */
    fun nearest(x: Double, z: Double): City =
        CITIES.minBy { (it.x - x) * (it.x - x) + (it.z - z) * (it.z - z) }
}
