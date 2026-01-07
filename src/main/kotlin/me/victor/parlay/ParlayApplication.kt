package me.victor.parlay

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ParlayApplication

fun main(args: Array<String>) {
    runApplication<ParlayApplication>(*args)
}
