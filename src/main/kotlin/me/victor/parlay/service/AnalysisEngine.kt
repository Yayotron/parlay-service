package me.victor.parlay.service

import me.victor.parlay.domain.BettingOption
import me.victor.parlay.infrastructure.api.PredictionItem
import org.springframework.stereotype.Service

@Service
class AnalysisEngine {

    fun analyzeMatch(matchName: String, prediction: PredictionItem): List<BettingOption> {
        val options = mutableListOf<BettingOption>()
        val p = prediction.predictions
        val comp = prediction.comparison

        // 1. Winner Prediction
        if (p.winner.name != null) {
            val confidence = comp.form.home.replace("%", "").toIntOrNull() ?: 50
            options.add(BettingOption(matchName, "${p.winner.name} Wins", confidence))
        }

        // 2. Both Teams to Score (Simplified logic based on advice or goals)
        if (p.advice.contains("Both teams to score", ignoreCase = true)) {
            options.add(BettingOption(matchName, "Both Score", 70))
        }

        // 3. Over/Under
        if (p.under_over != null) {
            options.add(BettingOption(matchName, "Goals ${p.under_over}", 65))
        }

        return options
    }

    fun calculateProbability(options: List<BettingOption>): Int {
        if (options.isEmpty()) return 0
        // Simple average of confidence scores for the parlay
        return options.map { it.confidence }.average().toInt()
    }

    fun calculateReturn(probability: Int, isHighRisk: Boolean): Int {
        // Mock return calculation: lower probability -> higher return
        return if (isHighRisk) (10000 / (probability.coerceAtLeast(1))) * 2 else (5000 / (probability.coerceAtLeast(1)))
    }
}
