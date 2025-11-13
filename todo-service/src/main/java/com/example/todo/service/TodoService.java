package com.example.todo.service;

import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoService {
    private final TodoRepository repo;

    public TodoService(TodoRepository repo) { this.repo = repo; }

    public List<Todo> getTodos(String username) {
        return repo.findByUsername(username);
    }

    public Todo create(String username, String title) {
        Todo t = Todo.builder().username(username).title(title).completed(false).build();
        return repo.save(t);
    }

    public Todo toggle(Long id, String username) {
        Todo t = repo.findById(id).orElseThrow();
        if (!t.getUsername().equals(username)) throw new RuntimeException("Unauthorized");
        t.setCompleted(!t.isCompleted());
        return repo.save(t);
    }

    public void delete(Long id, String username) {
        Todo t = repo.findById(id).orElseThrow();
        if (!t.getUsername().equals(username)) throw new RuntimeException("Unauthorized");
        repo.delete(t);
    }
}
