package com.sportslive.config;

import com.sportslive.adapter.SportAdapter;
import com.sportslive.adapter.soccer.SoccerAdapter;
import com.sportslive.adapter.basketball.BasketballAdapter;
import com.sportslive.adapter.tennis.TennisAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AdapterConfig {

    @Bean
    @Primary
    public Map<String, SportAdapter> sportAdapters(
            SoccerAdapter soccerAdapter,
            BasketballAdapter basketballAdapter,
            TennisAdapter tennisAdapter) {
        Map<String, SportAdapter> adapters = new HashMap<>();
        adapters.put("soccer", soccerAdapter);
        adapters.put("basketball", basketballAdapter);
        adapters.put("tennis", tennisAdapter);
        return adapters;
    }
}
