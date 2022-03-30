package components

import com.soywiz.kds.Stack
import Constants
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Cell(val position: Pair<Int, Int>, var zone: Zone) {

    lateinit var matrix: MatrixMap
    var adjacentEdges: List<Cell> = listOf()
    var cellType = CellType.EMPTY

    /**
     * checks whether cell is at the edge of a map or there is a different zone in 8 neighboring cells
     */
    fun isAtEdge(): Boolean {
        assignAdjacentEdges()
        if(adjacentEdges.isNotEmpty())
            cellType = CellType.EDGE
        return adjacentEdges.isNotEmpty()
    }

    fun checkSideNeighbors(func: (cell: Cell) -> Boolean): List<Cell> {
        val res = mutableListOf<Cell>()
        if (position.first - 1 >= 0 && func(matrix.matrix[position.first - 1, position.second]))
            res.add(matrix.matrix[position.first - 1, position.second])
        if (position.first + 1 < matrix.matrix.width && func(matrix.matrix[position.first + 1, position.second]))
            res.add(matrix.matrix[position.first + 1, position.second])
        if (position.second - 1 >= 0 && func(matrix.matrix[position.first, position.second - 1]))
            res.add(matrix.matrix[position.first, position.second - 1])
        if (position.second + 1 < matrix.matrix.height && func(matrix.matrix[position.first, position.second + 1]))
            res.add(matrix.matrix[position.first, position.second + 1])

        return res
    }

    private fun assignAdjacentEdges() {
        adjacentEdges = checkSideNeighbors { c: Cell -> c.zone != zone }
    }

    /**
     * get all adjacent cells from other zones
     */
    fun getEdge(): List<Cell> {
        val res = mutableListOf<Cell>()
        walk { i, j ->
            if (matrix.matrix[i, j].zone != zone &&
                ((i == position.first || j == position.second) && (i != position.first || j != position.second))
            )
                res.add(matrix.matrix[i, j])

        }

        return res
    }

    private fun walk(func: (x: Int, y: Int) -> Unit) {
        for (i in max(0, position.first - 1)..min(matrix.matrix.width - 1, position.first + 1))
            for (j in max(0, position.second - 1)..min(matrix.matrix.height - 1, position.second + 1))
                func(i, j)
    }

    /**
     * returns how many neighbors are obstacles divided by amount of all neighbors.
     * Ratio is needed because cells on the map edge will obviously have fewer obstacle neighbors
     */
    fun getObstacleNeighborsRatio(): Float {
        var all = 0
        var sum = 0
        walk { i, j ->
            if (i != 0 && j != 0) {
                if (Constants.OBSTACLES.contains(matrix.matrix[i, j].cellType))
                    sum += 1
                all += 1
            }
        }
        return sum.toFloat() / all
    }

    fun getNeighbors(): List<Cell> {
        val res = mutableListOf<Cell>()

        walk { i, j ->
            // take only side neighbors for now
            if ((i == position.first || j == position.second) && (i != position.first || j != position.second))
                res.add(matrix.matrix[i, j])
        }

        return res
    }

    fun getAllNeighbors(): List<Cell> {
        val res = mutableListOf<Cell>()
        walk { i, j ->
            if ((i != position.first || j != position.second))
                res.add(matrix.matrix[i, j])
        }
        return res
    }

    /**
     * TODO: might get out of matrix bounds if we return matrix[-1][i] for example
     */
    fun getOpposite(cell: Cell): Cell {
        val xAdd = position.first - cell.position.first
        val yAdd = position.second - cell.position.second
        if (matrix.matrix.width == position.first + xAdd
            || matrix.matrix.height == position.second + yAdd
            || -1 == position.first + xAdd
            || -1 == position.second + yAdd
        )
            return this
        return matrix.matrix[position.first + xAdd, position.second + yAdd]
    }

    /**
     * There might be a problem when a "bridging" cell is replaced.
     *
     * Bridging cell is the one, which when replaced makes zone split in two unconnected parts.
     */
    fun isBridgingCell(otherZone: Zone): Boolean {
        // ooo
        // xoo - that case is impossible (x is otherZone)
        // oxo case, where moving to last is not enough

        // temporarily change cell zone
        val prevZone = zone
        zone.cellSize--
        zone = otherZone
        zone.cellSize++
        assignAdjacentEdges()

        // for all 8 neighboring cells check that all cells of this zone can be connected

        // find first cell
        var first: Cell = this
        outer@ for (j in max(0, position.second - 1)..min(matrix.matrix.height - 1, position.second + 1))
            for (i in max(0, position.first - 1)..min(matrix.matrix.width - 1, position.first + 1)) {
                if (matrix.matrix[i, j].zone == prevZone) {
                    first = matrix.matrix[i, j]
                    break@outer
                }
            }
        // find last cell
        var last: Cell = this
        outer@ for (j in min(matrix.matrix.height - 1, position.second + 1)..max(0, position.second - 1))
            for (i in min(matrix.matrix.width - 1, position.first + 1)..max(0, position.first - 1)) {
                if (matrix.matrix[i, j].zone == prevZone) {
                    last = matrix.matrix[i, j]
                    break@outer
                }
            }
        // nothing to connect
        if (first == last)
            return false

        val stack = Stack<Cell>()
        val visited = mutableSetOf<Cell>()
        stack.push(first)
        // now try to connect first and last by stepping horizontally and vertically
        // bfs
        while (stack.isNotEmpty()) {
            val current = stack.pop()
            visited.add(current)
            if (current == last)
                return false
            // move right
            moveOneCell(current, Pair(1, 0), stack, visited)
            // move down
            moveOneCell(current, Pair(0, 1), stack, visited)
            // move left
            moveOneCell(current, Pair(-1, 0), stack, visited)
        }
        zone.cellSize--
        zone = prevZone
        zone.cellSize++
        assignAdjacentEdges()
        return true
    }

    private fun moveOneCell(
        origin: Cell,
        moveVector: Pair<Int, Int>,
        stack: Stack<Cell>,
        visited: MutableSet<Cell>
    ) {
        val xMove = origin.position.first + moveVector.first - position.first
        val yMove = origin.position.second + moveVector.second - position.second
        if (abs(xMove) <= 1 && abs(yMove) <= 1 && matrix.isInside(
                Pair(origin.position.first + moveVector.first, origin.position.second + moveVector.second)
            )
        ) {
            val movedCell =
                matrix.matrix[origin.position.first + moveVector.first, origin.position.second + moveVector.second]
            if (movedCell.zone == origin.zone &&
                !visited.contains(movedCell)
            ) {
                stack.push(movedCell)
                visited.add(movedCell)
            }
        }
    }
}

