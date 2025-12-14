package com.sportslive.infrastructure.sportradar;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class SportradarClient {

    private static final Logger log = LoggerFactory.getLogger(SportradarClient.class);

    private final WebClient soccerClient;
    private final WebClient basketballClient;
    private final WebClient tennisClient;
    private final String apiKey;

    public SportradarClient(
            WebClient.Builder webClientBuilder,
            @Value("${sportradar.base-urls.soccer}") String soccerUrl,
            @Value("${sportradar.base-urls.basketball}") String basketballUrl,
            @Value("${sportradar.base-urls.tennis}") String tennisUrl,
            @Value("${sportradar.api-key}") String apiKey) {

        this.apiKey = apiKey;
        this.soccerClient = webClientBuilder.baseUrl(soccerUrl).build();
        this.basketballClient = webClientBuilder.baseUrl(basketballUrl).build();
        this.tennisClient = webClientBuilder.baseUrl(tennisUrl).build();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getSoccerMatch(String matchId) {
        log.debug("Fetching soccer match: {}", matchId);
        return soccerClient.get()
                .uri("/sport_events/{id}/summary.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getSoccerTimeline(String matchId) {
        log.debug("Fetching soccer timeline: {}", matchId);
        return soccerClient.get()
                .uri("/sport_events/{id}/timeline.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getSoccerStatistics(String matchId) {
        log.debug("Fetching soccer statistics: {}", matchId);
        return soccerClient.get()
                .uri("/sport_events/{id}/summary.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getBasketballGame(String gameId) {
        log.debug("Fetching basketball game: {}", gameId);
        return basketballClient.get()
                .uri("/games/{id}/summary.json?api_key={key}", gameId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getBasketballPlayByPlay(String gameId) {
        log.debug("Fetching basketball play-by-play: {}", gameId);
        return basketballClient.get()
                .uri("/games/{id}/pbp.json?api_key={key}", gameId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getBasketballStatistics(String gameId) {
        log.debug("Fetching basketball statistics: {}", gameId);
        return basketballClient.get()
                .uri("/games/{id}/summary.json?api_key={key}", gameId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getTennisMatch(String matchId) {
        log.debug("Fetching tennis match: {}", matchId);
        return tennisClient.get()
                .uri("/sport_events/{id}/summary.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getTennisTimeline(String matchId) {
        log.debug("Fetching tennis timeline: {}", matchId);
        return tennisClient.get()
                .uri("/sport_events/{id}/timeline.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getTennisStatistics(String matchId) {
        log.debug("Fetching tennis statistics: {}", matchId);
        return tennisClient.get()
                .uri("/sport_events/{id}/summary.json?api_key={key}", matchId, apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getSoccerLiveEvents() {
        log.debug("Fetching soccer live events");
        return soccerClient.get()
                .uri("/schedules/live/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getSoccerScheduledEvents() {
        log.debug("Fetching soccer scheduled events");
        return soccerClient.get()
                .uri("/schedules/2024-12-14/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getBasketballLiveEvents() {
        log.debug("Fetching basketball live events");
        return basketballClient.get()
                .uri("/schedules/live/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getBasketballScheduledEvents() {
        log.debug("Fetching basketball scheduled events");
        return basketballClient.get()
                .uri("/schedules/2024-12-14/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getTennisLiveEvents() {
        log.debug("Fetching tennis live events");
        return tennisClient.get()
                .uri("/schedules/live/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @CircuitBreaker(name = "sportradar", fallbackMethod = "fallbackMap")
    @Retry(name = "sportradar")
    public Map<String, Object> getTennisScheduledEvents() {
        log.debug("Fetching tennis scheduled events");
        return tennisClient.get()
                .uri("/schedules/2024-12-14/summaries.json?api_key={key}", apiKey)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    @SuppressWarnings("unused")
    private Map<String, Object> fallbackMap(String id, Throwable t) {
        log.warn("Fallback triggered for id: {}, error: {}", id, t.getMessage());
        return Map.of(
                "error", true,
                "message", "Service temporarily unavailable",
                "provider_status", "unavailable");
    }
}
