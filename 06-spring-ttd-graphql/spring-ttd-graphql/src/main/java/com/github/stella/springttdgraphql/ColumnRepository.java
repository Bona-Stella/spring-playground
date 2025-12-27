package com.github.stella.springttdgraphql;

import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class ColumnRepository {
    private final List<Column> storage = new ArrayList<>();

    public ColumnRepository() {
        // 테스트 통과를 위한 초기 데이터
        // boardId "1"은 BoardRepository의 초기 데이터와 매칭됩니다.
        storage.add(new Column(UUID.randomUUID().toString(), "To Do", "1"));
        storage.add(new Column(UUID.randomUUID().toString(), "Doing", "1"));
        storage.add(new Column(UUID.randomUUID().toString(), "Done", "1"));
    }

    public List<Column> findByBoardId(String boardId) {
        // 메모리 리스트에서 boardId가 같은 것만 필터링해서 반환
        return storage.stream()
                .filter(column -> column.boardId().equals(boardId))
                .toList();
    }

    // 추후 사용을 위한 저장 메서드
    public Column save(String name, String boardId) {
        Column newColumn = new Column(UUID.randomUUID().toString(), name, boardId);
        storage.add(newColumn);
        return newColumn;
    }
}
