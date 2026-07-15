package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Scaffold
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class CellState(
    val id: Int,
    val x: Int,
    val y: Int,
    val direction: Direction? = null,
    val isDot: Boolean = true
)

class GameViewModel : ViewModel() {
    private val _grid = MutableStateFlow<List<CellState>>(emptyList())
    val grid: StateFlow<List<CellState>> = _grid.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level: StateFlow<Int> = _level.asStateFlow()

    private val _moves = MutableStateFlow(0)
    val moves: StateFlow<Int> = _moves.asStateFlow()

    init {
        restartLevel()
    }

    fun restartLevel() {
        _moves.value = 0
        _grid.value = createSolvableBoard()
    }

    fun removeCell(cell: CellState) {
        if (cell.direction == null || isBlocked(cell, _grid.value)) return

        val updatedGrid = _grid.value.map { current ->
            if (current.id == cell.id) {
                current.copy(direction = null, isDot = true)
            } else {
                current
            }
        }

        _moves.update { it + 1 }

        if (updatedGrid.none { it.direction != null }) {
            _level.update { it + 1 }
            _moves.value = 0
            _grid.value = createSolvableBoard()
        } else {
            _grid.value = updatedGrid
        }
    }

    private fun createSolvableBoard(): List<CellState> {
        val board = Array(GRID_SIZE) { y ->
            Array(GRID_SIZE) { x ->
                CellState(id = y * GRID_SIZE + x, x = x, y = y)
            }
        }

        var arrowsPlaced = 0
        var attempts = 0

        while (arrowsPlaced < TARGET_ARROWS && attempts < MAX_GENERATION_ATTEMPTS) {
            attempts++
            val x = Random.nextInt(GRID_SIZE)
            val y = Random.nextInt(GRID_SIZE)

            if (board[y][x].direction != null) continue

            val possibleDirections = Direction.values().filter { direction ->
                pathToEdgeIsClear(x, y, direction, board)
            }

            if (possibleDirections.isNotEmpty()) {
                board[y][x] = board[y][x].copy(
                    direction = possibleDirections.random(),
                    isDot = false
                )
                arrowsPlaced++
            }
        }

        return board.flatten()
    }

    private fun pathToEdgeIsClear(
        startX: Int,
        startY: Int,
        direction: Direction,
        board: Array<Array<CellState>>
    ): Boolean {
        var x = startX
        var y = startY

        while (true) {
            when (direction) {
                Direction.UP -> y--
                Direction.DOWN -> y++
                Direction.LEFT -> x--
                Direction.RIGHT -> x++
            }

            if (x !in 0 until GRID_SIZE || y !in 0 until GRID_SIZE) return true
            if (board[y][x].direction != null) return false
        }
    }

    companion object {
        const val GRID_SIZE = 5
        private const val TARGET_ARROWS = 20
        private const val MAX_GENERATION_ATTEMPTS = 2_000
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val gameViewModel: GameViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TopBar(gameViewModel) },
                    bottomBar = { BottomBar() }
                ) { innerPadding ->
                    GameScreen(
                        viewModel = gameViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(viewModel: GameViewModel) {
    val level by viewModel.level.collectAsStateWithLifecycle()
    val moves by viewModel.moves.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = level.toString(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "Level $level",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$moves moves",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(
            onClick = viewModel::restartLevel,
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart level",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BottomBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavButton(icon = Icons.Default.PlayArrow, text = "Play", selected = true)
        BottomNavButton(icon = Icons.Default.BarChart, text = "Stats", selected = false)
        BottomNavButton(icon = Icons.Default.Event, text = "Events", selected = false)
    }
}

@Composable
fun BottomNavButton(icon: ImageVector, text: String, selected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(alpha = if (selected) 1f else 0.6f)
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) {
                MaterialTheme.colorScheme.onBackground
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val grid by viewModel.grid.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 340.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (y in 0 until GameViewModel.GRID_SIZE) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (x in 0 until GameViewModel.GRID_SIZE) {
                            val cell = grid.firstOrNull { it.x == x && it.y == y }
                                ?: CellState(id = y * GameViewModel.GRID_SIZE + x, x = x, y = y)

                            CellView(
                                cell = cell,
                                isBlocked = isBlocked(cell, grid),
                                onCellRemoved = { viewModel.removeCell(cell) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        ActionButton(
            icon = Icons.Default.Refresh,
            text = "RESTART",
            onClick = viewModel::restartLevel
        )
    }
}

fun isBlocked(cell: CellState, grid: List<CellState>): Boolean {
    val direction = cell.direction ?: return true
    var x = cell.x
    var y = cell.y

    while (true) {
        when (direction) {
            Direction.UP -> y--
            Direction.DOWN -> y++
            Direction.LEFT -> x--
            Direction.RIGHT -> x++
        }

        if (x !in 0 until GameViewModel.GRID_SIZE || y !in 0 until GameViewModel.GRID_SIZE) {
            return false
        }

        val cellInPath = grid.firstOrNull { it.x == x && it.y == y }
        if (cellInPath?.direction != null) return true
    }
}

@Composable
fun CellView(
    cell: CellState,
    isBlocked: Boolean,
    onCellRemoved: () -> Unit
) {
    val offset = remember(cell.id, cell.direction) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                translationX = when (cell.direction) {
                    Direction.LEFT -> -offset.value
                    Direction.RIGHT -> offset.value
                    else -> 0f
                }
                translationY = when (cell.direction) {
                    Direction.UP -> -offset.value
                    Direction.DOWN -> offset.value
                    else -> 0f
                }
            }
            .clip(RoundedCornerShape(16.dp))
            .background(
                when {
                    cell.direction != null -> MaterialTheme.colorScheme.surface
                    cell.isDot -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = cell.direction != null) {
                scope.launch {
                    if (isBlocked) {
                        offset.animateTo(15f, tween(100))
                        offset.animateTo(-15f, tween(100))
                        offset.animateTo(0f, tween(100))
                    } else {
                        offset.animateTo(1_000f, tween(350))
                        onCellRemoved()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (cell.direction != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 2.dp,
                        shape = RoundedCornerShape(16.dp),
                        spotColor = Color.Black.copy(alpha = 0.1f)
                    )
            )

            val icon = when (cell.direction) {
                Direction.UP -> Icons.Default.ArrowUpward
                Direction.DOWN -> Icons.Default.ArrowDownward
                Direction.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                Direction.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
            }

            Icon(
                imageVector = icon,
                contentDescription = "Arrow ${cell.direction.name.lowercase()}",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        } else if (cell.isDot) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer)
            )
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp
        )
    }
}
