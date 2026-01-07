package me.victor.parlay.service

import me.victor.parlay.domain.BettingOption
import me.victor.parlay.domain.Parlay
import me.victor.parlay.domain.ParlayRecommendation
import me.victor.parlay.infrastructure.api.ApiFootballClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ParlayService(
    private val apiClient: ApiFootballClient,
    private val analysisEngine: AnalysisEngine
) {

    fun getRecommendations(date: String): Mono<ParlayRecommendation> {
        return apiClient.getFixtures(date)
            .flatMapMany { Flux.fromIterable(it.response) }
            .flatMap { item ->
                val fixtureId = item.fixture.id
                val homeId = item.teams.home.id
                val awayId = item.teams.away.id
                
                Mono.zip(
                    apiClient.getPredictions(fixtureId),
                    apiClient.getTeamStatistics(homeId, date),
                    apiClient.getTeamStatistics(awayId, date)
                ).map { tuple ->
                    val matchName = "${item.teams.home.name} vs ${item.teams.away.name}"
                    analysisEngine.analyzeMatch(matchName, tuple.t1.response.first(), tuple.t2.response, tuple.t3.response)
                }
            }
            .collectList()
            .map { allOptions ->
                val flattenedOptions = allOptions.flatten()
                
                // 1. Low Risk Parlay: Highest confidence options, combined prob > 60%
                val lowRiskOptions = selectOptionsForParlay(flattenedOptions, 60, 3)
                val lowRiskProb = analysisEngine.calculateProbability(lowRiskOptions)
                
                // 2. High Risk Parlay: Lower confidence, combined prob >= 30%
                val highRiskOptions = selectOptionsForParlay(flattenedOptions, 30, 4)
                val highRiskProb = analysisEngine.calculateProbability(highRiskOptions)

                ParlayRecommendation(
                    lowRiskParley = Parlay(
                        betting = lowRiskOptions.associate { it.game to it.bet },
                        successProbability = "$lowRiskProb%",
                        expectedReturn = "${analysisEngine.calculateReturn(lowRiskProb)}%"
                    ),
                    highRiskParley = Parlay(
                        betting = highRiskOptions.associate { it.game to it.bet },
                        successProbability = "$highRiskProb%",
                        expectedReturn = "${analysisEngine.calculateReturn(highRiskProb)}%"
                    )
                )
            }
    }

    private fun selectOptionsForParlay(options: List<BettingOption>, targetProb: Int, count: Int): List<BettingOption> {
        // Sort by confidence descending for low risk, or find a mix for high risk
        val sorted = options.sortedByDescending { it.confidence }
        val selected = mutableListOf<BettingOption>()
        
        for (option in sorted) {
            if (selected.size >= count) break
            val currentProb = analysisEngine.calculateProbability(selected + option)
            if (currentProb >= targetProb || selected.isEmpty()) {
                selected.add(option)
            }
        }
        
        // If we can't reach target with highest, just take the top ones
        return if (selected.isEmpty()) sorted.take(count) else selected
    }
}
