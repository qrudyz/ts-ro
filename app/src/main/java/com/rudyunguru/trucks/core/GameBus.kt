package com.rudyunguru.trucks.core

/**
 * Minimal typed event bus used to decouple the simulation from the UI layer. Keys live in
 * [GameEvents]; payload types are checked by the helper functions below.
 */
class GameBus {

    private val listeners = HashMap<String, MutableList<(Any?) -> Unit>>()

    fun on(key: String, listener: (Any?) -> Unit): () -> Unit {
        val list = listeners.getOrPut(key) { ArrayList(2) }
        list.add(listener)
        return { off(key, listener) }
    }

    inline fun <reified T : Any> onTyped(key: String, noinline listener: (T) -> Unit): () -> Unit =
        on(key) { payload -> if (payload is T) listener(payload) else if (payload == null) listener(Unit as T) else Unit }

    fun off(key: String, listener: (Any?) -> Unit) {
        listeners[key]?.remove(listener)
    }

    fun emit(key: String, payload: Any? = null) {
        val list = listeners[key] ?: return
        // Copy to allow listeners to unsubscribe during dispatch.
        for (listener in list.toList()) {
            runCatching { listener(payload) }
        }
    }

    fun clear() {
        listeners.clear()
    }
}

/** Event keys and their payload types, documented in one place. */
object GameEvents {
    /** payload: [com.rudyunguru.trucks.core.model.OwnedTruck] */
    const val TRUCK_PURCHASED = "truck.purchased"
    const val TRUCK_SOLD = "truck.sold"
    const val TRUCK_REPAIRED = "truck.repaired"
    const val TRUCK_UPGRADED = "truck.upgraded"
    const val TRUCK_REFUELLED = "truck.refuelled"
    const val TRUCK_ALLOCATED = "truck.allocated"

    /** payload: [com.rudyunguru.trucks.core.model.OwnedTrailer] */
    const val TRAILER_PURCHASED = "trailer.purchased"
    const val TRAILER_SOLD = "trailer.sold"
    const val TRAILER_ALLOCATED = "trailer.allocated"

    /** payload: [com.rudyunguru.trucks.core.model.Driver] */
    const val DRIVER_HIRED = "driver.hired"
    const val DRIVER_FIRED = "driver.fired"
    const val DRIVER_LEVEL_UP = "driver.levelUp"

    /** payload: [com.rudyunguru.trucks.core.model.Contract] */
    const val CONTRACT_ACCEPTED = "contract.accepted"
    const val CONTRACT_COMPLETED = "contract.completed"
    const val CONTRACT_FAILED = "contract.failed"
    const val CONTRACT_EXPIRED = "contract.expired"

    /** payload: [com.rudyunguru.trucks.core.model.Garage] */
    const val GARAGE_PURCHASED = "garage.purchased"
    const val GARAGE_UPGRADED = "garage.upgraded"

    const val COMPANY_CREATED = "company.created"
    const val COMPANY_RENAMED = "company.renamed"

    /** payload: Double (money delta) */
    const val MONEY_CHANGED = "money.changed"
    /** payload: Int (reputation delta) */
    const val REPUTATION_CHANGED = "reputation.changed"
    /** payload: Int (xp delta) */
    const val XP_GAINED = "player.xp"

    /** payload: [com.rudyunguru.trucks.core.model.Notification] */
    const val NOTIFICATION = "ui.notification"
    /** payload: String key of the affected model ("truck", "driver", ...) */
    const val STATE_DIRTY = "state.dirty"

    /** Driving session events. */
    const val JOB_STARTED = "job.started"
    const val JOB_DELIVERED = "job.delivered"
    const val JOB_ABANDONED = "job.abandoned"
    const val POLICE_FINE = "police.fine"
    const val COLLISION = "world.collision"
    const val FUEL_STATION_ENTERED = "world.fuelStation"
    const val SERVICE_ENTERED = "world.service"
    const val GATE_ENTERED = "world.gate"
    const val TRAFFIC_LIGHT_VIOLATION = "world.lightViolation"

    /** UI / system. */
    const val SAVE_REQUESTED = "save.requested"
    const val SAVE_COMPLETED = "save.completed"
    const val SETTINGS_CHANGED = "settings.changed"
    const val QUALITY_CHANGED = "quality.changed"
    const val SCREEN_OPENED = "ui.screenOpened"
    const val BACK_PRESSED = "ui.backPressed"
}
