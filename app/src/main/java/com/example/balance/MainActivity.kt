package com.example.balance

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LaberintoScreen(this)
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun LaberintoScreen(contexto: Context) {
    // 1. GESTIÓN DE SENSORES (Igual que antes, nuestro "motor" de movimiento)
    val admonSensores = remember { contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val acelerometro = remember { admonSensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var posX by remember { mutableStateOf(150f) }
    var posY by remember { mutableStateOf(150f) }
    val radioPelota = 35f // La hicimos un pelín más pequeña para que quepa en los pasillos
    val velocidad = 1.8f

    // 2. EL MAPA: Una lista de rectángulos (paredes)
    // Usamos 'Rect' para definir (izquierda, arriba, derecha, abajo)
    val paredes = remember {
        listOf(
            androidx.compose.ui.geometry.Rect(0f, 300f, 600f, 350f),   // Pared horizontal superior
            androidx.compose.ui.geometry.Rect(400f, 600f, 1000f, 650f), // Pared horizontal inferior
            androidx.compose.ui.geometry.Rect(400f, 350f, 450f, 600f),  // Columna vertical
            androidx.compose.ui.geometry.Rect(700f, 0f, 750f, 450f)     // Otra columna
        )
    }

    val listener = remember {
        object : SensorEventListener {
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            override fun onSensorChanged(event: SensorEvent) {
                // Calculamos hacia dónde se quiere mover la pelota en este instante
                val nuevaX = posX - (event.values[0] * velocidad)
                val nuevaY = posY + (event.values[1] * velocidad)

                // Creamos el "área de impacto" de la pelota en su futura posición
                val areaPelotaFutura = androidx.compose.ui.geometry.Rect(
                    nuevaX - radioPelota, nuevaY - radioPelota,
                    nuevaX + radioPelota, nuevaY + radioPelota
                )

                // 3. DETECCIÓN GLOBAL DE COLISIONES
                // '.any' recorre toda la lista de paredes.
                // Devuelve 'true' si la pelota choca con AL MENOS UNA pared.
                val chocaConAlgo = paredes.any { pared -> pared.overlaps(areaPelotaFutura) }

                // Si NO choca con ninguna de las paredes de la lista, actualizamos la posición
                if (!chocaConAlgo) {
                    posX = nuevaX
                    posY = nuevaY
                }
            }
        }
    }

    // 4. REGISTRO DEL SENSOR (Encender/Apagar para ahorrar batería)
    DisposableEffect(Unit) {
        admonSensores.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_GAME)
        onDispose { admonSensores.unregisterListener(listener) }
    }

    // 5. EL LIENZO (Donde dibujamos todo)
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val anchoPantalla = constraints.maxWidth.toFloat()
        val altoPantalla = constraints.maxHeight.toFloat()

        // Ajustamos la pelota para que no se salga de los bordes del celular
        posX = posX.coerceIn(radioPelota, anchoPantalla - radioPelota)
        posY = posY.coerceIn(radioPelota, altoPantalla - radioPelota)

        Canvas(modifier = Modifier.fillMaxSize()) {
            // DIBUJAR TODAS LAS PAREDES
            // Recorremos la lista y dibujamos cada una con un color más "elegante"
            paredes.forEach { pared ->
                drawRect(
                    color = Color(0xFF333333), // Un gris oscuro casi negro
                    topLeft = pared.topLeft,
                    size = pared.size
                )
            }

            // DIBUJAR LA PELOTA (Con el estilo que aprendimos antes)
            drawCircle(
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.Cyan, Color(0xFF0055FF)), // Ahora azul cian para variar
                    center = Offset(posX - 8f, posY - 8f),
                    radius = radioPelota
                ),
                radius = radioPelota,
                center = Offset(posX, posY)
            )
        }
    }
}