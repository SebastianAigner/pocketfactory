import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun GridCell(x: Int, y: Int, color: Color = Color.Black) {
    // 32.dp x 32.dp
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(color)
            .border(1.dp, Color.White)
    ) {
    }
}

@Composable
fun ConveyorItem(item: Item) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .background(Color.White)
            .border(1.dp, Color.Red)
    ) {
        Text("${item.id}", color = Color.Blue)
    }
}

@Composable
fun ConveyorItems(conveyor: Conveyor) {
    val conveyorItems = conveyor.itemsOnBelt.collectAsState()
    val direction = conveyor.direction.collectAsState()
    for (conveyorItem in conveyorItems.value) {
        val center = DpOffset(8.dp, 8.dp)
        val offset = when (direction.value) {
            Direction.UP -> DpOffset(center.x, 32.dp - center.y - conveyorItem.progress * 32.dp)
            Direction.DOWN -> DpOffset(center.x, -center.y + conveyorItem.progress * 32.dp)
            Direction.LEFT -> DpOffset(32.dp - center.x - conveyorItem.progress * 32.dp, center.y)
            Direction.RIGHT -> DpOffset(-center.x + conveyorItem.progress * 32.dp, center.y)
        }
        Box(Modifier.absoluteOffset(offset.x, offset.y)) {
            ConveyorItem(conveyorItem.item)
        }
    }
}

@Composable
fun ConveyorCell(x: Int, y: Int, conveyor: Conveyor) {
    // 32.dp x 32.dp
    val conveyorItems = conveyor.itemsOnBelt.collectAsState()
    val direction = conveyor.direction.collectAsState()
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(Color.Blue)
            .border(1.dp, Color.White)
    ) {
        Text(direction.value.toEmoji(), color = Color.White)
    }
}

val board = Board()
suspend fun startGame() {
    val cs = CoroutineScope(Dispatchers.Default)
    val c = Conveyor(cs, Direction.RIGHT, board)
    val c2 = Conveyor(cs, Direction.DOWN, board)
    val c3 = Conveyor(cs, Direction.LEFT, board)
    val c4 = Conveyor(cs, Direction.UP, board)
    board.place(c, Vec2(0, 0))
    board.place(c2, Vec2(1, 0))
    board.place(c3, Vec2(1, 1))
    board.place(c4, Vec2(0, 1))
    listOf(c, c2, c3, c4).forEach { it.run() }
    c.accept(Item(1))
}

@Composable
fun GridLayer(cell: @Composable (i: Int, j: Int) -> Unit) {
    Column {
        for (i in 0..10) {
            Row {
                for (j in 0..10) {
                    cell(i, j)
                }
            }
        }
    }
}

@Composable
fun FactoryGame() {
    val conveyors = board.conveyors.collectAsState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        startGame()
    }
    Column {
        Box {
            GridLayer(cell = { i, j ->
                GridCell(j, i, Color.Black)
            })
            GridLayer(cell = { i, j ->
                conveyors.value[Vec2(j, i)]?.let {
                    Box {
                        ConveyorCell(j, i, it)
                    }
                } ?: GridCell(j, i, Color.Transparent)
            })
            GridLayer(cell = { i, j ->
                conveyors.value[Vec2(j, i)]?.let {
                    Box {
                        GridCell(j, i, Color.Transparent)
                        ConveyorItems(it)
                    }
                } ?: GridCell(j, i, Color.Transparent)
            })
            GridLayer(cell = { i, j ->
                ClickableCell(j, i, onClick = {
                    board.modifyCell(j, i)
                }, onLongClick = {
//                    scope.launch {
//                        board.addItem(j, i, Item(Random.nextInt()))
//                    }
                    board.useTool(j, i)
                })
            })
        }
        MenuBar(board)
        val tool = board.selectedTool.collectAsState()
        Text("Selected tool: ${tool.value}")
    }
}


@Composable
fun MenuBar(board: Board) {
    Row {
        Button(onClick = {
            board.selectTool(SelectedTool.CONVEYOR)
        }) {
            Text("Conveyor")
        }
        Button(onClick = {
            board.selectTool(SelectedTool.SPAWNER)
        }) {
            Text("Spawner")
        }
        Button(onClick = {
            board.selectTool(SelectedTool.CONSUMER)
        }) {
            Text("Consumer")
        }
        Button(onClick = {
            board.selectTool(SelectedTool.BOX)
        }) {
            Text("Box")
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickableCell(x: Int, y: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    Box(
        modifier = Modifier
            .combinedClickable(
                onClick = {
                    println("Clicked $x, $y")
                    onClick()
                },
                onLongClick = {
                    onLongClick()
                })
            .size(32.dp)
            .background(Color.Transparent)
            .border(1.dp, Color.White)
    ) {
    }
}