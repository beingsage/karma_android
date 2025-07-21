//package com.technource.android.module.miscModule.miscscreen.Body.utils
//
//import java.util.regex.Pattern
//import javax.inject.Inject
//import javax.inject.Singleton
//
//@Singleton
//class VoiceCommandProcessor @Inject constructor() {
//
//    private val waterPatterns = listOf(
//        Pattern.compile("log (\\d+)\\s*(ml|milliliters?) of water", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("(\\d+)\\s*(ml|milliliters?) water", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("drink (\\d+)\\s*(ml|milliliters?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("water (\\d+)\\s*(ml|milliliters?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("log water", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("drink water", Pattern.CASE_INSENSITIVE)
//    )
//
//    private val weightPatterns = listOf(
//        Pattern.compile("i weigh (\\d+(?:\\.\\d+)?)\\s*(kg|kilograms?|lbs?|pounds?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("my weight is (\\d+(?:\\.\\d+)?)\\s*(kg|kilograms?|lbs?|pounds?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("log weight (\\d+(?:\\.\\d+)?)\\s*(kg|kilograms?|lbs?|pounds?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("weight (\\d+(?:\\.\\d+)?)\\s*(kg|kilograms?|lbs?|pounds?)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("log weight", Pattern.CASE_INSENSITIVE)
//    )
//
//    private val workoutPatterns = listOf(
//        Pattern.compile("start workout", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("begin workout", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("start exercise", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("begin exercise", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("workout time", Pattern.CASE_INSENSITIVE)
//    )
//
//    private val foodPatterns = listOf(
//        Pattern.compile("i ate (.*)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("i had (.*)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("log food (.*)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("log meal (.*)", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("ate (.*)", Pattern.CASE_INSENSITIVE)
//    )
//
//    private val photoPatterns = listOf(
//        Pattern.compile("take progress photo", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("progress photo", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("take photo", Pattern.CASE_INSENSITIVE),
//        Pattern.compile("capture photo", Pattern.CASE_INSENSITIVE)
//    )
//
//    fun processCommand(command: String): VoiceCommandResult {
//        val normalizedCommand = command.trim()
//
//        // Check water logging patterns
//        for (pattern in waterPatterns) {
//            val matcher = pattern.matcher(normalizedCommand)
//            if (matcher.find()) {
//                val amount = if (matcher.groupCount() >= 1) {
//                    try {
//                        matcher.group(1)?.toInt() ?: 250
//                    } catch (e: NumberFormatException) {
//                        250
//                    }
//                } else {
//                    250 // Default amount
//                }
//
//                return VoiceCommandResult(
//                    action = "LOG_WATER",
//                    parameters = mapOf("amount" to amount),
//                    confidence = 0.9f
//                )
//            }
//        }
//
//        // Check weight logging patterns
//        for (pattern in weightPatterns) {
//            val matcher = pattern.matcher(normalizedCommand)
//            if (matcher.find()) {
//                if (matcher.groupCount() >= 2) {
//                    try {
//                        val weightValue = matcher.group(1)?.toFloat() ?: 0f
//                        val unit = matcher.group(2)?.lowercase() ?: "kg"
//
//                        // Convert to kg if needed
//                        val weightInKg = if (unit.startsWith("lb") || unit.startsWith("pound")) {
//                            weightValue * 0.453592f
//                        } else {
//                            weightValue
//                        }
//
//                        return VoiceCommandResult(
//                            action = "LOG_WEIGHT",
//                            parameters = mapOf("weight" to weightInKg),
//                            confidence = 0.9f
//                        )
//                    } catch (e: NumberFormatException) {
//                        // Fall through to default weight logging
//                    }
//                }
//
//                return VoiceCommandResult(
//                    action = "LOG_WEIGHT",
//                    parameters = emptyMap(),
//                    confidence = 0.7f
//                )
//            }
//        }
//
//        // Check workout patterns
//        for (pattern in workoutPatterns) {
//            if (pattern.matcher(normalizedCommand).find()) {
//                return VoiceCommandResult(
//                    action = "START_WORKOUT",
//                    parameters = emptyMap(),
//                    confidence = 0.9f
//                )
//            }
//        }
//
//        // Check food logging patterns
//        for (pattern in foodPatterns) {
//            val matcher = pattern.matcher(normalizedCommand)
//            if (matcher.find()) {
//                val foodItem = if (matcher.groupCount() >= 1) {
//                    matcher.group(1)?.trim() ?: "food item"
//                } else {
//                    "food item"
//                }
//
//                return VoiceCommandResult(
//                    action = "LOG_FOOD",
//                    parameters = mapOf("food" to foodItem),
//                    confidence = 0.8f
//                )
//            }
//        }
//
//        // Check photo patterns
//        for (pattern in photoPatterns) {
//            if (pattern.matcher(normalizedCommand).find()) {
//                return VoiceCommandResult(
//                    action = "TAKE_PHOTO",
//                    parameters = emptyMap(),
//                    confidence = 0.9f
//                )
//            }
//        }
//
//        // No pattern matched
//        return VoiceCommandResult(
//            action = "UNKNOWN",
//            parameters = emptyMap(),
//            confidence = 0.0f
//        )
//    }
//
//    fun getSupportedCommands(): List<VoiceCommandExample> {
//        return listOf(
//            VoiceCommandExample(
//                command = "Log 500ml of water",
//                description = "Log water intake",
//                category = "Hydration"
//            ),
//            VoiceCommandExample(
//                command = "I weigh 70 kilograms",
//                description = "Log your current weight",
//                category = "Weight"
//            ),
//            VoiceCommandExample(
//                command = "Start workout",
//                description = "Begin a workout session",
//                category = "Exercise"
//            ),
//            VoiceCommandExample(
//                command = "I ate a banana",
//                description = "Log food intake",
//                category = "Nutrition"
//            ),
//            VoiceCommandExample(
//                command = "Take progress photo",
//                description = "Capture a progress photo",
//                category = "Progress"
//            )
//        )
//    }
//}
//
//data class VoiceCommandResult(
//    val action: String,
//    val parameters: Map<String, Any>,
//    val confidence: Float
//)
//
//data class VoiceCommandExample(
//    val command: String,
//    val description: String,
//    val category: String
//)
