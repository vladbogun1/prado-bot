package com.bogun.prado_bot.discord.board;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BoardViewRegistry {

    private final Map<String, BoardView> map;

    public BoardViewRegistry(List<BoardView> views) {
        this.map = views.stream().collect(Collectors.toUnmodifiableMap(BoardView::key, Function.identity()));
    }

    public BoardView get(String key) {
        var v = map.get(key);
        if (v == null) throw new IllegalArgumentException("Unknown board view: " + key);
        return v;
    }
}
