package com.rudyunguru.trucks.core.catalog

import com.rudyunguru.trucks.core.model.CabinKind
import com.rudyunguru.trucks.core.model.DriveConfig
import com.rudyunguru.trucks.core.model.GearboxKind
import com.rudyunguru.trucks.core.model.TruckClass
import com.rudyunguru.trucks.core.model.TruckModel

/**
 * The tractor units available in the dealer: MAN, DAF, Mercedes-Benz, Iveco, Scania, Volvo and
 * Renault Trucks, from a light regional MAN TGL up to the 750 hp Volvo FH16.
 *
 * LICENSING / TRADEMARK NOTE
 * Brand and model names are real trademarks used descriptively so the player recognises the trucks,
 * the same way real trucking games do after signing manufacturer licence agreements. Every mesh,
 * texture, badge and interior in this game is generated procedurally by our own code - nothing is
 * copied, extracted or converted from another game. Before a commercial release you either sign
 * those licences or you replace this single table with your own fictional brands; no other file in
 * the project needs to change.
 *
 * Specification figures are typical class values rounded for gameplay, not official data sheets.
 *
 * TABLE FORMAT (22 pipe separated fields, one model per row):
 * id|brand|model|class|price|hp|torqueNm|displacementL|gearbox|gears|drive|curbKg|gvwKg|tankL|
 * L/100km|topKmh|cabin|rating|meshVariant|paint1,paint2,paint3,paint4|serviceEuro|description
 */
object TruckCatalog {

    /** Typical loss of value when a truck is sold back to the dealer. */
    const val RESALE_FACTOR: Double = 0.62

    /** Number of distinct procedural bodies the renderer must provide (meshVariant 0..9). */
    const val MESH_VARIANTS: Int = 10

    /** The truck every new player can afford first. */
    const val STARTER_ID: String = "man_tgl12220"

    private val RAW: List<String> = listOf(
        "man_tgl12220|MAN|TGL 12.220|STARTER|68000|220|850|4.6|MANUAL|6|4x2|5400|12000|190|18.4|100|DAY_CAB|3.6|0|#D9051F,#F2F2F2,#1B1B1B,#3B6EA5|380|Camion ușor de oraș pentru marfă paletizată și distribuție regională.",
        "daf_cf430|DAF|CF 430|MEDIUM|118000|430|2100|10.8|AUTOMATED|12|4x2|7300|34000|620|27.1|88|SLEEPER|4.1|1|#1B5E20,#EDEDED,#243B53,#C0392B|700|Echilibrul dintre preț, consum și fiabilitate pentru naveta națională.",
        "mb_actros1845|Mercedes-Benz|Actros 1845|MEDIUM|152000|450|2200|10.7|AUTOMATED|12|4x2|7700|40000|800|28.2|89|SLEEPER|4.3|2|#C7CBD1,#0B2545,#8C1C1C,#1B1B1B|880|Cabina clasică de long-haul, confort bun și service previzibil.",
        "iveco_sway490|Iveco|S-Way 490|HEAVY|175000|490|2350|12.9|AUTOMATED|12|6x2|8200|40000|800|29.0|89|SLEEPER|4.2|3|#F2A413,#1D3557,#E5E5E5,#2C2C2C|940|Greutate medie cu axă ridicătoare, potrivit pentru convoaie mixte.",
        "man_tgx18510|MAN|TGX 18.510|HEAVY|178000|510|2500|12.4|AUTOMATIC|12|4x2|8100|40000|960|28.6|89|SLEEPER|4.5|4|#D9051F,#333333,#F5F5F5,#0B3C5D|1020|Tractorul de flotă: eficient, silențios, ideal pentru autostradă.",
        "scania_r450|Scania|R 450|HEAVY|186000|450|2350|12.7|AUTOMATIC|12|6x2|8300|40000|1000|27.8|89|SLEEPER|4.7|5|#0F4C81,#E4E4E4,#1B1B1B,#B31217|1100|Cuplu puternic la turații mici și consum redus pe distanțe lungi.",
        "volvo_fh460|Volvo|FH 460|HEAVY|192000|460|2300|12.8|AUTOMATED|12|6x2|8400|40000|900|27.5|89|SLEEPER|4.6|6|#1C6E8C,#F0F0F0,#101010,#8A8D91|1150|Cabină premium cu aerodinamică de top și vizibilitate excelentă.",
        "renault_t520|Renault Trucks|T 520|HEAVY|198000|520|2550|13.0|AUTOMATIC|12|6x2|8300|40000|1020|29.4|89|SLEEPER|4.5|7|#2E3A59,#E6E6E6,#9E2A2B,#171717|1120|Putere mare pentru rampe lungi, interior spațios și ergonomic.",
        "scania_s730|Scania|S 730|FLAGSHIP|246000|730|3500|16.4|AUTOMATIC|12|6x4|9100|40000|1000|33.5|89|LUXURY_SLEEPER|4.9|8|#0F4C81,#D4AF37,#1B1B1B,#F5F5F5|1550|V8 legendar: cabină cu podea plată, 6x4 și rezervă uriașă de putere.",
        "volvo_fh16_750|Volvo|FH16 750|FLAGSHIP|268000|750|3550|16.1|AUTOMATIC|12|6x4|9300|44000|1200|35.9|89|LUXURY_SLEEPER|4.8|9|#1C6E8C,#0B0B0B,#FFC300,#E6E6E6|1620|Cel mai puternic din flotă, pentru convoaie de 44 t și utilaje grele.",
    )

