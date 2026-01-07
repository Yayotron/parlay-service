package me.victor.parlay.infrastructure.web

import me.victor.parlay.domain.ParlayRecommendation
import me.victor.parlay.service.ParlayService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class ParlayController(private val parlayService: ParlayService) {

    @GetMapping("/parlays")
    fun getParlays(@RequestParam date: String): Mono<ParlayRecommendation> {
        return parlayService.getRecommendations(date)
    }
}
