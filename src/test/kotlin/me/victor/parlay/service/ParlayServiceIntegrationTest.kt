package me.victor.parlay.service

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import reactor.test.StepVerifier

@SpringBootTest
class ParlayServiceIntegrationTest {

    @Autowired
    private lateinit var parlayService: ParlayService

    companion object {
        private lateinit var mockWebServer: MockWebServer

        @BeforeAll
        @JvmStatic
        fun setUp() {
            mockWebServer = MockWebServer()
            mockWebServer.start()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            mockWebServer.shutdown()
        }

        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("api-football.base-url") { mockWebServer.url("/").toString() }
            registry.add("api-football.api-key") { "test-key" }
            registry.add("api-football.league-id") { "39" }
            registry.add("api-football.season") { "2023" }
        }
    }

    @Test
    fun `should generate parlay recommendations from mocked api responses`() {
        // 1. Mock Fixtures Response
        mockWebServer.enqueue(MockResponse()
            .setBody("""{"response": [{"fixture": {"id": 1, "date": "2023-10-01"}, "teams": {"home": {"id": 1, "name": "Arsenal"}, "away": {"id": 2, "name": "Chelsea"}}}]}""")
            .addHeader("Content-Type", "application/json"))

        // 2. Mock Predictions Response
        mockWebServer.enqueue(MockResponse()
            .setBody("""{"response": [{"predictions": {"winner": {"id": 1, "name": "Arsenal"}, "advice": "Home Win", "win_or_draw": true, "goals": {"home": "2", "away": "1"}}, "comparison": {"form": {"home": "80%", "away": "20%"}, "att": {"home": "80%", "away": "20%"}, "def": {"home": "80%", "away": "20%"}, "h2h": {"home": "80%", "away": "20%"}}}]}""")
            .addHeader("Content-Type", "application/json"))

        // 3. Mock Home Team Stats
        mockWebServer.enqueue(MockResponse()
            .setBody("""{"response": {"form": "WWWWW", "fixtures": {"played": {"total": 5, "home": 3}, "wins": {"total": 5, "home": 3}, "draws": {"total": 0}, "loses": {"total": 0}}, "goals": {"for": {"total": {"home": 15, "total": 15}, "average": {"home_avg": "3.0", "total_avg": "3.0"}}, "against": {"total": {"home": 2, "total": 2}, "average": {"home_avg": "0.5", "total_avg": "0.5"}}}, "clean_sheet": {"total": 3, "home": 2}, "failed_to_score": {"total": 0}}}""")
            .addHeader("Content-Type", "application/json"))

        // 4. Mock Away Team Stats
        mockWebServer.enqueue(MockResponse()
            .setBody("""{"response": {"form": "LLLLL", "fixtures": {"played": {"total": 5, "away": 3}, "wins": {"total": 0}, "draws": {"total": 0}, "loses": {"total": 5, "away": 3}}, "goals": {"for": {"total": {"away": 2, "total": 2}, "average": {"away_avg": "0.5", "total_avg": "0.5"}}, "against": {"total": {"away": 15, "total": 15}, "average": {"away_avg": "3.0", "total_avg": "3.0"}}}, "clean_sheet": {"total": 0}, "failed_to_score": {"total": 3, "away": 2}}}""")
            .addHeader("Content-Type", "application/json"))

        // 5. Mock Odds Response
        mockWebServer.enqueue(MockResponse()
            .setBody("""{"response": [{"bookmakers": [{"id": 1, "name": "10Bet", "bets": [{"id": 1, "name": "Match Winner", "values": [{"value": "Home", "odd": "1.4"}]}]}]}]}""")
            .addHeader("Content-Type", "application/json"))

        val result = parlayService.getRecommendations("2023-10-01")

        StepVerifier.create(result)
            .assertNext { recommendation ->
                assertThat(recommendation.lowRiskParley).isNotNull
                assertThat(recommendation.highRiskParley).isNotNull
                assertThat(recommendation.lowRiskParley.betting).isNotEmpty
                assertThat(recommendation.lowRiskParley.successProbability).contains("%")
            }
            .verifyComplete()
    }
}
