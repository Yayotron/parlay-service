package me.victor.parlay.service

import me.victor.parlay.domain.BettingOption
import me.victor.parlay.infrastructure.api.*
import org.springframework.stereotype.Service

@Service
class AnalysisEngine {

    fun analyzeMatch(
        matchName: String,
        prediction: PredictionItem,
        homeStats: TeamStats,
        awayStats: TeamStats,
        odds: OddsItem?
    ): List<BettingOption> {
        val options = mutableListOf<BettingOption>()
        
        // 1. Match Result
        val statsWinProb = calculateWinProbability(homeStats, awayStats, true)
        val predWinProb = extractPredictionWinProb(prediction, true)
        val oddsWinProb = extractOddsProb(odds, "Match Winner", "Home")
        val finalHomeWinProb = aggregateProbabilities(listOf(statsWinProb, predWinProb, oddsWinProb))
        
        if (finalHomeWinProb > 50) options.add(BettingOption(matchName, "Home Win", finalHomeWinProb))
        
        // 2. BTTS
        val statsBttsProb = calculateBTTSProbability(homeStats, awayStats)
        val oddsBttsProb = extractOddsProb(odds, "Both Teams Score", "Yes")
        val finalBttsProb = aggregateProbabilities(listOf(statsBttsProb, oddsBttsProb))
        options.add(BettingOption(matchName, "Both Score", finalBttsProb))

        // 3. Over 2.5 Goals
        val statsOverProb = calculateOverUnderProbability(homeStats, awayStats, 2.5)
        val oddsOverProb = extractOddsProb(odds, "Goals Over/Under", "Over 2.5")
        val finalOverProb = aggregateProbabilities(listOf(statsOverProb, oddsOverProb))
        options.add(BettingOption(matchName, "Over 2.5 Goals", finalOverProb))

        return options
    }

    private fun aggregateProbabilities(probs: List<Int?>): Int {
        val validProbs = probs.filterNotNull()
        if (validProbs.isEmpty()) return 50
        return validProbs.average().toInt()
    }

    private fun extractPredictionWinProb(prediction: PredictionItem, isHome: Boolean): Int? {
        val advice = prediction.predictions.advice.lowercase()
        val winner = prediction.predictions.winner.name
        val homeName = prediction.comparison.form.home // Using comparison as a proxy for team names if needed
        
        return when {
            advice.contains("home") && isHome -> 70
            advice.contains("away") && !isHome -> 70
            advice.contains("draw") -> 33
            else -> null
        }
    }

    private fun extractOddsProb(odds: OddsItem?, betName: String, outcomeValue: String): Int? {
        val bookmaker = odds?.bookmakers?.firstOrNull() ?: return null
        val bet = bookmaker.bets.find { it.name.contains(betName, ignoreCase = true) } ?: return null
        val outcome = bet.values.find { it.value.contains(outcomeValue, ignoreCase = true) } ?: return null
        
        val decimalOdds = outcome.odd.toDoubleOrNull() ?: return null
        return (100 / decimalOdds).toInt()
    }

    private fun calculateWinProbability(team: TeamStats, opponent: TeamStats, isHome: Boolean): Int {
        var score = 50
        val form = team.form?.takeLast(5) ?: ""
        score += form.count { it == 'W' } * 5
        score -= form.count { it == 'L' } * 5
        
        if (isHome) {
            val homeWins = team.fixtures.wins.home ?: 0
            val homePlayed = team.fixtures.played.home ?: 1
            score += (homeWins * 100 / homePlayed.coerceAtLeast(1)) / 10
        } else {
            val awayWins = team.fixtures.wins.away ?: 0
            val awayPlayed = team.fixtures.played.away ?: 1
            score += (awayWins * 100 / awayPlayed.coerceAtLeast(1)) / 10
        }

        val avgScored = (if (isHome) team.goals.forGoals.average.home_avg else team.goals.forGoals.average.away_avg)?.toDoubleOrNull() ?: 1.0
        val avgConceded = (if (isHome) opponent.goals.against.average.away_avg else opponent.goals.against.average.home_avg)?.toDoubleOrNull() ?: 1.0
        score += ((avgScored - avgConceded) * 10).toInt()

        return score.coerceIn(10, 90)
    }

    private fun calculateBTTSProbability(home: TeamStats, away: TeamStats): Int {
        val homeScoringProb = 100 - (home.failed_to_score.home ?: 0) * 100 / (home.fixtures.played.home ?: 1).coerceAtLeast(1)
        val awayScoringProb = 100 - (away.failed_to_score.away ?: 0) * 100 / (away.fixtures.played.away ?: 1).coerceAtLeast(1)
        val homeConcedingProb = 100 - (home.clean_sheet.home ?: 0) * 100 / (home.fixtures.played.home ?: 1).coerceAtLeast(1)
        val awayConcedingProb = 100 - (away.clean_sheet.away ?: 0) * 100 / (away.fixtures.played.away ?: 1).coerceAtLeast(1)
        return ((homeScoringProb + awayConcedingProb) / 2 + (awayScoringProb + homeConcedingProb) / 2) / 2
    }

    private fun calculateOverUnderProbability(home: TeamStats, away: TeamStats, threshold: Double): Int {
        val homeAvg = home.goals.forGoals.average.total_avg?.toDoubleOrNull() ?: 1.5
        val awayAvg = away.goals.forGoals.average.total_avg?.toDoubleOrNull() ?: 1.5
        val combinedAvg = homeAvg + awayAvg
        return if (combinedAvg > threshold) (combinedAvg * 20).toInt().coerceIn(30, 85) else (100 - combinedAvg * 20).toInt().coerceIn(30, 85)
    }

    fun calculateProbability(options: List<BettingOption>): Int {
        if (options.isEmpty()) return 0
        return options.fold(1.0) { acc, option -> acc * (option.confidence / 100.0) }.let { (it * 100).toInt() }
    }

    fun calculateReturn(probability: Int): Int {
        return (10000 / probability.coerceAtLeast(1))
    }
}
