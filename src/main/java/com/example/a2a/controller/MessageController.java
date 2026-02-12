package com.example.a2a.controller;

import io.a2a.spec.AgentCard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MessageController {

    private final AgentCard agentCard;

    public MessageController(AgentCard agentCard) {
        this.agentCard = agentCard;
    }

    @GetMapping("/.well-known/agent-card.json")
    public ResponseEntity<AgentCard> getAgentCard() {
        return ResponseEntity.ok(agentCard);
    }

    @GetMapping("/card")
    public ResponseEntity<AgentCard> getAgentCardAlt() {
        return ResponseEntity.ok(agentCard);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "agent", agentCard.name()
        ));
    }
}
