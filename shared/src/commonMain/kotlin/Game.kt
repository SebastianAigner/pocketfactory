import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Duration.Companion.seconds

class Game {

}

enum class Direction {
    UP, DOWN, LEFT, RIGHT
}

fun Direction.toEmoji(): String {
    return when (this) {
        Direction.UP -> "⬆️"
        Direction.DOWN -> "⬇️"
        Direction.LEFT -> "⬅️"
        Direction.RIGHT -> "➡️"
    }
}

fun Direction.toVec2(): Vec2 {
    return when (this) {
        Direction.UP -> Vec2(0, -1)
        Direction.DOWN -> Vec2(0, 1)
        Direction.LEFT -> Vec2(-1, 0)
        Direction.RIGHT -> Vec2(1, 0)
    }
}

fun Direction.rotate(): Direction {
    return when (this) {
        Direction.UP -> Direction.RIGHT
        Direction.DOWN -> Direction.LEFT
        Direction.LEFT -> Direction.UP
        Direction.RIGHT -> Direction.DOWN
    }
}

data class Vec2(val x: Int, val y: Int) {
    operator fun plus(other: Vec2) = Vec2(x + other.x, y + other.y)
}

class Board {
    private val _conveyors = MutableStateFlow<Map<Vec2, Conveyor>>(emptyMap())
    val conveyors: StateFlow<Map<Vec2,Conveyor>> get() = _conveyors
    val scope = CoroutineScope(Dispatchers.Default /*+ SupervisorJob()*/)

    fun modifyCell(x: Int, y: Int) {
        findConveyorAt(Vec2(x,y))?.let {
            it.rotate()
            println("Now facing ${it.direction}")
        } ?:
        run {
            val conveyor = Conveyor(scope, Direction.UP, this)
            place(conveyor, Vec2(x, y))
            conveyor.run()
            return
        }
    }

    private fun findConveyorAt(coord: Vec2): Conveyor? {
        return _conveyors.value[coord]
    }

    private fun locationForConveyor(c: Conveyor): Vec2 {
        return _conveyors.value.entries.first { it.value == c }.key
    }

    suspend fun emit(from: Conveyor, into: Direction, item: Item) {
        val newCoord = into.toVec2() + locationForConveyor(from)
        while (true) {
            val newConveyor = findConveyorAt(newCoord)
            if (newConveyor != null) {
                println("Item $item emitted from $from into $into ($newCoord) (conveyor $newConveyor)")
                newConveyor.accept(item)
                return
            } else {
                println("Can't emit item anywhere -- waiting to retry")
                delay(500)
            }
        }
    }

    fun place(c: Conveyor, at: Vec2) {
        _conveyors.update {
            it + (at to c)
        }
    }
}

class Item(val id: Int)

data class ItemProgress(val item: Item, var progress: Double)

class Conveyor(val scope: CoroutineScope, direction: Direction, val board: Board) {
    fun rotate() {
        _direction.update {
            it.rotate()
        }
    }
    private val _direction = MutableStateFlow<Direction>(direction)
    val direction: StateFlow<Direction> get() = _direction

    private val _itemsOnBelt = MutableStateFlow<List<ItemProgress>>(emptyList())
    val itemsOnBelt: StateFlow<List<ItemProgress>> get() = _itemsOnBelt

    suspend fun accept(item: Item) {
        _itemsOnBelt.update {
            it + ItemProgress(item, 0.0)
        }
    }

    fun run() {
        scope.launch {
            while (true) {
                delay(1.seconds / 60)
                _itemsOnBelt.update {
                    it.map { it.copy(progress = it.progress + 1.0 / 60) }
                }
                val itemsToEmit = _itemsOnBelt.value.filter { it.progress >= 1.0 }
                itemsToEmit.forEach {
                    scope.launch {
                        board.emit(this@Conveyor, direction.value, it.item)
                    }
                }
                _itemsOnBelt.update {
                    it.filter { it !in itemsToEmit }
                }
            }
        }
    }
}

suspend fun main() {
    val b = Board()
    val c = Conveyor(CoroutineScope(Dispatchers.Default), Direction.RIGHT, b)
    val c2 = Conveyor(CoroutineScope(Dispatchers.Default), Direction.RIGHT, b)
    b.place(c, Vec2(0, 0))
    b.place(c2, Vec2(1, 0))
    c.run()
    c2.run()
    c.accept(Item(1))
    delay(5000)
}