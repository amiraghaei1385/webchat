package models;

// Defines the type of a chat conversation.

public enum ChatType {
    // One-on-one private conversation between two users.
    PRIVATE,

    // Group conversation with multiple members.
    GROUP,

    // (Saved Messages).
    SAVED_MESSAGES
}