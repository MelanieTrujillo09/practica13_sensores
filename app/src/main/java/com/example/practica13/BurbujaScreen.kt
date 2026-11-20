package com.example.practica13

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BurbujaScreen(sensorManager: SensorManager) {
    // Posición de la burbuja
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Valores reales del acelerómetro (m/s²) en los 3 ejes
    var ejeX by remember { mutableFloatStateOf(0f) }
    var ejeY by remember { mutableFloatStateOf(0f) }
    var ejeZ by remember { mutableFloatStateOf(0f) }

    // Detección de sacudida
    var sacudido by remember { mutableStateOf(false) }
    var contadorSacudidas by remember { mutableIntStateOf(0) }
    var fuerzaG by remember { mutableFloatStateOf(0f) }

    val sensibilidad = 50f
    val radioMaximo = 250f
    val umbralSacudida = 1.05f      // fuerza G necesaria para considerarlo sacudida
    val tiempoEntreSacudidas = 1000L // evita contar la misma sacudida varias veces
    var ultimaSacudida by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    // 1. Lecturas de los tres ejes
                    val x = it.values[0]
                    val y = it.values[1]
                    val z = it.values[2]
                    ejeX = x
                    ejeY = y
                    ejeZ = z

                    // 2. Movimiento de la burbuja (limitada al círculo)
                    val rawX = -x * sensibilidad
                    val rawY = y * sensibilidad
                    val distancia = sqrt((rawX * rawX) + (rawY * rawY))
                    if (distancia > radioMaximo) {
                        val proporcion = radioMaximo / distancia
                        offsetX = rawX * proporcion
                        offsetY = rawY * proporcion
                    } else {
                        offsetX = rawX
                        offsetY = rawY
                    }

                    // 3. Detección de sacudida: aceleración vectorial en G
                    val magnitud = sqrt((x * x) + (y * y) + (z * z))
                    val g = magnitud / SensorManager.GRAVITY_EARTH
                    fuerzaG = g

                    val ahora = System.currentTimeMillis()
                    if (g > umbralSacudida && (ahora - ultimaSacudida) > tiempoEntreSacudidas) {
                        ultimaSacudida = ahora
                        contadorSacudidas++
                        sacudido = true
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        val acelerometro = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // La respuesta visual dura 1 segundo y regresa a la normalidad
    LaunchedEffect(sacudido) {
        if (sacudido) {
            delay(1000)
            sacudido = false
        }
    }

    val colorFondo by animateColorAsState(
        targetValue = if (sacudido) Color(0xFFD32F2F) else Color.DarkGray,
        label = "colorFondo"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sensores: Burbuja y Sacudida") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(colorFondo),
            contentAlignment = Alignment.Center
        ) {
            // Mensaje flotante al sacudir
            if (sacudido) {
                Text(
                    text = "¡SACUDIDA DETECTADA!",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp)
                )
            }

            // Círculo exterior
            Box(
                modifier = Modifier
                    .size(250.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Marca central
                Box(modifier = Modifier.size(50.dp).border(2.dp, Color.Red, CircleShape))

                // Burbuja verde
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .size(40.dp)
                        .background(Color(0xFF00FF00), CircleShape)
                )
            }

            // Valores de los tres ejes
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Eje X: ${"%.2f".format(ejeX)}", color = Color.White, fontSize = 20.sp)
                Text("Eje Y: ${"%.2f".format(ejeY)}", color = Color.White, fontSize = 20.sp)
                Text("Eje Z: ${"%.2f".format(ejeZ)}", color = Color.White, fontSize = 20.sp)
                Text("Fuerza: ${"%.2f".format(fuerzaG)} G", color = Color.Yellow, fontSize = 16.sp)
                Text("Sacudidas: $contadorSacudidas", color = Color.Yellow, fontSize = 16.sp)
            }
        }
    }
}