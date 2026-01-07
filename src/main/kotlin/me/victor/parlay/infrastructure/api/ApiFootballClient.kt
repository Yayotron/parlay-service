package me.victor.parlay.infrastructure.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class ApiFootballClient(
    @Value("\${api-football.base-url}") private val baseUrl: String,
    @Value("\${api-football.api-key}") private val apiKey: String,
    @Value("\${api-football.league-id}") private val leagueId: Int,
    @Value("\${api-football.season}") private val season: Int,
    private val objectMapper: ObjectMapper
) {
    private val webClient = WebClient.builder()
        .baseUrl(baseUrl)
        .defaultHeader("x-apisports-key", apiKey)
        .build()
    private val responseDirectory: Path = Paths.get("src/main/resources/data/responses")
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")

    fun getFixtures(date: String): Mono<FixturesResponse> {
        val identifier = "getFixturesDate${sanitizeSegment(date)}"
        return fetchAndStoreResponse(
            identifier,
            { it.path("/fixtures")
                .queryParam("league", leagueId)
                .queryParam("season", season)
                .queryParam("date", date)
                .build()
            },
            FixturesResponse::class.java
        )
    }

    fun getPredictions(fixtureId: Int): Mono<PredictionsResponse> {
        val identifier = "getPredictionsFixture$fixtureId"
        return fetchAndStoreResponse(
            identifier,
            { it.path("/predictions").queryParam("fixture", fixtureId).build() },
            PredictionsResponse::class.java
        )
    }

    fun getTeamStatistics(teamId: Int, date: String): Mono<TeamStatsResponse> {
        val identifier = "getTeamStatisticsTeam${teamId}Date${sanitizeSegment(date)}"
        return fetchAndStoreResponse(
            identifier,
            { it.path("/teams/statistics")
                .queryParam("league", leagueId)
                .queryParam("season", season)
                .queryParam("team", teamId)
                .queryParam("date", date)
                .build()
            },
            TeamStatsResponse::class.java
        )
    }

    fun getOdds(fixtureId: Int): Mono<OddsResponse> {
        val identifier = "getOddsFixture$fixtureId"
        return fetchAndStoreResponse(
            identifier,
            { it.path("/odds").queryParam("fixture", fixtureId).build() },
            OddsResponse::class.java
        )
    }

    private fun <T> fetchAndStoreResponse(
        identifier: String,
        uriBuilder: (org.springframework.web.util.UriBuilder) -> java.net.URI,
        responseType: Class<T>
    ): Mono<T> {
        return webClient.get()
            .uri(uriBuilder)
            .retrieve()
            .bodyToMono(String::class.java)
            .flatMap { body ->
                saveResponse(identifier, body).thenReturn(objectMapper.readValue(body, responseType))
            }
    }

    private fun saveResponse(identifier: String, body: String): Mono<Void> {
        val timestamp = LocalDateTime.now().format(timestampFormatter)
        val filename = "${sanitizeSegment(identifier)}$timestamp"
        val filePath = responseDirectory.resolve(filename)
        return Mono.fromCallable {
            Files.createDirectories(responseDirectory)
            Files.writeString(
                filePath,
                body,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
            .then()
    }

    private fun sanitizeSegment(value: String): String {
        return value.replace(Regex("[^A-Za-z0-9]"), "")
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

data class OddsResponse(val response: List<OddsItem>)
data class OddsItem(val bookmakers: List<Bookmaker>)
data class Bookmaker(val id: Int, val name: String, val bets: List<Bet>)
data class Bet(val id: Int, val name: String, val values: List<OddValue>)
data class OddValue(val value: String, val odd: String)
