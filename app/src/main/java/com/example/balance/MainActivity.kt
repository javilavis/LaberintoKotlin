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
import kotlin.math.sqrt


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
    val uuid = "d2aca40cee063efa414b3126cd04fb720c90f701eb1e87f3c5ab8f891d5665b5"
    val admonSensores = remember { contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val acelerometro = remember { admonSensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }

    // Estados de posición
    var posX by remember { mutableStateOf(-1f) }
    var posY by remember { mutableStateOf(-1f) }
    var juegoGanado by remember { mutableStateOf(false) }

    val radioPelota = 25f
    val velocidad = 2.5f

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val ancho = constraints.maxWidth.toFloat()
            val alto = constraints.maxHeight.toFloat()

            // 1. ORIGEN: Abajo a la izquierda (10% ancho, 90% alto)
            if (posX == -1f) {
                posX = ancho * 0.1f
                posY = alto * 0.96f
            }

            // 2. META: Círculo arriba en el centro (donde suele estar la cámara)
            val centroMetaX = ancho / 2
            val centroMetaY = alto * 0.026f
            val radioMeta = 45f

            // 3. PAREDES (Diseño en Zig-Zag para asegurar salida)
            val g = ancho * 0.02f // Grosor de paredes
            val paredes = remember(ancho, alto) {
                listOf(
                    // Horizontales de arriba pabajo0
                    //primera pegada
                    Rect(0f, alto * 0.1f, ancho * 0.4f, alto * 0.1f + g),
                    Rect(ancho * 0.55f, alto * 0.25f, ancho * 0.8f, alto * 0.25f + g),
                    Rect(ancho * 0.4f, alto * 0.45f, ancho * 0.8f, alto * 0.45f + g),
                    Rect(0f, alto * 0.6f, ancho * 0.28f, alto * 0.6f + g),

                    Rect(ancho * 0.4f, alto * 0.6f, ancho * 0.75f, alto * 0.6f + g),
                    Rect(ancho * 0.2f, alto * 0.8f, ancho * 0.91f, alto * 0.8f + g),
                    Rect(0f, alto * 0.9f, ancho * 0.45f, alto * 0.9f + g),
                    Rect(ancho * 0.65f, alto * 0.9f, ancho, alto * 0.9f + g),


                    // Verticales
                    Rect(ancho * 0.4f, alto * 0.1f, ancho * 0.4f + g, alto * 0.45f),
                    Rect(ancho * 0.55f, 0f, ancho * 0.55f + g, alto * 0.25f),
                    Rect(ancho * 0.78f, alto * 0.05f, ancho * 0.78f + g, alto * 0.25f),

                    Rect(ancho * 0.26f, alto * 0.2f, ancho * 0.26f + g, alto * 0.6f),
                    Rect(ancho * 0.4f, alto * 0.6f, ancho * 0.4f + g, alto * 0.8f),
                    Rect(ancho * 0.9f, alto * 0.55f, ancho * 0.9f + g, alto * 0.8f),
                    Rect(ancho * 0.55f, alto * 0.8f, ancho * 0.55f + g, alto)
                )
            }

            // 4. LÓGICA DE SENSORES Y COLISIÓN
            val listener = remember {
                object : SensorEventListener {
                    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
                    override fun onSensorChanged(event: SensorEvent) {
                        if (juegoGanado) return

                        val moverX = -(event.values[0] * velocidad)
                        val moverY = (event.values[1] * velocidad)

                        // Intento movimiento X
                        val futX = (posX + moverX).coerceIn(radioPelota, ancho - radioPelota)
                        if (!paredes.any { it.overlaps(Rect(futX - radioPelota, posY - radioPelota, futX + radioPelota, posY + radioPelota)) }) {
                            posX = futX
                        }

                        // Intento movimiento Y
                        val futY = (posY + moverY).coerceIn(radioPelota, alto - radioPelota)
                        if (!paredes.any { it.overlaps(Rect(posX - radioPelota, futY - radioPelota, posX + radioPelota, futY + radioPelota)) }) {
                            posY = futY
                        }

                        // VERIFICAR META (Distancia entre centros: Pitágoras)
                        val distanciaAMeta = sqrt(Math.pow((posX - centroMetaX).toDouble(), 2.0) +
                                Math.pow((posY - centroMetaY).toDouble(), 2.0))

                        if (distanciaAMeta < radioMeta) {
                            juegoGanado = true
                        }
                    }
                }
            }

            DisposableEffect(Unit) {
                admonSensores.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_GAME)
                onDispose { admonSensores.unregisterListener(listener) }
            }

            // 5. DIBUJO DEL JUEGO
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Dibujar Meta (Círculo tipo "Hoyo" de golf)
                drawCircle(
                    color = Color.Black,
                    radius = radioMeta,
                    center = Offset(centroMetaX, centroMetaY)
                )
                drawCircle(
                    color = Color(0xFF00FF7F), // Brillo verde
                    radius = radioMeta * 0.8f,
                    center = Offset(centroMetaX, centroMetaY)
                )

                // Dibujar Paredes
                paredes.forEach { drawRect(color = Color(0xFF34495E), it.topLeft, it.size) }

                // Dibujar Pelota
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
                        center = Offset(posX - 4f, posY - 4f),
                        radius = radioPelota
                    ),
                    radius = radioPelota,
                    center = Offset(posX, posY)
                )
            }

            // 6. INTERFAZ DE VICTORIA
            if (juegoGanado) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(green = 0.3f, alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("¡Victoria!", style = MaterialTheme.typography.headlineLarge, color = Color.White)
                        Spacer(modifier = Modifier.height(20.dp))

                        Button(onClick = {
                            posX = ancho * 0.1f
                            posY = alto * 0.96f
                            juegoGanado = false
                        }) {
                            Text("Reiniciar")
                        }
                        Spacer(modifier = Modifier.height(500.dp))
                        Text("UUID: \n$uuid", style = MaterialTheme.typography.headlineSmall, color = Color.White)
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