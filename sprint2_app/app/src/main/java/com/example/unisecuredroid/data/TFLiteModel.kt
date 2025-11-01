package com.example.unisecuredroid.data

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteModel(private val context: Context) {
    private var interpreter: Interpreter? = null

    // 1. Carga el modelo desde la carpeta assets
    fun loadModel(): Boolean {
        try {
            val modelBuffer = loadModelFile(context, "model.tflite")
            val options = Interpreter.Options()
            options.setNumThreads(4) // Optimización para dispositivos multi-core
            interpreter = Interpreter(modelBuffer, options)
            return true
        } catch (e: Exception) {
            println("Error al cargar el modelo TFLite: ${e.message}")
            return false
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    // 2. Ejecuta la inferencia
    fun runInference(inputVector: FloatArray): Float? {
        if (interpreter == null) return null

        // Crear el input tensor (Tamaño: 1x35, Tipo: Float32/Float16)
        val inputBuffer = ByteBuffer.allocateDirect(StaticAnalyzer.FEATURE_SIZE * 4) // 4 bytes por float
        inputBuffer.order(ByteOrder.nativeOrder())

        // Cargar el vector de características en el buffer
        inputBuffer.asFloatBuffer().put(inputVector)

        // Crear el output tensor (Tamaño: 1x1, Tipo: Float32)
        val outputBuffer = Array(1) { FloatArray(1) } // Devuelve la probabilidad (0.0 a 1.0)

        try {
            interpreter?.run(inputBuffer, outputBuffer)
            // Devolver la probabilidad única
            return outputBuffer[0][0]
        } catch (e: Exception) {
            println("Error en la inferencia TFLite: ${e.message}")
            return null
        }
    }

    // 3. Cierra el intérprete
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}