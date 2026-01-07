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
                apiClient.getPredictions(item.fixture.id)
                    .map { predictionResponse ->
                        val matchName = "${item.teams.home.name} vs ${item.teams.away.name}"
                        analysisEngine.analyzeMatch(matchName, predictionResponse.response.first())
                    }
            }
            .collectList()
            .map { allOptions ->
                val flattenedOptions = allOptions.flatten()
                
                // Create Low Risk Parlay (highest confidence options)
                val lowRiskOptions = flattenedOptions.sortedByDescending { it.confidence }.take(3)
                val lowRiskProb = analysisEngine.calculateProbability(lowRiskOptions)
                
                // Create High Risk Parlay (more options or lower confidence)
                val highRiskOptions = flattenedOptions.sortedBy { it.confidence }.take(3)
                val highRiskProb = analysisEngine.calculateProbability(highRiskOptions)

                ParlayRecommendation(
                    lowRiskParley = Parlay(
                        betting = lowRiskOptions.associate { it.game to it.bet },
                        successProbability = "$lowRiskProb%",
                        expectedReturn = "${analysisEngine.calculateReturn(lowRiskProb, false)}%"
                    ),
                    highRiskParley = Parlay(
                        betting = highRiskOptions.associate { it.game to it.bet },
                        successProbability = "$highRiskProb%",
                        expectedReturn = "${analysisEngine.calculateReturn(highRiskProb, true)}%"
                    )
                )
            }
    }
}
