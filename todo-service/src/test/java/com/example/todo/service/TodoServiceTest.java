package com.example.todo.service;

import com.example.todo.entity.Todo;
import com.example.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TodoServiceTest {

    @Mock
    private TodoRepository repo;

    @InjectMocks
    private TodoService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetTodos() {
        List<Todo> todos = List.of(new Todo(1L, "user", "Task 1", false));
        when(repo.findByUsername("user")).thenReturn(todos);

        List<Todo> result = service.getTodos("user");

        assertEquals(1, result.size());
        verify(repo).findByUsername("user");
    }

    @Test
    void testCreate() {
        Todo t = new Todo(1L, "user", "Task 1", false);
        when(repo.save(any(Todo.class))).thenReturn(t);

        Todo created = service.create("user", "Task 1");

        assertEquals("user", created.getUsername());
        assertEquals("Task 1", created.getTitle());
        assertFalse(created.isCompleted());
        verify(repo).save(any(Todo.class));
    }

    @Test
    void testToggle_Success() {
        Todo existing = new Todo(1L, "user", "Task 1", false);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(Todo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Todo result = service.toggle(1L, "user");

        assertTrue(result.isCompleted());
        verify(repo).save(existing);
    }

    @Test
    void testToggle_Unauthorized() {
        Todo existing = new Todo(1L, "other", "Task", false);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> service.toggle(1L, "user"));
    }

    @Test
    void testDelete_Success() {
        Todo existing = new Todo(1L, "user", "Task", false);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L, "user");

        verify(repo).delete(existing);
    }

    @Test
    void testDelete_Unauthorized() {
        Todo existing = new Todo(1L, "other", "Task", false);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));

        assertThrows(RuntimeException.class, () -> service.delete(1L, "user"));
    }

    @Test
    void testDelete_NotFound() {
        when(repo.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.delete(1L, "user"));
    }
}
