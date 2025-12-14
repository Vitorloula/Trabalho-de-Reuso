package com.sportslive.adapter;

import com.sportslive.domain.model.*;

import java.util.List;

public interface SportAdapter {

    Sport getSupportedSport();

    SportEvent getEvent(String eventId);

    Score getScore(String eventId);

    Timeline getTimeline(String eventId);

    Statistics getStatistics(String eventId, StatisticsFilter filter);

    List<SportEvent> listLiveEvents();

    List<SportEvent> listScheduledEvents();

    record StatisticsFilter(
            boolean includeAdvanced,
            String period) {
        public static StatisticsFilter basic() {
            return new StatisticsFilter(false, "total");
        }

        public static StatisticsFilter advanced(String period) {
            return new StatisticsFilter(true, period);
        }
    }
}
