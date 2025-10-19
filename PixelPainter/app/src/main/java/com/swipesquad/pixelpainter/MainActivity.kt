package com.swipesquad.pixelpainter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.swipesquad.pixelpainter.ui.theme.PixelPainterTheme
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PixelPainterTheme {
                PixelMalerApp()
            }
        }
    }
}

//Constants
private const val GRID_SIZE = 13
private const val CELL_COUNT = GRID_SIZE * GRID_SIZE
private val CELL_SIZE_DP = 28.dp
private val CELL_BORDER = 1.dp

private val Blue = Color(0xFF1364B7)
private val Green = Color(0xFF13B717)
private val Yellow = Color(0xFFFFEA00)
private val Red = Color(0xFFD90505)
private val Black = Color(0xFF000000)
private val Gray = Color(0xFFB7B7B7)

private val White = Color(0xFFFFFFFF)

//Data Class for a Cell
data class CellState(var color: MutableState<Color>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PixelMalerApp() {
    val context = LocalContext.current

    // Grid state: 169 cells
    val cells = remember {
        mutableStateListOf<CellState>().apply {
            repeat(CELL_COUNT) { add(CellState(mutableStateOf(White))) }
        }
    }

    // Selected color palette, default blue
    var selectedColor by remember { mutableStateOf(Blue) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pixel Painter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {
                        val json = buildGridJson(cells)

                        val log = JSONObject()
                        log.put("task", "Pixelmaler")
                        log.put("pixels", json)

                        sendLogbookIntent(context, log.toString())
                    }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.primary,
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send to logbook"
                        )
                    }
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PixelGrid(
                    cells = cells,
                    onCellClick = { index ->
                        cells[index].color.value = selectedColor
                    }
                )

                Spacer(Modifier.height(12.dp))

                PaletteRow(selectedColor = selectedColor, onColorSelected = { selectedColor = it })

                Spacer(Modifier.height(12.dp))

                // Actions row
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        // Clear all cells
                        for (i in 0 until CELL_COUNT) cells[i].color.value = White
                    }) {
                        Text("Clear")
                    }
                }
            }
        }
    )
}

@Composable
fun PaletteRow(selectedColor: Color, onColorSelected: (Color) -> Unit) {
    val palette = listOf(Blue, Green, Yellow, Red, Black, Gray, White)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        palette.forEach { color ->
            val isSelected = color == selectedColor
            val border =
                if (isSelected) BorderStroke(2.dp, Color.Black) else BorderStroke(1.dp, Color.Gray)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(border, shape = MaterialTheme.shapes.small)
                    .background(color = color)
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = if (color == White) "E" else "",
                        color = if (color == Black) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun PixelGrid(cells: List<CellState>, onCellClick: (Int) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_SIZE),
        modifier = Modifier
            .size(
                width = (CELL_SIZE_DP * GRID_SIZE) + 2.dp * GRID_SIZE,
                height = (CELL_SIZE_DP * GRID_SIZE) + 2.dp * GRID_SIZE
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.Center,
        content = {
            itemsIndexed(cells) { index, cell ->
                CellView(cellState = cell, cellIndex = index, onClicked = onCellClick)
            }
        }
    )
}

@Composable
fun CellView(cellState: CellState, cellIndex: Int, onClicked: (Int) -> Unit) {
    val color by cellState.color
    Box(
        modifier = Modifier
            .size(CELL_SIZE_DP)
            .padding(0.5.dp)
            .background(color)
            .border(CELL_BORDER, Color.LightGray)
            .clickable { onClicked(cellIndex) }
    )
}

/**
 * Build JSON describing the colored cells in the grid.
 *
 * Output format: JSON array of objects like:
 * [
 *   { "y": 3, "x": 5, "color": "#FF1364B7" },
 *   ...
 * ]
 *
 * Only non-white cells are included.
 * Coordinates: x = column (0..12), y = row (0..12). (0,0) is top-left.
 */
fun buildGridJson(cells: List<CellState>): JSONArray {
    val arr = JSONArray()
    for (index in cells.indices) {
        val c = cells[index].color.value
        // Skip white/transparent background
        if (isColorWhite(c)) continue

        val x = index % GRID_SIZE
        val y = index / GRID_SIZE

        val argbInt = c.toArgb()
        val hex = intToARGBHex(argbInt)

        val obj = JSONObject()
        obj.put("y", y.toString())
        obj.put("x", x.toString())
        obj.put("color", hex)
        arr.put(obj)
    }
    return arr
}

fun isColorWhite(color: Color): Boolean {
    return color.toArgb() == White.toArgb()
}

fun intToARGBHex(argb: Int): String {
    return String.format("#%08X", argb)
}

fun sendLogbookIntent(context: Context, value: String) {
    val intent = Intent("ch.apprun.intent.LOG").apply {
        putExtra("ch.apprun.logmessage", value)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Log.e("Logger", "LogBook application is not installed on this device.")
    }
}