package com.sportslive.service;

import com.sportslive.adapter.SportAdapter;
import com.sportslive.adapter.SportAdapter.StatisticsFilter;
import com.sportslive.domain.model.*;
import com.sportslive.exception.UnsupportedSportException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class EventService {

    private final Map<String, SportAdapter> adapters;

    public EventService(@Qualifier("sportAdapters") Map<String, SportAdapter> adapters) {
        this.adapters = adapters;
    }

    @Cacheable(value = "event-details", key = "#sport + '-' + #eventId")
    public SportEvent getEvent(String sport, String eventId) {
        return getAdapter(sport).getEvent(eventId);
    }

    @Cacheable(value = "live-scores", key = "#sport + '-' + #eventId")
    public Score getScore(String sport, String eventId) {
        return getAdapter(sport).getScore(eventId);
    }

    @Cacheable(value = "timeline", key = "#sport + '-' + #eventId")
    public Timeline getTimeline(String sport, String eventId) {
        return getAdapter(sport).getTimeline(eventId);
    }

    @Cacheable(value = "statistics", key = "#sport + '-' + #eventId + '-' + #filter.period() + '-' + #filter.includeAdvanced()")
    public Statistics getStatistics(String sport, String eventId, StatisticsFilter filter) {
        return getAdapter(sport).getStatistics(eventId, filter);
    }

    @Cacheable(value = "live-events", key = "#sport")
    public java.util.List<SportEvent> listLiveEvents(String sport) {
        return getAdapter(sport).listLiveEvents();
    }

    @Cacheable(value = "scheduled-events", key = "#sport")
    public java.util.List<SportEvent> listScheduledEvents(String sport) {
        return getAdapter(sport).listScheduledEvents();
    }

    private SportAdapter getAdapter(String sport) {
        SportAdapter adapter = adapters.get(sport.toLowerCase());
        if (adapter == null) {
            throw new UnsupportedSportException(sport);
        }
        return adapter;
    }
}
