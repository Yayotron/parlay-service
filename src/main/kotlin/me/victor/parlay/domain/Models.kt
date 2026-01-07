package me.victor.parlay.domain

data class Match(
    val id: Int,
    val homeTeam: String,
    val awayTeam: String,
    val date: String
)

data class BettingOption(
    val game: String,
    val bet: String,
    val confidence: Int // 0-100
)

data class Parlay(
    val betting: Map<String, String>,
    val successProbability: String,
    val expectedReturn: String
)

data class ParlayRecommendation(
    val lowRiskParley: Parlay,
    val highRiskParley: Parlay
)
