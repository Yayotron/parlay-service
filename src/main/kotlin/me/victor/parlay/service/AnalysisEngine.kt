package me.victor.parlay.service

import me.victor.parlay.domain.BettingOption
import me.victor.parlay.infrastructure.api.PredictionItem
import me.victor.parlay.infrastructure.api.TeamStats
import org.springframework.stereotype.Service

@Service
class AnalysisEngine {

    fun analyzeMatch(
        matchName: String,
        prediction: PredictionItem,
        homeStats: TeamStats,
        awayStats: TeamStats
    ): List<BettingOption> {
        val options = mutableListOf<BettingOption>()
        
        // 1. Match Result (Home Win, Draw, Away Win)
        val homeWinProb = calculateWinProbability(homeStats, awayStats, true)
        val awayWinProb = calculateWinProbability(awayStats, homeStats, false)
        
        if (homeWinProb > 60) options.add(BettingOption(matchName, "Home Win", homeWinProb))
        else if (awayWinProb > 60) options.add(BettingOption(matchName, "Away Win", awayWinProb))
        else options.add(BettingOption(matchName, "Draw", 100 - homeWinProb - awayWinProb))

        // 2. Both Teams to Score (BTTS)
        val bttsProb = calculateBTTSProbability(homeStats, awayStats)
        options.add(BettingOption(matchName, "Both Score", bttsProb))

        // 3. Over/Under 2.5 Goals
        val over25Prob = calculateOverUnderProbability(homeStats, awayStats, 2.5)
        options.add(BettingOption(matchName, "Over 2.5 Goals", over25Prob))

        return options
    }

    private fun calculateWinProbability(team: TeamStats, opponent: TeamStats, isHome: Boolean): Int {
        var score = 50
        
        // Form weight (last 5 matches)
        val form = team.form?.takeLast(5) ?: ""
        score += form.count { it == 'W' } * 5
        score -= form.count { it == 'L' } * 5
        
        // Home/Away advantage
        if (isHome) {
            val homeWins = team.fixtures.wins.home ?: 0
            val homePlayed = team.fixtures.played.home ?: 1
            score += (homeWins * 100 / homePlayed.coerceAtLeast(1)) / 10
        } else {
            val awayWins = team.fixtures.wins.away ?: 0
            val awayPlayed = team.fixtures.played.away ?: 1
            score += (awayWins * 100 / awayPlayed.coerceAtLeast(1)) / 10
        }

        // Goals comparison
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
        
        return if (combinedAvg > threshold) {
            (combinedAvg * 20).toInt().coerceIn(30, 85)
        } else {
            (100 - combinedAvg * 20).toInt().coerceIn(30, 85)
        }
    }

    fun calculateProbability(options: List<BettingOption>): Int {
        if (options.isEmpty()) return 0
        // Combined probability for a parlay (product of individual probabilities)
        return options.fold(1.0) { acc, option -> acc * (option.confidence / 100.0) }.let { (it * 100).toInt() }
    }

    fun calculateReturn(probability: Int): Int {
        // Return is inversely proportional to probability
        // e.g., 50% prob -> 200% return, 25% prob -> 400% return
        return (10000 / probability.coerceAtLeast(1))
    }
}
