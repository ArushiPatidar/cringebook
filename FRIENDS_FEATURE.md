# Friend/Connection Feature Documentation

## Overview
This feature introduces a user connection system (friends) with controlled profile visibility based on connection status.

## Database Schema

### New Tables

#### friend_requests
- `request_id` (Primary Key, Auto-increment)
- `requester_id` (Foreign Key to user_table.user_id)
- `recipient_id` (Foreign Key to user_table.user_id)
- `status` (VARCHAR: 'PENDING', 'ACCEPTED', 'REJECTED')
- `created_at` (TIMESTAMP)

#### friendships
- `friendship_id` (Primary Key, Auto-increment)
- `user1_id` (Foreign Key to user_table.user_id)
- `user2_id` (Foreign Key to user_table.user_id)
- `created_at` (TIMESTAMP)

## Backend APIs

### User Search & Profile APIs

#### GET /api/users/search?query={searchTerm}
- **Description**: Search users by name, username, or email
- **Headers**: Authorization: {jwtToken}
- **Response**: Array of User objects (excluding current user)

#### GET /api/users/profile/{userId}
- **Description**: View user profile with friendship status
- **Headers**: Authorization: {jwtToken}
- **Response**: 
```json
{
  "user": { User object },
  "areFriends": boolean,
  "hasPendingRequest": boolean,
  "canSendRequest": boolean
}
```

### Friend Request APIs

#### POST /api/friends/request
- **Description**: Send friend request
- **Headers**: Authorization: {jwtToken}
- **Body**: `recipientId={userId}`
- **Response**: Success/error message

#### POST /api/friends/accept
- **Description**: Accept friend request
- **Headers**: Authorization: {jwtToken}
- **Body**: `requestId={requestId}`
- **Response**: Success/error message

#### GET /api/friends/requests
- **Description**: Get pending friend requests for current user
- **Headers**: Authorization: {jwtToken}
- **Response**: Array of request objects with requester details

#### GET /api/friends/list
- **Description**: Get list of current user's friends
- **Headers**: Authorization: {jwtToken}
- **Response**: Array of User objects

## Frontend Pages

### search_users.html
- Search users by name, username, or email
- View search results with "View Profile" buttons
- Navigation back to home

### user_profile.html
- Display user profile information
- Show friendship status and appropriate action buttons
- Controlled access: full profile for friends, limited for non-friends
- Send friend request functionality

### friend_requests.html
- List pending friend requests
- Accept/decline functionality
- View requester profiles

### friends_list.html
- Display all friends
- Quick access to friend profiles

### Updated Navigation
The main memories page (show_memory.html) now includes:
- 🔍 Find Friends (search_users.html)
- 👥 Friends (friends_list.html)
- 📨 Requests (friend_requests.html)

## Privacy Controls

### Profile Access Control
- **Friends**: Can view full profile including email and phone
- **Non-friends**: Can only see name and username
- Profile viewing API automatically enforces these restrictions

### Memory Access Control
- Ready for future implementation: memories should only be visible to friends
- Framework is in place for controlled access based on friendship status

## Usage Flow

1. **Search for Users**: Use "Find Friends" to search by name, username, or email
2. **View Profiles**: Click on users to view their profiles
3. **Send Friend Request**: If not friends, send a friend request from their profile
4. **Accept Requests**: Check "Requests" page to see and accept pending requests
5. **View Friends**: Use "Friends" page to see all connections
6. **Controlled Access**: Friends can see full profiles, non-friends see limited info

## Technical Implementation Details

### Authentication
- All APIs require JWT token in Authorization header
- Uses existing authentication system from the app

### Database Queries
- Optimized queries with proper indexing on user_id fields
- Native SQL queries for performance
- CONCAT function used for cross-database compatibility

### Frontend Integration
- Uses existing HTML/CSS/JS structure
- Follows same styling patterns as existing pages
- Error handling and loading states included
- Responsive design maintained

## Testing

### Unit Tests
- Entity creation and validation tests
- Uses H2 in-memory database for testing
- Verifies table creation and basic functionality

### Manual Testing Flow
1. Create multiple user accounts
2. Search for users
3. Send friend requests
4. Accept/decline requests
5. Verify profile access controls
6. Test navigation between pages