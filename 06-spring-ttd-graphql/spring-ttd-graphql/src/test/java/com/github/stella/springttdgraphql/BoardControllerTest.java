package com.github.stella.springttdgraphql;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.context.annotation.Import; // Import 추가
import org.springframework.graphql.test.tester.GraphQlTester;

@GraphQlTest(BoardController.class)
@Import({BoardRepository.class, ColumnRepository.class})
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

    @Test
    void shouldCreateBoard() {
        // when: 보드 생성 Mutation을 날린다
        this.graphQlTester.document("""
                    mutation {
                        createBoard(name: "My New Board") {
                            id
                            name
                        }
                    }
                """)
                .execute()
                .path("createBoard.name").entity(String.class).isEqualTo("My New Board") // 이름이 잘 들어갔는지
                .path("createBoard.id").hasValue(); // ID가 생성되었는지 (null이 아닌지)
    }

    @Test
    void shouldGetBoardWithColumns() {
        // when: 보드와 그 안의 컬럼을 같이 요청한다
        this.graphQlTester.document("""
                    query {
                        boards {
                            id
                            name
                            columns {
                                name
                            }
                        }
                    }
                """)
                .execute()
                .path("boards[0].columns")
                .entityList(Object.class) // ✅ 이 줄을 추가해야 "리스트"로 인식하고 사이즈를 챌 수 있습니다.
                .hasSize(3)
                .path("boards[0].columns[0].name").entity(String.class).isEqualTo("To Do");
    }
}