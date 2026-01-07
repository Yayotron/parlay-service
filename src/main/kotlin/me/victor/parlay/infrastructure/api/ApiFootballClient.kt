package me.victor.parlay.infrastructure.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class ApiFootballClient(
    @Value("\${api-football.base-url}") private val baseUrl: String,
    @Value("\${api-football.api-key}") private val apiKey: String,
    @Value("\${api-football.league-id}") private val leagueId: Int,
    @Value("\${api-football.season}") private val season: Int
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("x-apisports-key", apiKey)
        .build()

    fun getFixtures(date: String): Mono<FixturesResponse> {
        return webClient.get()
            .uri { it.path("/fixtures")
                .queryParam("league", leagueId)
                .queryParam("season", season)
                .queryParam("date", date)
                .build()
            }
            .retrieve()
            .bodyToMono(FixturesResponse::class.java)
    }

    fun getPredictions(fixtureId: Int): Mono<PredictionsResponse> {
        return webClient.get()
            .uri("/predictions") { it.queryParam("fixture", fixtureId).build() }
            .retrieve()
            .bodyToMono(PredictionsResponse::class.java)
    }

    fun getTeamStatistics(teamId: Int, date: String): Mono<TeamStatsResponse> {
        return webClient.get()
            .uri { it.path("/teams/statistics")
                .queryParam("league", leagueId)
                .queryParam("season", season)
                .queryParam("team", teamId)
                .queryParam("date", date)
                .build()
            }
            .retrieve()
            .bodyToMono(TeamStatsResponse::class.java)
    }
}

data class FixturesResponse(val response: List<FixtureItem>)
data class FixtureItem(val fixture: Fixture, val teams: Teams)
data class Fixture(val id: Int, val date: String)
data class Teams(val home: Team, val away: Team)
data class Team(val id: Int, val name: String)

data class PredictionsResponse(val response: List<PredictionItem>)
data class PredictionItem(val predictions: Prediction, val comparison: Comparison)
data class Prediction(val winner: TeamWinner, val win_or_draw: Boolean, val under_over: String?, val goals: GoalsPrediction, val advice: String)
data class TeamWinner(val id: Int?, val name: String?, val comment: String?)
data class GoalsPrediction(val home: String?, val away: String?)
data class Comparison(val form: ComparisonValue, val att: ComparisonValue, val def: ComparisonValue, val h2h: ComparisonValue)
data class ComparisonValue(val home: String, val away: String)

data class TeamStatsResponse(val response: TeamStats)
data class TeamStats(
    val form: String?,
    val fixtures: TeamFixtures,
    val goals: TeamGoalsStats,
    val clean_sheet: StatValue,
    val failed_to_score: StatValue
)
data class TeamFixtures(val played: StatValue, val wins: StatValue, val draws: StatValue, val loses: StatValue)
data class TeamGoalsStats(@com.fasterxml.jackson.annotation.JsonProperty("for") val forGoals: GoalDetails, val against: GoalDetails)
data class GoalDetails(val total: StatValue, val average: StatValue)
data class StatValue(val home: Int?, val away: Int?, val total: Int?, val home_avg: String? = null, val away_avg: String? = null, val total_avg: String? = null)

