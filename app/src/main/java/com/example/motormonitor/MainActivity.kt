package com.example.motormonitor

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    // --- Variables de la Interfaz Gráfica (UI) ---
    private lateinit var textViewVoltage: TextView
    private lateinit var textViewCurrent: TextView
    private lateinit var textViewSpeed: TextView
    private lateinit var textViewDutyCycle: TextView // ¡NUEVO!
    private lateinit var chartVoltage: LineChart
    private lateinit var chartCurrent: LineChart
    private lateinit var chartSpeed: LineChart
    private lateinit var chartDutyCycle: LineChart // ¡NUEVO!
    private lateinit var buttonOn: Button
    private lateinit var buttonOff: Button
    private var time = 0f

    // --- Variables para la Conexión Bluetooth ---
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    // IMPORTANTE: Cambia "HC-05" por el nombre exacto de tu módulo Bluetooth
    private val DEVICE_NAME = "HC-05"

    // UUID estándar para el perfil de puerto serie (SPP)
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa todos los componentes de la interfaz
        textViewVoltage = findViewById(R.id.textViewVoltage)
        textViewCurrent = findViewById(R.id.textViewCurrent)
        textViewSpeed = findViewById(R.id.textViewSpeed)
        textViewDutyCycle = findViewById(R.id.textViewDutyCycle) // ¡NUEVO!
        chartVoltage = findViewById(R.id.chartVoltage)
        chartCurrent = findViewById(R.id.chartCurrent)
        chartSpeed = findViewById(R.id.chartSpeed)
        chartDutyCycle = findViewById(R.id.chartDutyCycle) // ¡NUEVO!
        buttonOn = findViewById(R.id.buttonOn)
        buttonOff = findViewById(R.id.buttonOff)

        // Configura el estilo y las propiedades de las gráficas
        setupChart(chartVoltage, "Voltaje", ContextCompat.getColor(this, R.color.teal_200))
        setupChart(chartCurrent, "Corriente", ContextCompat.getColor(this, R.color.light_blue_400))
        setupChart(chartSpeed, "Velocidad", ContextCompat.getColor(this, R.color.amber_400))
        setupChart(chartDutyCycle, "Ciclo de Trabajo", ContextCompat.getColor(this, R.color.pink_400)) // ¡NUEVO!

        // Asigna las acciones a los botones
        setupButtonListeners()

        // Inicia el proceso de configuración y conexión Bluetooth
        setupBluetooth()
    }

    // ¡MODIFICADO! - Procesa la nueva cadena de texto del Arduino
    private fun processData(data: String) {
        Log.d("BluetoothData", "Recibido: $data")
        // El formato esperado es "voltaje,corriente,rpm,dutycycle", por ejemplo "12.34,1.56,3000,75"
        try {
            val parts = data.split(',')
            // Verifica que hemos recibido exactamente 4 partes
            if (parts.size == 4) {
                val newVoltage = parts[0].toFloat()
                val newCurrent = parts[1].toFloat()
                val newSpeed = parts[2].toFloat()
                val newDutyCycle = parts[3].toFloat() // ¡NUEVO!

                // Actualiza la interfaz de usuario en el hilo principal
                runOnUiThread {
                    // Actualiza los TextViews
                    textViewVoltage.text = String.format("%.2f V", newVoltage)
                    textViewCurrent.text = String.format("%.2f A", newCurrent)
                    textViewSpeed.text = String.format("%.0f RPM", newSpeed)
                    textViewDutyCycle.text = String.format("%.0f %%", newDutyCycle) // ¡NUEVO!

                    // Añade los datos a las cuatro gráficas
                    addEntry(chartVoltage, newVoltage, "Voltaje", ContextCompat.getColor(this, R.color.teal_200))
                    addEntry(chartCurrent, newCurrent, "Corriente", ContextCompat.getColor(this, R.color.light_blue_400))
                    addEntry(chartSpeed, newSpeed, "Velocidad", ContextCompat.getColor(this, R.color.amber_400))
                    addEntry(chartDutyCycle, newDutyCycle, "Ciclo de Trabajo", ContextCompat.getColor(this, R.color.pink_400)) // ¡NUEVO!

                    time += 0.5f
                }
            } else {
                Log.w("DataParsingWarning", "Formato de datos incorrecto (se esperaban 4 partes): '$data'")
            }
        } catch (e: Exception) {
            Log.e("DataParsingError", "No se pudo procesar: '$data'", e)
        }
    }

    // Configura los comandos para los botones
    private fun setupButtonListeners() {
        buttonOn.setOnClickListener {
            sendData("ON\n")
            Toast.makeText(this, "Comando: ENCENDER", Toast.LENGTH_SHORT).show()
        }

        buttonOff.setOnClickListener {
            sendData("OFF\n")
            Toast.makeText(this, "Comando: APAGAR", Toast.LENGTH_SHORT).show()
        }
    }

    // --- EL RESTO DEL CÓDIGO PERMANECE IGUAL ---

    private fun setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) { Toast.makeText(this, "Bluetooth no disponible", Toast.LENGTH_LONG).show(); return }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 101); return
        }
        val targetDevice = bluetoothAdapter?.bondedDevices?.find { it.name == DEVICE_NAME }
        if (targetDevice != null) {
            Toast.makeText(this, "Dispositivo '$DEVICE_NAME' encontrado. Conectando...", Toast.LENGTH_SHORT).show()
            connectToDevice(targetDevice)
        } else {
            Toast.makeText(this, "'$DEVICE_NAME' no encontrado. Empareja primero.", Toast.LENGTH_LONG).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        thread {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) { return@thread }
                bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                runOnUiThread { Toast.makeText(this, "Conectado a ${device.name}", Toast.LENGTH_SHORT).show() }
                startListening()
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread { Toast.makeText(this, "Error al conectar: ${e.message}", Toast.LENGTH_LONG).show() }
                closeConnection()
            }
        }
    }

    private fun startListening() {
        thread {
            val buffer = ByteArray(1024)
            val stringBuilder = StringBuilder()
            while (bluetoothSocket?.isConnected == true) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes != -1) {
                        val readMessage = String(buffer, 0, bytes)
                        stringBuilder.append(readMessage)
                        var endOfLineIndex = stringBuilder.indexOf("\n")
                        while (endOfLineIndex > -1) {
                            val fullMessage = stringBuilder.substring(0, endOfLineIndex).trim()
                            stringBuilder.delete(0, endOfLineIndex + 1)
                            if (fullMessage.isNotEmpty()) { processData(fullMessage) }
                            endOfLineIndex = stringBuilder.indexOf("\n")
                        }
                    }
                } catch (e: IOException) { e.printStackTrace(); break }
            }
        }
    }

    private fun sendData(message: String) {
        if (bluetoothSocket?.isConnected == true) {
            try { outputStream?.write(message.toByteArray()) }
            catch (e: IOException) { e.printStackTrace(); Toast.makeText(this, "Error al enviar", Toast.LENGTH_SHORT).show() }
        } else { Toast.makeText(this, "No conectado", Toast.LENGTH_SHORT).show() }
    }

    private fun closeConnection() {
        try { inputStream?.close(); outputStream?.close(); bluetoothSocket?.close() }
        catch (e: IOException) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        closeConnection()
    }

    private fun setupChart(chart: LineChart, label: String, color: Int) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true); isDragEnabled = true; setScaleEnabled(true)
            setDrawGridBackground(false); setPinchZoom(true); setBackgroundColor(Color.TRANSPARENT)
            val data = LineData(); data.setValueTextColor(Color.WHITE); this.data = data
            legend.isEnabled = false
            xAxis.apply { textColor = Color.WHITE; setDrawGridLines(false); setAvoidFirstLastClipping(true); isEnabled = true; position = XAxis.XAxisPosition.BOTTOM }
            axisLeft.apply { textColor = Color.WHITE; setDrawGridLines(true); gridColor = Color.parseColor("#33FFFFFF") }
            axisRight.isEnabled = false
        }
    }

    private fun createSet(label: String, color: Int): LineDataSet {
        val set = LineDataSet(null, label)
        set.axisDependency = com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT
        set.color = color; set.setCircleColor(color); set.lineWidth = 2f; set.circleRadius = 4f
        set.fillAlpha = 65; set.fillColor = color; set.highLightColor = Color.rgb(244, 117, 117)
        set.valueTextColor = Color.WHITE; set.valueTextSize = 9f; set.setDrawValues(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        val drawable: Drawable? = ContextCompat.getDrawable(this, R.drawable.fade_chart_gradient)
        set.fillDrawable = drawable; set.setDrawFilled(true)
        return set
    }

    private fun addEntry(chart: LineChart, value: Float, label: String, color: Int) {
        val data = chart.data
        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if (set == null) { set = createSet(label, color); data.addDataSet(set) }
            data.addEntry(Entry(time, value), 0)
            data.notifyDataChanged(); chart.notifyDataSetChanged()
            chart.setVisibleXRangeMaximum(10f); chart.moveViewToX(data.entryCount.toFloat())
        }
    }
}

