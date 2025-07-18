-- SQL queries for Friend/Connection Feature Database Schema

-- Create friend_requests table
CREATE TABLE friend_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    requester_id INT NOT NULL,
    recipient_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (requester_id) REFERENCES user_table(user_id),
    FOREIGN KEY (recipient_id) REFERENCES user_table(user_id),
    INDEX idx_recipient (recipient_id),
    INDEX idx_requester (requester_id),
    UNIQUE KEY unique_request (requester_id, recipient_id)
);

-- Create friendships table
CREATE TABLE friendships (
    friendship_id INT AUTO_INCREMENT PRIMARY KEY,
    user1_id INT NOT NULL,
    user2_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user1_id) REFERENCES user_table(user_id),
    FOREIGN KEY (user2_id) REFERENCES user_table(user_id),
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    UNIQUE KEY unique_friendship (user1_id, user2_id)
);

-- Add indexes to existing user_table for search optimization
CREATE INDEX idx_username ON user_table(username);
CREATE INDEX idx_name ON user_table(name);
CREATE INDEX idx_email ON user_table(email);

-- Sample queries for the friend system

-- Search users by name, username, or email
SELECT * FROM user_table 
WHERE username LIKE CONCAT('%', ?, '%') 
   OR name LIKE CONCAT('%', ?, '%') 
   OR email LIKE CONCAT('%', ?, '%');

-- Check if two users are friends
SELECT * FROM friendships 
WHERE (user1_id = ? AND user2_id = ?) 
   OR (user1_id = ? AND user2_id = ?);

-- Get pending friend requests for a user
SELECT fr.*, u.name, u.username, u.email 
FROM friend_requests fr
JOIN user_table u ON fr.requester_id = u.user_id
WHERE fr.recipient_id = ? AND fr.status = 'PENDING'
ORDER BY fr.created_at DESC;

-- Get all friends of a user
SELECT u.* FROM user_table u
JOIN friendships f ON (u.user_id = f.user1_id OR u.user_id = f.user2_id)
WHERE (f.user1_id = ? OR f.user2_id = ?) AND u.user_id != ?
ORDER BY u.name;

-- Check for existing friend request between two users
SELECT * FROM friend_requests 
WHERE (requester_id = ? AND recipient_id = ?) 
   OR (requester_id = ? AND recipient_id = ?);