    val TRUCKS: List<TruckModel> = RAW.map { parse(it) }

    private val byId: Map<String, TruckModel> = TRUCKS.associateBy { it.id }

    private fun parse(row: String): TruckModel {
        val f = row.split('|')
        require(f.size == 22) { "Invalid truck row (${f.size} fields): $row" }
        return TruckModel(
            id = f[0],
            brand = f[1],
            name = f[2],
            truckClass = TruckClass.valueOf(f[3]),
            priceEuro = f[4].toLong(),
            powerHp = f[5].toInt(),
            torqueNm = f[6].toInt(),
            displacementL = f[7].toDouble(),
            gearbox = GearboxKind.valueOf(f[8]),
            gearCount = f[9].toInt(),
            driveConfig = DriveConfig.valueOf("D" + f[10]),
            curbWeightKg = f[11].toInt(),
            maxGrossWeightKg = f[12].toInt(),
            fuelTankL = f[13].toInt(),
            baseConsumptionL100 = f[14].toDouble(),
            topSpeedKmh = f[15].toInt(),
            cabin = CabinKind.valueOf(f[16]),
            rating = f[17].toDouble(),
            meshVariant = f[18].toInt(),
            paintOptions = f[19].split(','),
            serviceCostEuro = f[20].toLong(),
            description = f[21],
            resaleFactor = RESALE_FACTOR,
        )
    }

    fun byId(id: String): TruckModel = byId[id] ?: byId.getValue(STARTER_ID)

    fun byIdOrNull(id: String): TruckModel? = byId[id]

    fun name(id: String): String = byIdOrNull(id)?.fullName ?: id

    fun cheapest(): TruckModel = TRUCKS.minBy { it.priceEuro }

    fun byClass(truckClass: TruckClass): List<TruckModel> = TRUCKS.filter { it.truckClass == truckClass }

    /** Trucks that can legally pull the given total weight (truck + trailer + cargo). */
    fun ableToPull(totalTons: Double): List<TruckModel> =
        TRUCKS.filter { it.maxGrossWeightKg / 1000.0 >= totalTons }

    /** Sort options of the "compară camioane" table in the showroom. */
    enum class SortKey(val label: String) {
        PRICE("Preț"),
        POWER("Putere"),
        CONSUMPTION("Consum"),
        PAYLOAD("Încărcătură"),
        RATING("Rating"),
    }

    fun sorted(key: SortKey, ascending: Boolean = true): List<TruckModel> {
        val base = when (key) {
            SortKey.PRICE -> TRUCKS.sortedBy { it.priceEuro }
            SortKey.POWER -> TRUCKS.sortedBy { it.powerHp }
            SortKey.CONSUMPTION -> TRUCKS.sortedBy { it.baseConsumptionL100 }
            SortKey.PAYLOAD -> TRUCKS.sortedBy { it.maxPayloadTons }
            SortKey.RATING -> TRUCKS.sortedBy { it.rating }
        }
        return if (ascending) base else base.reversed()
    }
}
