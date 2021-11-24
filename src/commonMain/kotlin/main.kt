import GeometryExtensions.rotateDegrees
import com.soywiz.korge.Korge
import com.soywiz.korge.view.*
import com.soywiz.korim.color.Colors
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.distanceTo
import kotlin.math.max
import kotlin.math.min

const val height = 320
const val width = 320

suspend fun main() = Korge(
    width = width, height = height, bgcolor = Colors["#111111"]
) {

    val z0 = Zone(Biome.GRASS, 50, mutableListOf(), 0)
    val z1 = Zone(Biome.DIRT, 50, mutableListOf(), 1)
    val z2 = Zone(Biome.SNOW, 50, mutableListOf(), 2)
    val z3 = Zone(Biome.LAVA, 50, mutableListOf(), 3)
    val z4 = Zone(Biome.SWAMP, 50, mutableListOf(), 3)
    val c01 = Connection(z0, z1, ConnectionType.REGULAR, 1)
    val c02 = Connection(z0, z2, ConnectionType.REGULAR, 1)
    val c03 = Connection(z0, z3, ConnectionType.REGULAR, 1)
    val c04 = Connection(z0, z4, ConnectionType.REGULAR, 1)
    val c13 = Connection(z1, z3, ConnectionType.REGULAR, 1)
    val c14 = Connection(z1, z4, ConnectionType.REGULAR, 1)

    z0.connections.addAll(mutableListOf(c01, c02, c03, c04))
    z1.connections.addAll(mutableListOf(c01, c13, c14))
    z2.connections.add(c02)
    z3.connections.addAll(mutableListOf(c03, c13))
    z4.connections.addAll(mutableListOf(c04, c14))
    val zones = mutableListOf(z0, z1, z2, z3, z4)

    placeZoneCircles(zones, mutableListOf(c01, c02, c03,c13,c14), this)
}

fun placeFirst(zones: MutableList<Zone>, stage: Stage) {
    var angle = 0
    val z = zones.first()
    z.circle = stage.circle(z.size.toDouble(), Colors[z.type.color])
    z.centerToPoint(Point(width / 2, height / 2))

    for (i in z.connections) {
        i.getZone(z).circle = stage.circle(
            i.getZone(z).size.toDouble(),
            Colors[i.getZone(z).type.color]
        )

        angle += 360 / z.connections.size + (-120 / z.connections.size..120 / z.connections.size).random()

        i.line = stage.line(
            Point(width / 2, height / 2),
            Point(
                width / 2,
                height / 2 - z.size - i.getZone(z).size + (
                        -min(z.size / 3, i.getZone(z).size) / 3..
                                min(z.size / 3, i.getZone(z).size) / 3).random()
            )
        ).rotateDegrees(angle)

        i.getZone(z).centerToPoint(Point(i.line.x2, i.line.y2))
    }
}


fun placeZoneCircles(zones: MutableList<Zone>, connections: List<Connection>, stage: Stage) {
    val resolved = mutableListOf<Zone>()
    zones.sortBy { it.index }
    for (i in zones) {
        i.connections.sortBy { it.type }
    }

    placeFirst(zones, stage)

    for (i in 1..zones.lastIndex) {
        resolveZone(zones[i], stage)
        resolved.add(zones[i])
    }

    // trying to find a function which will check if lines intersect.

//    if(connections[3].line.collidesWithShape(connections[4].line)){
//        println("HOORAY!")
//    }
}

fun resolveZone(zone: Zone, stage: Stage) {

    for (i in zone.getPlaced()) {
        i.getConnection(zone).line = stage.line(zone.getCenter(), i.getCenter())

        // TODO: draw conenection here and check if it intersects something.
        // OR check after this if
        if (zone.circle.pos.distanceTo(i.circle.pos) >= 3.5 * max(zone.size, i.size)) {
            i.toNearestValidPosition()
        }

    }
}