package com.github.stella.springttdgraphql;

import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BoardRepository {
    // DB 대신 사용할 메모리 저장소
    private final List<Board> storage = new ArrayList<>();

    public BoardRepository() {
        // 초기 데이터 세팅 (나중엔 삭제 예정)
        storage.add(new Board("1", "TDD Board"));
    }

    public List<Board> findAll() {
        return new ArrayList<>(storage);
    }

    // ▼ 추가된 메서드
    public Board save(String name) {
        String id = UUID.randomUUID().toString(); // 랜덤 ID 생성
        Board newBoard = new Board(id, name);
        storage.add(newBoard);
        return newBoard;
    }
}