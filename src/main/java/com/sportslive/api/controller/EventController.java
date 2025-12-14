package com.sportslive.api.controller;

import com.sportslive.adapter.SportAdapter.StatisticsFilter;
import com.sportslive.domain.model.*;
import com.sportslive.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/{sport}/events")
@Tag(name = "Events", description = "API unificada para eventos esportivos")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{eventId}")
    @Operation(summary = "Obter detalhes do evento", description = "Retorna informações completas do evento esportivo")
    public ResponseEntity<SportEvent> getEvent(
            @Parameter(description = "Esporte: soccer, basketball, tennis") @PathVariable String sport,
            @Parameter(description = "ID do evento") @PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getEvent(sport, eventId));
    }

    @GetMapping("/{eventId}/score")
    @Operation(summary = "Obter placar atual", description = "Retorna o placar do evento em tempo real")
    public ResponseEntity<Score> getScore(
            @PathVariable String sport,
            @PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getScore(sport, eventId));
    }

    @GetMapping("/{eventId}/timeline")
    @Operation(summary = "Obter timeline", description = "Retorna eventos play-by-play do jogo")
    public ResponseEntity<Timeline> getTimeline(
            @PathVariable String sport,
            @PathVariable String eventId) {
        return ResponseEntity.ok(eventService.getTimeline(sport, eventId));
    }

    @GetMapping("/{eventId}/stats")
    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas básicas e avançadas do evento")
    public ResponseEntity<Statistics> getStatistics(
            @PathVariable String sport,
            @PathVariable String eventId,
            @Parameter(description = "Incluir estatísticas avançadas") @RequestParam(defaultValue = "false") boolean advanced,
            @Parameter(description = "Período: total, 1st_half, 2nd_half, q1, q2, set1, etc") @RequestParam(defaultValue = "total") String period) {

        StatisticsFilter filter = new StatisticsFilter(advanced, period);
        return ResponseEntity.ok(eventService.getStatistics(sport, eventId, filter));
    }

    @GetMapping("/live")
    @Operation(summary = "Listar eventos ao vivo", description = "Retorna todos os eventos em andamento do esporte")
    public ResponseEntity<java.util.List<SportEvent>> listLiveEvents(
            @Parameter(description = "Esporte: soccer, basketball, tennis") @PathVariable String sport) {
        return ResponseEntity.ok(eventService.listLiveEvents(sport));
    }

    @GetMapping("/scheduled")
    @Operation(summary = "Listar eventos agendados", description = "Retorna eventos agendados para hoje")
    public ResponseEntity<java.util.List<SportEvent>> listScheduledEvents(
            @Parameter(description = "Esporte: soccer, basketball, tennis") @PathVariable String sport) {
        return ResponseEntity.ok(eventService.listScheduledEvents(sport));
    }
}
