package com.example.stressmonitor.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import com.example.stressmonitor.presentation.theme.StressMonitorTheme
import com.google.firebase.database.FirebaseDatabase
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class StressRecord(
    val id: Long = System.currentTimeMillis(),
    val time: String,
    val avgBpm: Int,
    val label: String,
    val color: Color,
    val rawProgress: List<Float>
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private var lastUploadTime = 0L
    private lateinit var sensorManager: SensorManager
    private var hrSensor: Sensor? = null
    private var model: Module? = null

    // Make sure this says dbRef and uses FirebaseDatabase correctly
    private lateinit var dbRef: com.google.firebase.database.DatabaseReference

    private var bpmState = mutableStateOf(0f)
    private var stressLabel = mutableStateOf("Ready")
    private var bgColor = mutableStateOf(Color.Black)
    private var isFinished = mutableStateOf(false)
    private val heartRateWindow = mutableStateListOf<Float>()
    private var modelStatus = mutableStateOf("Initializing...")
    private var isModelLoaded = mutableStateOf(false)

    private val historyList = mutableStateListOf<StressRecord>()
    private var selectedRecordForDetail = mutableStateOf<StressRecord?>(null)
    private var showHistory = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        dbRef = com.google.firebase.database.FirebaseDatabase.getInstance().reference.child("readings")
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        hrSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        loadModel()

        setContent {
            StressMonitorTheme {
                when {
                    selectedRecordForDetail.value != null -> {
                        ProgressDetailScreen(record = selectedRecordForDetail.value!!, onBack = { selectedRecordForDetail.value = null })
                    }
                    showHistory.value -> {
                        HistoryScreen(history = historyList, onBack = { showHistory.value = false }, onSelectRecord = { selectedRecordForDetail.value = it })
                    }
                    else -> {
                        WearApp(
                            bpm = bpmState.value, stress = stressLabel.value, bgColor = bgColor.value,
                            isFinished = isFinished.value, count = heartRateWindow.size,
                            modelMsg = modelStatus.value, isLoaded = isModelLoaded.value,
                            onReset = { resetMeasurement() }, onViewHistory = { showHistory.value = true },
                            onViewDetails = { if (historyList.isNotEmpty()) selectedRecordForDetail.value = historyList.first() }
                        )
                    }
                }
            }
        }
    }

    private fun loadModel() {
        try {
            val modelPath = assetFilePath(this, "stress_model.pt")
            model = LiteModuleLoader.load(modelPath)
            modelStatus.value = "Model Initialized Successfully"
            isModelLoaded.value = true
        } catch (e: Exception) {
            modelStatus.value = "Error: ${e.localizedMessage}"
            isModelLoaded.value = false
        }
    }

    private fun resetMeasurement() {
        heartRateWindow.clear()
        isFinished.value = false
        stressLabel.value = "Waiting for pulse..."
        bgColor.value = Color.Black
        bpmState.value = 0f
    }

    private fun assetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        context.assets.open(assetName).use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        return file.absolutePath
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val bpm = event.values[0]
            if (bpm > 40 && bpm < 180 && !isFinished.value) {
                runOnUiThread {
                    bpmState.value = bpm
                    heartRateWindow.add(bpm)
                    if (heartRateWindow.size == 30) {
                        isFinished.value = true
                        performInference()
                    } else {
                        stressLabel.value = "Measuring... ${heartRateWindow.size}/30"
                    }
                }
            }
        }
    }

    private fun performInference() {
        if (!isModelLoaded.value) return
        try {
            val rawData = heartRateWindow.toList()
            val rrIntervals = rawData.map { 60000f / it }
            val avgHR = rawData.average().toFloat()
            val meanRR = rrIntervals.average().toFloat()
            val sdrr = Math.sqrt(rrIntervals.map { Math.pow((it - meanRR).toDouble(), 2.0) }.average()).toFloat()

            var sumDiffSq = 0.0
            var count50 = 0
            for (i in 0 until rrIntervals.size - 1) {
                val diff = Math.abs(rrIntervals[i + 1] - rrIntervals[i])
                sumDiffSq += Math.pow(diff.toDouble(), 2.0)
                if (diff > 50.0) count50++
            }
            val rmssd = Math.sqrt(sumDiffSq / (rrIntervals.size - 1)).toFloat()
            val pnn50 = (count50.toFloat() / (rrIntervals.size - 1)) * 100f

            val inputData = FloatArray(30 * 5)
            for (i in 0 until 30) {
                val b = i * 5
                inputData[b + 0] = (avgHR - 73.94182f) / 10.33745f
                inputData[b + 1] = (meanRR - 846.65010f) / 124.60398f
                inputData[b + 2] = (sdrr - 109.35253f) / 77.11703f
                inputData[b + 3] = (rmssd - 14.97750f) / 4.12077f
                inputData[b + 4] = (pnn50 - 0.86600f) / 0.99019f
            }

            val out = model!!.forward(IValue.from(Tensor.fromBlob(inputData, longArrayOf(1, 30, 5)))).toTensor().dataAsFloatArray
            val maxIdx = out.indices.maxByOrNull { out[it] } ?: 0

            val (label, color) = when (maxIdx) {
                0 -> "Relaxed ✅" to Color(0xFF1B5E20)
                1 -> "Interrupted ⚠️" to Color(0xFFFBC02D)
                else -> "Stressed 🔥" to Color(0xFFB71C1C)
            }

            stressLabel.value = label
            bgColor.value = color

            val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val record = StressRecord(time = time, avgBpm = avgHR.toInt(), label = label, color = color, rawProgress = rawData)

            historyList.add(0, record)
            if (historyList.size > 30) historyList.removeAt(historyList.lastIndex)

            // --- PUSH TO FIREBASE ---
            val firebaseData = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "label" to label,
                "bpm" to avgHR.toInt(),
                "raw_data" to rawData
            )
            dbRef.push().setValue(firebaseData)

        } catch (e: Exception) { stressLabel.value = "Inference Error" }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED) {
            sensorManager.registerListener(this, hrSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }
    override fun onPause() { super.onPause(); sensorManager.unregisterListener(this) }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}

