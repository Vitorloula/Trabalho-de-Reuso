package com.sportslive.adapter.basketball;

import com.sportslive.adapter.SportAdapter;
import com.sportslive.domain.model.*;
import com.sportslive.infrastructure.sportradar.SportradarClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class BasketballAdapter implements SportAdapter {

    private final SportradarClient client;

    public BasketballAdapter(SportradarClient client) {
        this.client = client;
    }

    @Override
    public Sport getSupportedSport() {
        return Sport.BASKETBALL;
    }

    @Override
    public SportEvent getEvent(String eventId) {
        var response = client.getBasketballGame(eventId);
        return mapToSportEvent(response);
    }

    @Override
    public Score getScore(String eventId) {
        var response = client.getBasketballGame(eventId);
        return mapToScore(response);
    }

    @Override
    public Timeline getTimeline(String eventId) {
        var response = client.getBasketballPlayByPlay(eventId);
        return mapToTimeline(eventId, response);
    }

    @Override
    public Statistics getStatistics(String eventId, StatisticsFilter filter) {
        var response = client.getBasketballStatistics(eventId);
        return mapToStatistics(eventId, response, filter);
    }

    @Override
    public List<SportEvent> listLiveEvents() {
        var response = client.getBasketballLiveEvents();
        return mapToEventList(response);
    }

    @Override
    public List<SportEvent> listScheduledEvents() {
        var response = client.getBasketballScheduledEvents();
        return mapToEventList(response);
    }

    @SuppressWarnings("unchecked")
    private List<SportEvent> mapToEventList(Map<String, Object> response) {
        var summaries = (List<Map<String, Object>>) response.get("summaries");
        if (summaries == null)
            return List.of();
        return summaries.stream().map(this::mapToSportEvent).toList();
    }

    private SportEvent mapToSportEvent(Map<String, Object> response) {
        var sportEvent = getMap(response, "sport_event");
        var status = getMap(response, "sport_event_status");
        var competition = getMap(sportEvent, "sport_event_context", "competition");
        var venue = getMap(sportEvent, "venue");
        var competitors = getList(sportEvent, "competitors");

        return new SportEvent(
                getString(sportEvent, "id"),
                Sport.BASKETBALL,
                mapStatus(getString(status, "status")),
                competition != null ? new Competition(
                        getString(competition, "id"),
                        getString(competition, "name"),
                        null, null, null) : null,
                mapParticipants(competitors),
                mapToScore(response),
                mapCurrentPeriod(status),
                parseDateTime(getString(sportEvent, "scheduled")),
                null,
                venue != null ? new Venue(
                        getString(venue, "id"),
                        getString(venue, "name"),
                        getString(venue, "city_name"),
                        getString(venue, "country_name"),
                        getInt(venue, "capacity")) : null,
                CoverageMetadata.full());
    }

    private Score mapToScore(Map<String, Object> response) {
        var status = getMap(response, "sport_event_status");
        if (status == null)
            return Score.of(0, 0);

        int home = getInt(status, "home_score");
        int away = getInt(status, "away_score");

        Map<String, Score.PeriodScore> quarters = Map.of(
                "Q1",
                new Score.PeriodScore("Q1", getInt(status, "period_scores", "home_score_q1"),
                        getInt(status, "period_scores", "away_score_q1")),
                "Q2",
                new Score.PeriodScore("Q2", getInt(status, "period_scores", "home_score_q2"),
                        getInt(status, "period_scores", "away_score_q2")),
                "Q3",
                new Score.PeriodScore("Q3", getInt(status, "period_scores", "home_score_q3"),
                        getInt(status, "period_scores", "away_score_q3")),
                "Q4", new Score.PeriodScore("Q4", getInt(status, "period_scores", "home_score_q4"),
                        getInt(status, "period_scores", "away_score_q4")));

        return new Score(home, away, quarters, home + " - " + away);
    }

    private Timeline mapToTimeline(String eventId, Map<String, Object> response) {
        var plays = getList(response, "sport_event_status", "play_by_play");
        if (plays == null) {
            return new Timeline(eventId, List.of(), CoverageMetadata.minimal());
        }

        List<TimelineEvent> events = plays.stream()
                .map(this::mapTimelineEvent)
                .toList();

        return new Timeline(eventId, events, CoverageMetadata.full());
    }

    private TimelineEvent mapTimelineEvent(Map<String, Object> event) {
        String type = getString(event, "type");
        return new TimelineEvent(
                getString(event, "id"),
                type,
                normalizeEventType(type),
                null,
                getString(event, "period"),
                getString(event, "clock"),
                getString(event, "team_id"),
                getString(event, "player_id"),
                getString(event, "player_name"),
                getString(event, "description"),
                event);
    }

    private Statistics mapToStatistics(String eventId, Map<String, Object> response, StatisticsFilter filter) {
        AdvancedStatistics advanced = filter.includeAdvanced()
                ? extractAdvancedStats(response)
                : AdvancedStatistics.unavailable(Sport.BASKETBALL);

        return new Statistics(eventId, filter.period(), Map.of(), advanced, CoverageMetadata.full());
    }

    private AdvancedStatistics extractAdvancedStats(Map<String, Object> response) {
        Map<String, Object> metrics = Map.of(
                "efg_pct", getDouble(response, "statistics", "efg_pct"),
                "ts_pct", getDouble(response, "statistics", "ts_pct"),
                "offensive_rating", getDouble(response, "statistics", "offensive_rating"),
                "defensive_rating", getDouble(response, "statistics", "defensive_rating"),
                "pace", getDouble(response, "statistics", "pace"));
        return AdvancedStatistics.of(Sport.BASKETBALL, metrics);
    }

    private EventStatus mapStatus(String status) {
        if (status == null)
            return EventStatus.UNKNOWN;
        return switch (status.toLowerCase()) {
            case "not_started", "scheduled" -> EventStatus.SCHEDULED;
            case "inprogress", "live" -> EventStatus.LIVE;
            case "complete", "closed" -> EventStatus.FINISHED;
            case "cancelled" -> EventStatus.CANCELLED;
            case "postponed" -> EventStatus.POSTPONED;
            default -> EventStatus.UNKNOWN;
        };
    }

    private Period mapCurrentPeriod(Map<String, Object> status) {
        if (status == null)
            return null;
        int quarter = getInt(status, "period");
        String clock = getString(status, "clock");
        return new Period("Quarter " + quarter, quarter, clock, true);
    }

    private List<Participant> mapParticipants(List<Map<String, Object>> competitors) {
        if (competitors == null)
            return List.of();
        return competitors.stream()
                .map(c -> new Participant(
                        getString(c, "id"),
                        getString(c, "name"),
                        getString(c, "abbreviation"),
                        Participant.ParticipantType.TEAM,
                        getString(c, "country"),
                        null))
                .toList();
    }

    private String normalizeEventType(String type) {
        if (type == null)
            return "OTHER";
        return switch (type.toLowerCase()) {
            case "point", "basket", "free_throw" -> "POINT";
            case "foul" -> "FOUL";
            case "timeout" -> "TIMEOUT";
            case "period_end" -> "QUARTER_END";
            default -> "OTHER";
        };
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else
                return null;
        }
        return current instanceof Map ? (Map<String, Object>) current : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getList(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1 ? getMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        Object value = target != null ? target.get(keys[keys.length - 1]) : null;
        return value instanceof List ? (List<Map<String, Object>>) value : null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1 ? getMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        Object value = target != null ? target.get(keys[keys.length - 1]) : null;
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private double getDouble(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1 ? getMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        Object value = target != null ? target.get(keys[keys.length - 1]) : null;
        return value instanceof Number ? ((Number) value).doubleValue() : 0.0;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null)
            return null;
        try {
            return LocalDateTime.parse(dateStr.replace("Z", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
