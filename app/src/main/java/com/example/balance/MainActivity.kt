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
    // 'remember' hace que la variable no se reinicie cada vez que la pantalla se dibuja de nuevoo
    val admonSensores = remember {
        // Obtenemos el servicio del sistema que controla el hardware (sensores)
        contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val acelerometro = remember {
        // De todos los sensores, pedimos específicamente el de movimiento (acelerómetro)
        admonSensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    // 'mutableStateOf' crea variables que, al cambiar, "avisan" a la pantalla para que se redibuje
    var posX by remember { mutableStateOf(200f) } // Posición horizontal inicial
    var posY by remember { mutableStateOf(200f) } // Posición vertical inicial

    // Valores fijos para nuestro juego
    val radioPelota = 45f
    val velocidad = 1.5f

    // Creamos un rectángulo virtual: (izquierda, arriba, derecha, abajo)
    // Esto sirve para definir dónde está la pared en el espacio
    val paredPrueba = remember {
        androidx.compose.ui.geometry.Rect(400f, 600f, 800f, 650f)
    }

    /*La parte de los sensores*/
    val listener = remember {
        object : SensorEventListener {
            // Este es obligatorio pero casi nunca se usa (cambios en precisión del sensor)
            override fun onAccuracyChanged(s: Sensor?, a: Int) {}
            // Este se ejecuta MILES de veces por segundo cuando mueves el celular
            override fun onSensorChanged(event: SensorEvent) {
                // event.values[0] es la inclinación en X, values[1] en Y
                // Calculamos a dónde QUERRÍA ir la pelota (nueva posición tentativa)
                val nuevaX = posX - (event.values[0] * velocidad)
                val nuevaY = posY + (event.values[1] * velocidad)

                // Creamos un cuadrado imaginario alrededor de la "futura" pelota para ver si choca
                val areaPelotaFutura = androidx.compose.ui.geometry.Rect(
                    nuevaX - radioPelota, nuevaY - radioPelota,
                    nuevaX + radioPelota, nuevaY + radioPelota
                )

                // 'overlaps' devuelve true si dos rectángulos se tocan
                // "Si NO hay choque con la pared, entonces actualiza la posición real"
                if (!paredPrueba.overlaps(areaPelotaFutura)) {
                    posX = nuevaX
                    posY = nuevaY
                }
            }
        }
    }

// Este bloque gestiona cuándo conectar el sensor
    DisposableEffect(Unit) {
        // Al empezar: "Oye Android, empieza a mandarme datos del acelerómetro"
        admonSensores.registerListener(listener, acelerometro, SensorManager.SENSOR_DELAY_GAME)

        // Al cerrar la app o cambiar de pantalla: "Oye, deja de gastar batería, apaga el sensor"
        onDispose { admonSensores.unregisterListener(listener) }
    }

    // BoxWithConstraints nos permite saber qué tan grande es la pantalla del celular actual
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val anchoPantalla = constraints.maxWidth.toFloat()
        val altoPantalla = constraints.maxHeight.toFloat()

        // .coerceIn asegura que el valor no se pase de los límites (mínimo, máximo)
        // Evita que la pelota se salga de los bordes del celular
        posX = posX.coerceIn(radioPelota, anchoPantalla - radioPelota)
        posY = posY.coerceIn(radioPelota, altoPantalla - radioPelota)

        // Aquí es donde sucede la "magia" visual
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Dibujamos el rectángulo de la pared usando las coordenadas de 'paredPrueba'
            drawRect(color = Color.DarkGray, topLeft = paredPrueba.topLeft, size = paredPrueba.size)

            // Dibujamos la pelota
            drawCircle(
                // Creamos un degradado para que no se vea plana (efecto 3D)
                brush = androidx.compose.ui.graphics.Brush.radialGradient(
                    colors = listOf(Color.White, Color.Magenta, Color(0xFF4B0082)),
                    // Movemos el centro de la luz un poquito arriba a la izquierda
                    center = Offset(posX - 10f, posY - 10f),
                    radius = radioPelota
                ),
                radius = radioPelota,
                center = Offset(posX, posY) // La posición central es la que nos da el sensor
            )
        }
    }

}