package com.sportslive.adapter.tennis;

import com.sportslive.adapter.SportAdapter;
import com.sportslive.domain.model.*;
import com.sportslive.infrastructure.sportradar.SportradarClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class TennisAdapter implements SportAdapter {

    private final SportradarClient client;

    public TennisAdapter(SportradarClient client) {
        this.client = client;
    }

    @Override
    public Sport getSupportedSport() {
        return Sport.TENNIS;
    }

    @Override
    public SportEvent getEvent(String eventId) {
        var response = client.getTennisMatch(eventId);
        return mapToSportEvent(response);
    }

    @Override
    public Score getScore(String eventId) {
        var response = client.getTennisMatch(eventId);
        return mapToScore(response);
    }

    @Override
    public Timeline getTimeline(String eventId) {
        var response = client.getTennisTimeline(eventId);
        return mapToTimeline(eventId, response);
    }

    @Override
    public Statistics getStatistics(String eventId, StatisticsFilter filter) {
        var response = client.getTennisStatistics(eventId);
        return mapToStatistics(eventId, response, filter);
    }

    @Override
    public List<SportEvent> listLiveEvents() {
        var response = client.getTennisLiveEvents();
        return mapToEventList(response);
    }

    @Override
    public List<SportEvent> listScheduledEvents() {
        var response = client.getTennisScheduledEvents();
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
                Sport.TENNIS,
                mapStatus(getString(status, "status")),
                competition != null ? new Competition(
                        getString(competition, "id"),
                        getString(competition, "name"),
                        null,
                        null,
                        getString(sportEvent, "sport_event_context", "round", "name")) : null,
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
                        null) : null,
                CoverageMetadata.full());
    }

    private Score mapToScore(Map<String, Object> response) {
        var status = getMap(response, "sport_event_status");
        if (status == null)
            return new Score(null, null, Map.of(), "0-0");

        var periodScores = getList(status, "period_scores");
        StringBuilder display = new StringBuilder();

        if (periodScores != null) {
            for (var period : periodScores) {
                int home = getInt(period, "home_score");
                int away = getInt(period, "away_score");
                display.append(home).append("-").append(away).append(" ");
            }
        }

        String gameScore = getInt(status, "game_state", "home_score") + "-" +
                getInt(status, "game_state", "away_score");

        return new Score(null, null, Map.of(), display.toString().trim() + " (" + gameScore + ")");
    }

    private Timeline mapToTimeline(String eventId, Map<String, Object> response) {
        var timeline = getList(response, "timeline");
        if (timeline == null) {
            return new Timeline(eventId, List.of(), CoverageMetadata.minimal());
        }

        List<TimelineEvent> events = timeline.stream()
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
                "Set " + getInt(event, "set"),
                null,
                getString(event, "competitor"),
                getString(event, "player", "id"),
                getString(event, "player", "name"),
                null,
                event);
    }

    private Statistics mapToStatistics(String eventId, Map<String, Object> response, StatisticsFilter filter) {
        AdvancedStatistics advanced = filter.includeAdvanced()
                ? extractAdvancedStats(response)
                : AdvancedStatistics.unavailable(Sport.TENNIS);

        return new Statistics(eventId, filter.period(), Map.of(), advanced, CoverageMetadata.full());
    }

    private AdvancedStatistics extractAdvancedStats(Map<String, Object> response) {
        var stats = getMap(response, "statistics");
        if (stats == null)
            return AdvancedStatistics.unavailable(Sport.TENNIS);

        Map<String, Object> metrics = Map.of(
                "first_serve_pct", getDouble(stats, "first_serve_pct"),
                "first_serve_points_won_pct", getDouble(stats, "first_serve_points_won_pct"),
                "second_serve_points_won_pct", getDouble(stats, "second_serve_points_won_pct"),
                "break_points_converted", getInt(stats, "break_points_converted"),
                "break_points_saved", getInt(stats, "break_points_saved"),
                "aces", getInt(stats, "aces"),
                "double_faults", getInt(stats, "double_faults"),
                "winners", getInt(stats, "winners"),
                "unforced_errors", getInt(stats, "unforced_errors"));
        return AdvancedStatistics.of(Sport.TENNIS, metrics);
    }

    private EventStatus mapStatus(String status) {
        if (status == null)
            return EventStatus.UNKNOWN;
        return switch (status.toLowerCase()) {
            case "not_started" -> EventStatus.SCHEDULED;
            case "live", "inprogress" -> EventStatus.LIVE;
            case "ended", "closed" -> EventStatus.FINISHED;
            case "cancelled" -> EventStatus.CANCELLED;
            case "postponed" -> EventStatus.POSTPONED;
            default -> EventStatus.UNKNOWN;
        };
    }

    private Period mapCurrentPeriod(Map<String, Object> status) {
        if (status == null)
            return null;
        int set = getInt(status, "current_set");
        return new Period("Set " + set, set, null, true);
    }

    private List<Participant> mapParticipants(List<Map<String, Object>> competitors) {
        if (competitors == null)
            return List.of();
        return competitors.stream()
                .map(c -> new Participant(
                        getString(c, "id"),
                        getString(c, "name"),
                        getString(c, "abbreviation"),
                        Participant.ParticipantType.PLAYER,
                        getString(c, "country"),
                        null))
                .toList();
    }

    private String normalizeEventType(String type) {
        if (type == null)
            return "OTHER";
        return switch (type.toLowerCase()) {
            case "ace" -> "ACE";
            case "double_fault" -> "DOUBLE_FAULT";
            case "break_point" -> "BREAK_POINT";
            case "set_end" -> "SET_END";
            case "match_end" -> "MATCH_END";
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

    private String getString(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1 ? getMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        Object value = target != null ? target.get(keys[keys.length - 1]) : null;
        return value != null ? value.toString() : null;
    }

    private int getInt(Map<String, Object> map, String... keys) {
        Map<String, Object> target = keys.length > 1 ? getMap(map, java.util.Arrays.copyOf(keys, keys.length - 1))
                : map;
        Object value = target != null ? target.get(keys[keys.length - 1]) : null;
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object value = map != null ? map.get(key) : null;
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
