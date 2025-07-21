//package com.technource.android.module.miscModule.miscscreen.Body.utils
//
//import android.content.Context
//import android.graphics.Bitmap
//import android.util.Log
//import com.google.mlkit.vision.common.InputImage
//import com.google.mlkit.vision.label.ImageLabeling
//import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class MLNutritionAnalyzer @Inject constructor(
//    private val context: Context
//) {
//
//    private val imageLabeler = ImageLabeling.getClient(
//        ImageLabelerOptions.Builder()
//            .setConfidenceThreshold(0.7f)
//            .build()
//    )
//
//    private val nutritionDatabase = NutritionDatabase()
//
////    suspend fun analyzeFoodImage(bitmap: Bitmap): FoodAnalysisResult {
////        return try {
//////            val inputImage = InputImage.fromBitmap(bitmap, 0)
//////            val labels = imageLabeler.process(inputImage).await()
////
//////            val detectedFoods = labels.mapNotNull { label: com.google.mlkit.vision.label.ImageLabel ->
//////                val foodItem = nutritionDatabase.findFoodByLabel(label.text)
//////                if (foodItem != null) {
//////                    DetectedFood(
//////                        name = foodItem.name,
//////                        confidence = label.confidence,
//////                        nutrition = foodItem.nutrition
//////                    )
//////                } else null
//////            }.sortedByDescending { it.confidence }
////
//////            if (detectedFoods.isNotEmpty()) {
//////                val primaryFood = detectedFoods.first()
//////                FoodAnalysisResult.Success(
//////                    detectedFoods = detectedFoods,
//////                    estimatedNutrition = estimateNutrition(detectedFoods),
//////                    suggestions = generateSuggestions(primaryFood)
//////                )
//////            } else {
//////                FoodAnalysisResult.NoFoodDetected
//////            }
////
////        } catch (e: Exception) {
////            Log.e("MLNutritionAnalyzer", "Error analyzing food image", e)
////            FoodAnalysisResult.Error(e.message ?: "Unknown error occurred")
////        }
////    }
//
//    private fun estimateNutrition(detectedFoods: List<DetectedFood>): NutritionEstimate {
//        // Use the most confident detection for nutrition estimation
//        val primaryFood = detectedFoods.firstOrNull()
//
//        return if (primaryFood != null) {
//            // Apply confidence-based scaling
//            val confidenceMultiplier = primaryFood.confidence
//            NutritionEstimate(
//                calories = (primaryFood.nutrition.calories * confidenceMultiplier).toInt(),
//                protein = (primaryFood.nutrition.protein * confidenceMultiplier),
//                carbs = (primaryFood.nutrition.carbs * confidenceMultiplier),
//                fat = (primaryFood.nutrition.fat * confidenceMultiplier),
//                fiber = (primaryFood.nutrition.fiber * confidenceMultiplier),
//                sugar = (primaryFood.nutrition.sugar * confidenceMultiplier),
//                sodium = (primaryFood.nutrition.sodium * confidenceMultiplier),
//                confidence = primaryFood.confidence
//            )
//        } else {
//            NutritionEstimate.empty()
//        }
//    }
//
//    private fun generateSuggestions(primaryFood: DetectedFood): List<String> {
//        val suggestions = mutableListOf<String>()
//
//        // Add portion size suggestions
//        suggestions.add("Consider weighing your ${primaryFood.name} for more accurate tracking")
//
//        // Add nutritional insights
//        when {
//            primaryFood.nutrition.protein > 20 -> {
//                suggestions.add("Great protein source! Perfect for muscle building.")
//            }
//            primaryFood.nutrition.fiber > 5 -> {
//                suggestions.add("High in fiber - excellent for digestive health!")
//            }
//            primaryFood.nutrition.sugar > 15 -> {
//                suggestions.add("High in sugar - consider portion control")
//            }
//            primaryFood.nutrition.sodium > 500 -> {
//                suggestions.add("High sodium content - balance with low-sodium foods")
//            }
//        }
//
//        // Add pairing suggestions
//        suggestions.add("Pair with vegetables for a balanced meal")
//
//        return suggestions
//    }
//
//    fun analyzeBarcode(barcode: String): BarcodeAnalysisResult {
//        return try {
//            val foodItem = nutritionDatabase.findFoodByBarcode(barcode)
//            if (foodItem != null) {
//                BarcodeAnalysisResult.Success(foodItem)
//            } else {
//                BarcodeAnalysisResult.NotFound(barcode)
//            }
//        } catch (e: Exception) {
//            BarcodeAnalysisResult.Error(e.message ?: "Unknown error occurred")
//        }
//    }
//}
//
//sealed class FoodAnalysisResult {
//    data class Success(
//        val detectedFoods: List<DetectedFood>,
//        val estimatedNutrition: NutritionEstimate,
//        val suggestions: List<String>
//    ) : FoodAnalysisResult()
//
//    object NoFoodDetected : FoodAnalysisResult()
//    data class Error(val message: String) : FoodAnalysisResult()
//}
//
//sealed class BarcodeAnalysisResult {
//    data class Success(val foodItem: FoodItem) : BarcodeAnalysisResult()
//    data class NotFound(val barcode: String) : BarcodeAnalysisResult()
//    data class Error(val message: String) : BarcodeAnalysisResult()
//}
//
//data class DetectedFood(
//    val name: String,
//    val confidence: Float,
//    val nutrition: NutritionInfo
//)
//
//data class NutritionEstimate(
//    val calories: Int,
//    val protein: Float,
//    val carbs: Float,
//    val fat: Float,
//    val fiber: Float,
//    val sugar: Float,
//    val sodium: Float,
//    val confidence: Float
//) {
//    companion object {
//        fun empty() = NutritionEstimate(0, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
//    }
//}
//
//data class FoodItem(
//    val id: String,
//    val name: String,
//    val barcode: String? = null,
//    val nutrition: NutritionInfo,
//    val category: String,
//    val brand: String? = null
//)
//
//data class NutritionInfo(
//    val calories: Int,
//    val protein: Float,
//    val carbs: Float,
//    val fat: Float,
//    val fiber: Float = 0f,
//    val sugar: Float = 0f,
//    val sodium: Float = 0f,
//    val servingSize: String = "100g"
//)
//
//class NutritionDatabase {
//    private val foodItems = listOf(
//        FoodItem(
//            id = "apple_001",
//            name = "Apple",
//            nutrition = NutritionInfo(52, 0.3f, 14f, 0.2f, 2.4f, 10f, 1f),
//            category = "Fruits"
//        ),
//        FoodItem(
//            id = "banana_001",
//            name = "Banana",
//            nutrition = NutritionInfo(89, 1.1f, 23f, 0.3f, 2.6f, 12f, 1f),
//            category = "Fruits"
//        ),
//        FoodItem(
//            id = "chicken_breast_001",
//            name = "Chicken Breast",
//            nutrition = NutritionInfo(165, 31f, 0f, 3.6f, 0f, 0f, 74f),
//            category = "Protein"
//        ),
//        FoodItem(
//            id = "rice_001",
//            name = "White Rice",
//            nutrition = NutritionInfo(130, 2.7f, 28f, 0.3f, 0.4f, 0.1f, 1f),
//            category = "Grains"
//        ),
//        FoodItem(
//            id = "broccoli_001",
//            name = "Broccoli",
//            nutrition = NutritionInfo(34, 2.8f, 7f, 0.4f, 2.6f, 1.5f, 33f),
//            category = "Vegetables"
//        ),
//        FoodItem(
//            id = "salmon_001",
//            name = "Salmon",
//            nutrition = NutritionInfo(208, 20f, 0f, 12f, 0f, 0f, 59f),
//            category = "Protein"
//        ),
//        FoodItem(
//            id = "bread_001",
//            name = "Whole Wheat Bread",
//            nutrition = NutritionInfo(247, 13f, 41f, 4.2f, 7f, 6f, 400f),
//            category = "Grains"
//        ),
//        FoodItem(
//            id = "egg_001",
//            name = "Egg",
//            nutrition = NutritionInfo(155, 13f, 1.1f, 11f, 0f, 1.1f, 124f),
//            category = "Protein"
//        )
//    )
//
//    private val labelMappings = mapOf(
//        "apple" to "apple_001",
//        "fruit" to "apple_001",
//        "banana" to "banana_001",
//        "chicken" to "chicken_breast_001",
//        "meat" to "chicken_breast_001",
//        "rice" to "rice_001",
//        "grain" to "rice_001",
//        "broccoli" to "broccoli_001",
//        "vegetable" to "broccoli_001",
//        "salmon" to "salmon_001",
//        "fish" to "salmon_001",
//        "bread" to "bread_001",
//        "egg" to "egg_001"
//    )
//
//    fun findFoodByLabel(label: String): FoodItem? {
//        val normalizedLabel = label.lowercase()
//        val foodId = labelMappings[normalizedLabel]
//        return foodId?.let { id -> foodItems.find { it.id == id } }
//    }
//
//    fun findFoodByBarcode(barcode: String): FoodItem? {
//        return foodItems.find { it.barcode == barcode }
//    }
//
//    fun searchFoods(query: String): List<FoodItem> {
//        val normalizedQuery = query.lowercase()
//        return foodItems.filter {
//            it.name.lowercase().contains(normalizedQuery) ||
//            it.category.lowercase().contains(normalizedQuery)
//        }
//    }
//}
