package com.github.stella.springttdgraphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import; // Import 추가
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(BoardController.class)
@Import(BoardRepository.class) // ✅ 핵심: 리포지토리도 테스트 문맥에 포함시킨다!
class BoardControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Test
    void shouldGetBoards() {
        // ... 기존 코드 유지 ...
        this.graphQlTester.document("""
                    query {
                        boards {
                            id
                            name
                        }
                    }
                """)
                .execute()
                .path("boards")
                .entityList(Board.class)
                .hasSize(1)
                .contains(new Board("1", "TDD Board"));
    }
}