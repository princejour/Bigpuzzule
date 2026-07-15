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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
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
    val isDot: Boolean = false,
    val isAnimatingOut: Boolean = false,
    val translationX: Float = 0f,
    val translationY: Float = 0f
)

class GameViewModel : ViewModel() {
    private val _grid = MutableStateFlow<List<CellState>>(emptyList())
    val grid: StateFlow<List<CellState>> = _grid.asStateFlow()

    private val _level = MutableStateFlow(1)
    val level = _level.asStateFlow()
    
    private val _moves = MutableStateFlow(0)
    val moves = _moves.asStateFlow()

    init {
        generateLevel()
    }

    fun generateLevel() {
        _moves.value = 0
        val size = 5
        // Start with empty board
        val board = Array(size) { Array(size) { CellState(0, 0, 0) } }
        for (y in 0 until size) {
            for (x in 0 until size) {
                board[y][x] = CellState(id = y * size + x, x = x, y = y, isDot = true) // start all as dots
            }
        }

        // Generate backwards: place arrows that can "fly in" from the edges.
        // Or simpler: generate random arrows and see if it's solvable. If not, retry.
        // Actually, reverse generation is 100% reliable.
        val arrowPositions = mutableListOf<CellState>()
        
        // We will place 20 arrows.
        val targetArrows = 20
        var attempts = 0
        while (arrowPositions.size < targetArrows && attempts < 1000) {
            attempts++
            // Pick a random spot that is a dot (meaning empty in reverse generation)
            val x = Random.nextInt(size)
            val y = Random.nextInt(size)
            if (!board[y][x].isDot) continue // already an arrow

            // Check which directions are "clear" to the edge for it to have flown IN from
            // Since it's reverse, "flying in" means it came from the edge to this spot, so the path to the edge in the OPPOSITE direction of the arrow must be clear.
            // Wait, if an arrow is pointing UP, it flies UP. In reverse, it comes from UP and lands here. So the path UP must be clear of arrows.
            // So we check if the path UP is clear of arrows. If so, we can place an UP arrow.
            val possibleDirs = Direction.values().filter { dir ->
                var clear = true
                var cx = x
                var cy = y
                while (true) {
                    when (dir) {
                        Direction.UP -> cy--
                        Direction.DOWN -> cy++
                        Direction.LEFT -> cx--
                        Direction.RIGHT -> cx++
                    }
                    if (cx !in 0 until size || cy !in 0 until size) break
                    if (!board[cy][cx].isDot) {
                        clear = false
                        break
                    }
                }
                clear
            }

            if (possibleDirs.isNotEmpty()) {
                val chosenDir = possibleDirs.random()
                board[y][x] = board[y][x].copy(isDot = false, direction = chosenDir)
                arrowPositions.add(board[y][x])
            }
        }

        _grid.value = board.flatten()
        _level.update { it + 1 }
    }

    fun onCellClicked(cell: CellState, onAnimate: suspend (CellState, Float, Float) -> Unit) {
        if (cell.isDot || cell.direction == null || cell.isAnimatingOut) return

        val size = 5
        var cx = cell.x
        var cy = cell.y
        var blocked = false

        while (true) {
            when (cell.direction) {
                Direction.UP -> cy--
                Direction.DOWN -> cy++
                Direction.LEFT -> cx--
                Direction.RIGHT -> cx++
            }
            if (cx !in 0 until size || cy !in 0 until size) break
            
            val checkCell = _grid.value.find { it.x == cx && it.y == cy }
            if (checkCell != null && (checkCell.direction != null || checkCell.isDot)) {
                blocked = true
                break
            }
        }

        if (!blocked) {
            _moves.update { it + 1 }
            // Launch animation from UI side, then update state
        } else {
            // Shake animation or just ignore
        }
    }
    
    fun removeCell(cell: CellState) {
        _grid.update { currentGrid ->
            currentGrid.map {
                if (it.id == cell.id) it.copy(direction = null, isDot = true) else it
            }
        }
        
        // Check win
        if (_grid.value.none { it.direction != null }) {
            generateLevel()
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = { TopBar() },
                    bottomBar = { BottomBar() }
                ) { innerPadding ->
                    GameScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TopBar() {
    val viewModel: GameViewModel = viewModel()
    val level by viewModel.level.collectAsState()
    
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
                    text = "Medium Difficulty",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(
            onClick = { viewModel.generateLevel() },
            modifier = Modifier
                .size(44.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart",
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
fun BottomNavButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, selected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.alpha(if (selected) 1f else 0.6f)
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun Modifier.alpha(alpha: Float) = this.graphicsLayer(alpha = alpha)

@Composable
fun GameScreen(modifier: Modifier = Modifier, viewModel: GameViewModel = viewModel()) {
    val grid by viewModel.grid.collectAsState()
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                for (y in 0 until 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (x in 0 until 5) {
                            val cell = grid.find { it.x == x && it.y == y } ?: CellState(0,x,y,isDot = true)
                            val isBlocked = checkBlocked(cell, grid)
                            CellView(
                                cell = cell,
                                isBlocked = isBlocked,
                                onCellClicked = { 
                                    if (!isBlocked && cell.direction != null) {
                                        viewModel.removeCell(cell)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ActionButton(icon = Icons.Default.Refresh, text = "UNDO")
            ActionButton(icon = Icons.Default.Settings, text = "HINT")
        }
    }
}

fun checkBlocked(cell: CellState, grid: List<CellState>): Boolean {
    if (cell.direction == null) return true
    var cx = cell.x
    var cy = cell.y
    while(true) {
        when (cell.direction) {
            Direction.UP -> cy--
            Direction.DOWN -> cy++
            Direction.LEFT -> cx--
            Direction.RIGHT -> cx++
        }
        if (cx !in 0 until 5 || cy !in 0 until 5) break
        val checkCell = grid.find { it.x == cx && it.y == cy }
        if (checkCell != null && (checkCell.direction != null || checkCell.isDot)) {
            return true
        }
    }
    return false
}

@Composable
fun CellView(cell: CellState, onCellClicked: (Boolean) -> Unit, isBlocked: Boolean) {
    val offset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .graphicsLayer {
                translationX = if (cell.direction == Direction.LEFT) -offset.value else if (cell.direction == Direction.RIGHT) offset.value else 0f
                translationY = if (cell.direction == Direction.UP) -offset.value else if (cell.direction == Direction.DOWN) offset.value else 0f
            }
            .clip(RoundedCornerShape(16.dp))
            .background(if (cell.direction != null) MaterialTheme.colorScheme.surface else if (cell.isDot) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = cell.direction != null) {
                if (isBlocked) {
                    scope.launch {
                        offset.animateTo(15f, tween(100))
                        offset.animateTo(-15f, tween(100))
                        offset.animateTo(0f, tween(100))
                    }
                } else {
                    scope.launch {
                        offset.animateTo(1000f, tween(500))
                        onCellClicked(false)
                        offset.snapTo(0f)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (cell.direction != null) {
            Box(modifier = Modifier.fillMaxSize().shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.1f)))
            val icon = when (cell.direction) {
                Direction.UP -> Icons.Default.ArrowUpward
                Direction.DOWN -> Icons.Default.ArrowDownward
                Direction.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                Direction.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
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
fun ActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Column(
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .width(60.dp),
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
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 1.sp
        )
    }
}
