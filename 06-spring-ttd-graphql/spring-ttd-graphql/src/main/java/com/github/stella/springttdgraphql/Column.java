package com.github.stella.springttdgraphql;

public record Column(String id, String name, String boardId) {
    // boardId는 어떤 보드에 속하는지 알기 위해 필요
}
