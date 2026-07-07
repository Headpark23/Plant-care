package com.example.api

import android.graphics.Bitmap
import android.util.Base64
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Our Custom Plant Analysis Structure ---

@JsonClass(generateAdapter = true)
data class PlantAnalysisResult(
    val speciesName: String,
    val wateringIntervalDays: Int,
    val fertilizingIntervalDays: Int,
    val pruningIntervalDays: Int,
    val sunlightExposureNeeded: String, // e.g. "Bright Indirect Light", "Full Sun"
    val careTips: String
)

// --- Retrofit API Service Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun identifyPlant(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Retrofit Client & Business Logic ---

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compress image to fit within safe API size limits, e.g., max 1024 width/height
        val scaledBitmap = scaleBitmapIfNeeded(bitmap)
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        val maxDim = 1024
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val (newWidth, newHeight) = if (ratio > 1) {
            Pair(maxDim, (maxDim / ratio).toInt())
        } else {
            Pair((maxDim * ratio).toInt(), maxDim)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun analyzePlantImage(bitmap: Bitmap, apiKey: String): PlantAnalysisResult? {
        val base64Data = bitmapToBase64(bitmap)
        val prompt = """
            Identify this house plant. Respond ONLY with a valid, clean JSON object matching the following fields. 
            Do NOT wrap the JSON in markdown code blocks or add any other text around it.
            
            Fields:
            - speciesName (String): The popular name, plus scientific name in parentheses. (e.g. "Snake Plant (Sansevieria trifasciata)")
            - wateringIntervalDays (Integer): Recommended days between waterings (typically 3 to 21).
            - fertilizingIntervalDays (Integer): Recommended days between fertilizing (typically 14 to 90).
            - pruningIntervalDays (Integer): Recommended days between maintenance pruning (typically 30 to 180).
            - sunlightExposureNeeded (String): E.g., "Full Sun", "Bright Indirect Light", "Partial Shade", or "Low Light".
            - careTips (String): 2-3 brief tips separated by semicolons (e.g., "Let soil dry completely;Wipe leaves regularly;Keep in warm drafts").
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Data))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.2f
            )
        )

        return try {
            val response = service.identifyPlant(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (jsonText != null) {
                // Parse JSON response using Moshi
                val adapter = moshi.adapter(PlantAnalysisResult::class.java)
                adapter.fromJson(jsonText.trim())
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
