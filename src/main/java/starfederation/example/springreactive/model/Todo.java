package starfederation.example.springreactive.model;

import org.springframework.data.annotation.Id;
import java.util.UUID;

public class Todo {
    @Id
    private int id;
    private String description;
    private boolean completed;
    private String username;

    // Constructors
    public Todo() {}

    public Todo(int id, String description, boolean completed, String username) {
        this.id = id;
        this.description = description;
        this.completed = completed;
        this.username = username;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}