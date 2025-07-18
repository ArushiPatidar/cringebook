package com.cringebook.app;

import com.cringebook.app.entity.User;
import com.cringebook.app.entity.FriendRequest;
import com.cringebook.app.entity.Friendship;
import com.cringebook.app.repository.UserRepo;
import com.cringebook.app.repository.FriendRequestRepo;
import com.cringebook.app.repository.FriendshipRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class FriendshipFeatureTest {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private FriendRequestRepo friendRequestRepo;

    @Autowired
    private FriendshipRepo friendshipRepo;

    @Test
    void testFriendshipEntitiesCreation() {
        // Test basic entity creation without database operations
        User user1 = new User("testuser1", "password", "Test User 1", "test1@email.com", "1234567890");
        User user2 = new User("testuser2", "password", "Test User 2", "test2@email.com", "0987654321");
        
        assertNotNull(user1);
        assertNotNull(user2);
        assertEquals("testuser1", user1.getUserName());
        assertEquals("Test User 1", user1.getName());
        
        FriendRequest request = new FriendRequest(1, 2);
        assertNotNull(request);
        assertEquals("PENDING", request.getStatus());
        assertNotNull(request.getCreatedAt());
        
        Friendship friendship = new Friendship(1, 2);
        assertNotNull(friendship);
        assertNotNull(friendship.getCreatedAt());
    }
}