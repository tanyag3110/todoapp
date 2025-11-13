package com.example.todo.controller;

import com.example.todo.entity.Todo;
import com.example.todo.service.TodoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/todos")
@CrossOrigin(origins = "*")
@Slf4j
public class TodoController {

    @Autowired
    private TodoService service;

    private String username(HttpServletRequest req) {
        return (String) req.getAttribute("username");
    }

    @GetMapping
    public List<Todo> list(HttpServletRequest req) {
        String user = username(req);
        log.info("GET /api/todos for user: {}", user);
        return service.getTodos(user);
    }

    @PostMapping
    public Todo create(HttpServletRequest req, @RequestBody Map<String, String> body) {
        String user = username(req);
        String title = body.get("title");
        log.info("POST /api/todos for user: {}, title: {}", user, title);
        return service.create(user, title);
    }

    @PutMapping("/{id}")
    public Todo toggle(HttpServletRequest req, @PathVariable Long id) {
        String user = username(req);
        log.info("PUT /api/todos/{} for user: {}", id, user);
        return service.toggle(id, user);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest req, @PathVariable Long id) {
        String user = username(req);
        log.info("DELETE /api/todos/{} for user: {}", id, user);
        service.delete(id, user);
    }
}
