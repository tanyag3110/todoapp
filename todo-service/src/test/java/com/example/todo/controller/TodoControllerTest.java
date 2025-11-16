package com.example.todo.controller;

import com.example.todo.entity.Todo;
import com.example.todo.service.TodoService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoControllerTest {

    @Mock
    private TodoService service;

    @Mock
    private HttpServletRequest req;

    @InjectMocks
    private TodoController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(req.getAttribute("username")).thenReturn("user1");
    }

    @Test
    void testList() {
        List<Todo> todos = List.of(new Todo(1L, "user1", "Test", false));
        when(service.getTodos("user1")).thenReturn(todos);

        List<Todo> result = controller.list(req);

        assertEquals(1, result.size());
        verify(service).getTodos("user1");
    }

    @Test
    void testCreate() {
        Map<String, String> body = Map.of("title", "Task A");
        Todo saved = new Todo(1L, "user1", "Task A", false);
        when(service.create("user1", "Task A")).thenReturn(saved);

        Todo result = controller.create(req, body);

        assertEquals("Task A", result.getTitle());
        verify(service).create("user1", "Task A");
    }

    @Test
    void testToggle() {
        Todo toggled = new Todo(1L, "user1", "Task", true);
        when(service.toggle(1L, "user1")).thenReturn(toggled);

        Todo result = controller.toggle(req, 1L);

        assertTrue(result.isCompleted());
        verify(service).toggle(1L, "user1");
    }

    @Test
    void testDelete() {
        controller.delete(req, 2L);
        verify(service).delete(2L, "user1");
    }
}
