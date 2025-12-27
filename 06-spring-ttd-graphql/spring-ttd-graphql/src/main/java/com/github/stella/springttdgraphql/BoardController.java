package com.github.stella.springttdgraphql;

import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import java.util.List;

@Controller
public class BoardController {

    private final BoardRepository boardRepository;

    // 생성자 주입
    public BoardController(BoardRepository boardRepository) {
        this.boardRepository = boardRepository;
    }

    @QueryMapping
    public List<Board> boards() {
        return boardRepository.findAll();
    }
}