import com.soywiz.korge.view.Circle
import com.soywiz.korge.view.Line
import com.soywiz.korma.geom.Point
import com.soywiz.korma.geom.distanceTo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object GeometryExtensions {
    fun Line.rotateDegrees(degrees: Int): Line {
        val radians = degrees * 2 * PI / 360
        val x2C = x1 + (x2 - x1) * cos(radians) + (y2 - y1) * sin(radians)
        this.y2 = y1 - (x2 - x1) * sin(radians) + (y2 - y1) * cos(radians)
        this.x2 = x2C
        return this
    }

    // TODO: intersection algorithm
    fun Line.intersects(other: Connection): Boolean {
        return true
    }

    fun Circle.intersects(other: Circle): Boolean {
        return this.pos.distanceTo(other.pos) < this.radius + other.radius
    }
}