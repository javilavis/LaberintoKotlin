package com.example.balance

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.androidgamesdk.gametextinput.Settings
import java.security.MessageDigest
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
    val admonSensores = remember { contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val acelerometro = remember { admonSensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    var posX by remember { mutableStateOf(-1f) }
    var posY by remember { mutableStateOf(-1f) }
    var juegoGanado by remember { mutableStateOf(false) }

    val radioPelota = 28f // Un poco más pequeña para pasillos finos
    val velocidad = 2.4f

    val uuid = "d2aca40cee063efa414b3126cd04fb720c90f701eb1e87f3c5ab8f891d5665b5"

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val ancho = constraints.maxWidth.toFloat()
            val alto = constraints.maxHeight.toFloat()

            // Posición inicial
            if (posX == -1f) {
                posX = ancho * 0.05f
                posY = alto * 0.05f
            }

            // --- DEFINICIÓN DE PAREDES DELGADAS ---
            // Definimos un grosor constante (1.5% del ancho de pantalla)
            val g = ancho * 0.015f

            val paredes = remember(ancho, alto) {
                listOf(
                    // Horizontales (Izquierda, Arriba, Derecha, Abajo)
                    Rect(ancho* 0.1f, alto * 0.15f, ancho * 0.6f, alto * 0.15f + g),
                    Rect(ancho * 0.3f, alto * 0.3f, ancho * 0.85f, alto * 0.3f + g),
                    Rect(0f, alto * 0.45f, ancho * 0.6f, alto * 0.45f + g),
                    Rect(ancho * 0.4f, alto * 0.6f, ancho, alto * 0.6f + g),
                    Rect(0f, alto * 0.75f, ancho * 0.8f, alto * 0.75f + g),
                    Rect(ancho * 0.2f, alto * 0.9f, ancho, alto * 0.9f + g),

                    // Verticales (Izquierda, Arriba, Derecha, Abajo)
                    Rect(ancho * 0.75f, 0f, ancho * 0.7f + g, alto * 0.15f),
                    Rect(ancho * 0.3f, alto * 0.15f, ancho * 0.3f + g, alto * 0.3f),
                    Rect(ancho * 0.6f, alto * 0.3f, ancho * 0.6f + g, alto * 0.5f),
                    Rect(ancho * 0.4f, alto * 0.65f, ancho * 0.4f + g, alto * 0.75f)
                )
            }

            val meta = remember(ancho, alto) {
                Rect(ancho * 0.85f, alto * 0.92f, ancho * 0.98f, alto * 0.98f)
            }

            // --- LÓGICA DE SENSORES ---
            val listener = remember {
                object : SensorEventListener {
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                    override fun onSensorChanged(event: SensorEvent) {
                        if (juegoGanado) return

                        val moverX = -(event.values[0] * velocidad)
                        val moverY = (event.values[1] * velocidad)

                        // Deslizamiento en X
                        val futuraX = (posX + moverX).coerceIn(radioPelota, ancho - radioPelota)
                        if (!paredes.any { it.overlaps(Rect(futuraX - radioPelota, posY - radioPelota, futuraX + radioPelota, posY + radioPelota)) }) {
                            posX = futuraX
                        }

                        // Deslizamiento en Y
                        val futuraY = (posY + moverY).coerceIn(radioPelota, alto - radioPelota)
                        if (!paredes.any { it.overlaps(Rect(posX - radioPelota, futuraY - radioPelota, posX + radioPelota, futuraY + radioPelota)) }) {
                            posY = futuraY
                        }

                        if (meta.overlaps(Rect(posX - radioPelota, posY - radioPelota, posX + radioPelota, posY + radioPelota))) {
                            juegoGanado = true
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                admonSensores.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_GAME)
                onDispose { admonSensores.unregisterListener(listener) }
            }

            // --- RENDERIZADO ---
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Meta
                drawRect(color = Color(0xFF00FF7F), topLeft = meta.topLeft, size = meta.size)

                // Paredes
                paredes.forEach { drawRect(color = Color(0xFF2C3E50), it.topLeft, it.size) }

                // Pelota
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFF5722), Color(0xFFBF360C)), // Naranja fuego
                        center = Offset(posX - 4f, posY - 4f),
                        radius = radioPelota
                    ),
                    radius = radioPelota,
                    center = Offset(posX, posY)
                )
            }

            // --- PANTALLA DE VICTORIA ---
            if (juegoGanado) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡MAESTRO DEL BALANCE!", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Text("UUID: $uuid", style = MaterialTheme.typography.headlineLarge, color = Color.White)

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = {
                            posX = ancho * 0.05f
                            posY = alto * 0.05f
                            juegoGanado = false
                        }) {
                            Text("Reiniciar")
                        }
                    }
                }
            }
        }
    }
}

/*
fun deviceSignature(context: Context): String {

    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    val data = androidId + Build.MODEL + Build.MANUFACTURER

    val md = MessageDigest.getInstance("SHA-256")
    val hash = md.digest(data.toByteArray())

    return hash.joinToString("") { "%02x".format(it) }
}*/