@Composable
fun WearApp(bpm: Float, stress: String, bgColor: Color, isFinished: Boolean, count: Int, modelMsg: String, isLoaded: Boolean, onReset: () -> Unit, onViewHistory: () -> Unit, onViewDetails: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(bgColor), contentAlignment = Alignment.Center) {
        TimeText()
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
            if (!isFinished) {
                Text(text = "BPM: ${bpm.toInt()}", style = MaterialTheme.typography.display1)
                Text(text = "Progress: $count/30", style = MaterialTheme.typography.caption2)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = modelMsg, style = MaterialTheme.typography.caption3, color = if (isLoaded) Color.Green else Color.Yellow, fontSize = 8.sp)
            } else {
                Text(text = stress, style = MaterialTheme.typography.title3, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Button(onClick = onReset, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize), colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black)) { Text("Reset", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onViewDetails, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize), colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black)) { Text("Data", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(onClick = onViewHistory, modifier = Modifier.size(ButtonDefaults.ExtraSmallButtonSize), colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black)) { Text("Hist", fontSize = 8.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun ProgressDetailScreen(record: StressRecord, onBack: () -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        item { Text("Details: ${record.time}", style = MaterialTheme.typography.caption1, color = record.color) }
        items(record.rawProgress.asReversed().withIndex().toList()) { (index, bpm) ->
            Text(text = "P${30 - index}: ${bpm.toInt()} BPM", style = MaterialTheme.typography.caption2)
        }
        item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black)) { Text("Back", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
fun HistoryScreen(history: List<StressRecord>, onBack: () -> Unit, onSelectRecord: (StressRecord) -> Unit) {
    ScalingLazyColumn(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        item { Text("History", style = MaterialTheme.typography.caption1) }
        items(history) { record ->
            Card(onClick = { onSelectRecord(record) }, backgroundPainter = CardDefaults.cardBackgroundPainter(startBackgroundColor = record.color.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(record.label, style = MaterialTheme.typography.caption2, fontWeight = FontWeight.Bold)
                    Text(record.time, style = MaterialTheme.typography.caption2)
                }
            }
        }
        item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black)) { Text("Back", fontWeight = FontWeight.Bold) } }
    }
}