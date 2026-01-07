package me.victor.parlay.service

import me.victor.parlay.infrastructure.api.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnalysisEngineTest {

    private val analysisEngine = AnalysisEngine()

    @Test
    fun `should calculate high probability for strong home team`() {
        val homeStats = createMockTeamStats(form = "WWWWW", homeWins = 5, homePlayed = 5, homeGoalsAvg = "3.0")
        val awayStats = createMockTeamStats(form = "LLLLL", awayWins = 0, awayPlayed = 5, awayGoalsAgainstAvg = "3.0")
        val prediction = createMockPrediction(advice = "Home Win")
        
        val options = analysisEngine.analyzeMatch("Home vs Away", prediction, homeStats, awayStats, null)
        
        val homeWinOption = options.find { it.bet == "Home Win" }
        assertThat(homeWinOption).isNotNull
        assertThat(homeWinOption!!.confidence).isGreaterThan(70)
    }

    @Test
    fun `should calculate high BTTS probability for high scoring teams`() {
        val homeStats = createMockTeamStats(failedToScoreHome = 0, cleanSheetHome = 0, homePlayed = 5)
        val awayStats = createMockTeamStats(failedToScoreAway = 0, cleanSheetAway = 0, awayPlayed = 5)
        val prediction = createMockPrediction()
        
        val options = analysisEngine.analyzeMatch("Home vs Away", prediction, homeStats, awayStats, null)
        
        val bttsOption = options.find { it.bet == "Both Score" }
        assertThat(bttsOption).isNotNull
        assertThat(bttsOption!!.confidence).isGreaterThan(80)
    }

    @Test
    fun `should aggregate probabilities from multiple sources`() {
        val homeStats = createMockTeamStats(form = "WWWWW") // High stats prob
        val prediction = createMockPrediction(advice = "Home Win") // High pred prob
        val odds = createMockOdds(betName = "Match Winner", outcomeValue = "Home", oddValue = "1.5") // ~66% odds prob
        
        val options = analysisEngine.analyzeMatch("Home vs Away", prediction, homeStats, createMockTeamStats(), odds)
        
        val homeWinOption = options.find { it.bet == "Home Win" }
        assertThat(homeWinOption).isNotNull
        // Average of stats (~80), pred (70), and odds (66) should be around 72
        assertThat(homeWinOption!!.confidence).isBetween(65, 80)
    }

    private fun createMockTeamStats(
        form: String = "WDLWD",
        homeWins: Int = 2,
        homePlayed: Int = 5,
        awayWins: Int = 2,
        awayPlayed: Int = 5,
        homeGoalsAvg: String = "1.5",
        awayGoalsAvg: String = "1.5",
        homeGoalsAgainstAvg: String = "1.5",
        awayGoalsAgainstAvg: String = "1.5",
        failedToScoreHome: Int = 1,
        failedToScoreAway: Int = 1,
        cleanSheetHome: Int = 1,
        cleanSheetAway: Int = 1
    ): TeamStats {
        return TeamStats(
            form = form,
            fixtures = TeamFixtures(
                played = StatValue(homePlayed, awayPlayed, homePlayed + awayPlayed),
                wins = StatValue(homeWins, awayWins, homeWins + awayWins),
                draws = StatValue(1, 1, 2),
                loses = StatValue(1, 1, 2)
            ),
            goals = TeamGoalsStats(
                forGoals = GoalDetails(
                    total = StatValue(10, 10, 20),
                    average = StatValue(null, null, null, homeGoalsAvg, awayGoalsAvg, "1.5")
                ),
                against = GoalDetails(
                    total = StatValue(10, 10, 20),
                    average = StatValue(null, null, null, homeGoalsAgainstAvg, awayGoalsAgainstAvg, "1.5")
                )
            ),
            clean_sheet = StatValue(cleanSheetHome, cleanSheetAway, cleanSheetHome + cleanSheetAway),
            failed_to_score = StatValue(failedToScoreHome, failedToScoreAway, failedToScoreHome + failedToScoreAway)
        )
    }

    private fun createMockPrediction(advice: String = "No advice"): PredictionItem {
        return PredictionItem(
            predictions = Prediction(
                winner = TeamWinner(1, "Team", "Comment"),
                win_or_draw = true,
                under_over = null,
                goals = GoalsPrediction("1", "1"),
                advice = advice
            ),
            comparison = Comparison(
                form = ComparisonValue("50%", "50%"),
                att = ComparisonValue("50%", "50%"),
                def = ComparisonValue("50%", "50%"),
                h2h = ComparisonValue("50%", "50%")
            )
        )
    }

    private fun createMockOdds(betName: String, outcomeValue: String, oddValue: String): OddsItem {
        return OddsItem(
            bookmakers = listOf(
                Bookmaker(1, "10Bet", listOf(
                    Bet(1, betName, listOf(
                        OddValue(outcomeValue, oddValue)
                    ))
                ))
            )
        )
    }
}
