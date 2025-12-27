package com.github.stella.springttdgraphql;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class BoardController {

    private final BoardRepository boardRepository;
    private final ColumnRepository columnRepository; // 추가

    // 생성자 주입 (파라미터 추가)
    public BoardController(BoardRepository boardRepository, ColumnRepository columnRepository) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
    }

    @QueryMapping
    public List<Board> boards() {
        return boardRepository.findAll();
    }

    @MutationMapping
    public Board createBoard(@Argument String name) {
        return boardRepository.save(name);
    }

    @SchemaMapping(typeName = "Board", field = "columns")
    public List<Column> getColumns(Board board) {
        // 하드코딩 제거 -> 리포지토리 호출
        return columnRepository.findByBoardId(board.id());
    }
}