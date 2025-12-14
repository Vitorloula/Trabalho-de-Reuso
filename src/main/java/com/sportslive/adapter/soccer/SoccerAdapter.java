package com.sportslive.adapter.soccer;

import com.sportslive.adapter.SportAdapter;
import com.sportslive.domain.model.*;
import com.sportslive.infrastructure.sportradar.SportradarClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
public class SoccerAdapter implements SportAdapter {

    private final SportradarClient client;

    public SoccerAdapter(SportradarClient client) {
        this.client = client;
    }

    @Override
    public Sport getSupportedSport() {
        return Sport.SOCCER;
    }

    @Override
    public SportEvent getEvent(String eventId) {
        var response = client.getSoccerMatch(eventId);
        return mapToSportEvent(response);
    }

    @Override
    public Score getScore(String eventId) {
        var response = client.getSoccerMatch(eventId);
        return mapToScore(response);
    }

    @Override
    public Timeline getTimeline(String eventId) {
        var response = client.getSoccerTimeline(eventId);
        return mapToTimeline(eventId, response);
    }

    @Override
    public Statistics getStatistics(String eventId, StatisticsFilter filter) {
        var response = client.getSoccerStatistics(eventId);
        return mapToStatistics(eventId, response, filter);
    }

    @Override
    public List<SportEvent> listLiveEvents() {
        var response = client.getSoccerLiveEvents();
        return mapToEventList(response);
    }

    @Override
    public List<SportEvent> listScheduledEvents() {
        var response = client.getSoccerScheduledEvents();
        return mapToEventList(response);
    }

    @SuppressWarnings("unchecked")
    private List<SportEvent> mapToEventList(Map<String, Object> response) {
        var summaries = (List<Map<String, Object>>) response.get("summaries");
        if (summaries == null)
            return List.of();

        return summaries.stream()
                .map(this::mapToSportEvent)
                .toList();
    }

    private SportEvent mapToSportEvent(Map<String, Object> response) {
        var sportEvent = getNestedMap(response, "sport_event");
        var status = getNestedMap(response, "sport_event_status");
        var competition = getNestedMap(sportEvent, "sport_event_context", "competition");
        var venue = getNestedMap(sportEvent, "venue");
        var competitors = getNestedList(sportEvent, "competitors");

        return new SportEvent(
                getString(sportEvent, "id"),
                Sport.SOCCER,
                mapStatus(getString(status, "status")),
                new Competition(
                        getString(competition, "id"),
                        getString(competition, "name"),
                        null,
                        null,
                        null),
                mapParticipants(competitors),
                mapToScore(response),
                mapCurrentPeriod(status),
                parseDateTime(getString(sportEvent, "scheduled")),
                parseDateTime(getString(sportEvent, "start_time")),
                venue != null ? new Venue(
                        getString(venue, "id"),
                        getString(venue, "name"),
                        getString(venue, "city_name"),
                        getString(venue, "country_name"),
                        getInteger(venue, "capacity")) : null,
                determineCoverage(response));
    }

    private Score mapToScore(Map<String, Object> response) {
        var status = getNestedMap(response, "sport_event_status");
        if (status == null)
            return Score.of(0, 0);

        int home = getInteger(status, "home_score");
        int away = getInteger(status, "away_score");
        return Score.of(home, away);
    }

    private Timeline mapToTimeline(String eventId, Map<String, Object> response) {
        var timelineEvents = getNestedList(response, "timeline");
        if (timelineEvents == null) {
            return new Timeline(eventId, List.of(), CoverageMetadata.minimal());
        }

        List<TimelineEvent> events = timelineEvents.stream()
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
                getString(event, "match_clock"),
                getString(event, "competitor"),
                null,
                getString(event, "player", "name"),
                null,
                event);
    }

    private Statistics mapToStatistics(String eventId, Map<String, Object> response, StatisticsFilter filter) {
        var stats = getNestedMap(response, "statistics");
        if (stats == null) {
            return new Statistics(eventId, filter.period(), Map.of(),
                    AdvancedStatistics.unavailable(Sport.SOCCER), CoverageMetadata.minimal());
        }

        Map<String, Statistics.ParticipantStatistics> participantStats = Map.of();
        AdvancedStatistics advanced = filter.includeAdvanced()
                ? extractAdvancedStats(response)
                : AdvancedStatistics.unavailable(Sport.SOCCER);

        return new Statistics(eventId, filter.period(), participantStats, advanced, CoverageMetadata.full());
    }

    private AdvancedStatistics extractAdvancedStats(Map<String, Object> response) {
        Map<String, Object> metrics = Map.of(
                "xG", getDouble(response, "statistics", "xg"),
                "xA", getDouble(response, "statistics", "xa"),
                "possession", getDouble(response, "statistics", "possession"));
        return AdvancedStatistics.of(Sport.SOCCER, metrics);
    }

    private EventStatus mapStatus(String status) {
        if (status == null)
            return EventStatus.UNKNOWN;
        return switch (status.toLowerCase()) {
            case "not_started" -> EventStatus.SCHEDULED;
            case "live", "1st_half", "2nd_half" -> EventStatus.LIVE;
            case "ended", "closed" -> EventStatus.FINISHED;
            case "cancelled" -> EventStatus.CANCELLED;
            case "postponed" -> EventStatus.POSTPONED;
            default -> EventStatus.UNKNOWN;
        };
    }

    private Period mapCurrentPeriod(Map<String, Object> status) {
        if (status == null)
            return null;
        String matchStatus = getString(status, "match_status");
        String clock = getString(status, "clock", "match_time");
        return new Period(matchStatus, null, clock, true);
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
            case "score_change" -> "GOAL";
            case "yellow_card" -> "YELLOW_CARD";
            case "red_card" -> "RED_CARD";
            case "substitution" -> "SUBSTITUTION";
            case "penalty_awarded" -> "PENALTY";
            default -> "OTHER";
        };
    }

    private CoverageMetadata determineCoverage(Map<String, Object> response) {
        var coverage = getNestedMap(response, "sport_event", "coverage");
        if (coverage == null)
            return CoverageMetadata.basic();

        boolean hasLive = Boolean.TRUE.equals(coverage.get("live"));
        return hasLive ? CoverageMetadata.full() : CoverageMetadata.basic();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(key);
            } else {
                return null;
            }
        }
        return current instanceof Map ? (Map<String, Object>) current : null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getNestedList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof List ? (List<Map<String, Object>>) value : null;
    }

    private String getString(Map<String, Object> map, String... keys) {
        if (keys.length == 1) {
            Object value = map.get(keys[0]);
            return value != null ? value.toString() : null;
        }
        Map<String, Object> nested = getNestedMap(map, java.util.Arrays.copyOf(keys, keys.length - 1));
        return nested != null ? getString(nested, keys[keys.length - 1]) : null;
    }

    private int getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number)
            return ((Number) value).intValue();
        return 0;
    }

    private double getDouble(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1
                ? getNestedMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        if (target == null)
            return 0.0;
        Object value = target.get(keys[keys.length - 1]);
        if (value instanceof Number)
            return ((Number) value).doubleValue();
        return 0.0;
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
