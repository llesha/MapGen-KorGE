import com.soywiz.korge.view.Circle
import com.soywiz.korge.view.circle
import com.soywiz.korge.view.xy
import com.soywiz.korma.geom.Point
import kotlin.random.Random

/**
 * Sizes are computed proportionally to each other.
 * Zone placement starts with 0 index-zone.
 */
class Zone constructor(var type: Biome, val size: Int, val connections: MutableList<Connection>, val index: Int) {
    lateinit var circle: Circle

    /**
     * Will always return existing connection in scope of algorithm
     */
    fun getConnection(zone: Zone): Connection {
        return connections.find { it.z1 === zone || it.z2 === zone }!!
    }

    init {
        if (type == Biome.RANDOM) {
            type = Biome.fromInt((1..5).random())
        }
    }

    fun centerToPoint(source: Point) {
        circle.xy(Point(source.x - size, source.y - size))
    }

    fun getCenter(): Point = Point(circle.x + size, circle.y + size)

    fun getPlaced(): List<Zone> {
        return connections.filter { it.getZone(this)::circle.isInitialized }.map { it.getZone(this) }
    }

    fun sortByPlaced() {
        connections.sortBy { it.getZone(this)::circle.isInitialized }
    }

    fun toNearestValidPosition() {
        val references = getPlaced()
        if (references.size == 1)
            return
        val point = Point(0, 0)
        references.onEach {

            point.x += it.circle.x
            point.y += it.circle.y
        }
        point.x /= references.size
        point.y /= references.size

        circle.xy(point)

        if (references.size == 2) {
            circle.xy(
                Point(
                    references[0].circle.y - references[1].circle.y,
                    -references[0].circle.x + references[1].circle.x
                ) + circle.pos
            )
        }

        for (i in references) {
            getConnection(i).line.x2 = circle.x + size
            getConnection(i).line.y2 = circle.y + size
        }
    }
}

enum class Biome(val color: String) {
    RANDOM("#808080"),
    DIRT("#964B00"),
    GRASS("#378805"),
    LAVA("#FF0000"),
    SNOW("#E6E1E1"),
    SWAMP("#5D6A00");

    companion object {
        fun fromInt(value: Int): Biome {
            return when (value) {
                0 -> RANDOM
                1 -> DIRT
                2 -> GRASS
                3 -> LAVA
                4 -> SNOW
                5 -> SWAMP
                else -> throw IllegalArgumentException("No zoneType for that")
            }
        }
    }
}