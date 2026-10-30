package com.example.demo_pam.service;

import com.example.demo_pam.dto.response.RestaurantTableResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class TableStatusSseService {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("status-stream"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    // Notifica a todos los emitters conectados un cambio de estado de una mesa
    public void broadcast(RestaurantTableResponse response) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("table-status").data(response));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }
}
