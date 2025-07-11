# Event Filtering System Documentation

## Overview

The event filtering system has been significantly improved to provide comprehensive, secure, and flexible filtering capabilities for the Event Management API.

## Key Improvements

### 1. New Controller Endpoint
- **Endpoint**: `GET /events`
- **Purpose**: Filter events with multiple query parameters
- **Access**: Public (with authentication-aware results)

### 2. Query Parameters

| Parameter | Type | Required | Description | Example |
|-----------|------|----------|-------------|---------|
| `location` | String | No | Case-insensitive partial match for event location | `?location=new york` |
| `startDate` | DateTime | No | Events starting on or after this date | `?startDate=2024-12-01T00:00:00` |
| `endDate` | DateTime | No | Events ending on or before this date | `?endDate=2024-12-31T23:59:59` |
| `visibility` | String | No | Filter by visibility (PUBLIC/PRIVATE) | `?visibility=PUBLIC` |
| `page` | Int | No | Page number (0-based) | `?page=0` |
| `size` | Int | No | Page size (default: 20) | `?size=10` |
| `sort` | String | No | Sort field and direction | `?sort=startTime,desc` |

### 3. Authentication-Aware Visibility

#### Unauthenticated Users
- Can only see **PUBLIC** events
- All filtering applies only to public events

#### Regular Authenticated Users
- Can see **PUBLIC** events
- Can see **PRIVATE** events they host
- Filtering respects user permissions

#### Admin Users
- Can see **ALL** events regardless of visibility
- Can filter by any visibility level
- Full administrative access

### 4. Example API Calls

```bash
# Get all public events
GET /events

# Get events in New York starting after today
GET /events?location=new%20york&startDate=2024-07-10T00:00:00

# Get private events (authenticated users only see their own)
GET /events?visibility=PRIVATE

# Get events with pagination and sorting
GET /events?page=0&size=10&sort=startTime,desc

# Complex filtering
GET /events?location=conference&startDate=2024-08-01T00:00:00&endDate=2024-08-31T23:59:59&visibility=PUBLIC
```

### 5. Response Format

```json
{
  "content": [
    {
      "id": "uuid",
      "title": "Event Title",
      "description": "Event Description",
      "startTime": "2024-07-15T14:00:00",
      "endTime": "2024-07-15T16:00:00",
      "location": "Event Location",
      "visibility": "PUBLIC",
      "hostId": "host-uuid",
      "hostName": "Host Name",
      "attendeeCount": 25
    }
  ],
  "pageable": {
    "sort": {
      "sorted": true,
      "unsorted": false
    },
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5,
  "last": false,
  "first": true,
  "numberOfElements": 20
}
```

## Security Features

### 1. Soft Delete Protection
- Only non-deleted events are returned
- Hard requirement in all queries

### 2. Role-Based Access Control
- **Public Access**: Only public events
- **User Access**: Public + owned private events
- **Admin Access**: All events

### 3. Parameter Validation
- Invalid visibility values are ignored
- Malformed dates return proper error responses
- SQL injection protection through JPA Criteria API

## Implementation Details

### Specification Pattern
The filtering uses Spring Data JPA's `Specification` pattern for:
- **Type Safety**: Compile-time query validation
- **Composability**: Easy combination of multiple filters
- **Security**: Protection against SQL injection
- **Performance**: Efficient database queries with proper indexing

### Database Indexes
Optimized queries with indexes on:
- `start_time` - For date range filtering
- `location` - For location searching
- `visibility` - For visibility filtering
- `deleted` - For soft delete filtering
- `host_id` - For ownership checks

## Error Handling

### Invalid Parameters
- **Invalid visibility**: Ignored, defaults to user's accessible events
- **Invalid date format**: HTTP 400 Bad Request
- **Missing required auth**: Works as public access

### Common Error Responses

```json
// Invalid date format
{
  "error": "Bad Request",
  "message": "Invalid date format. Use ISO 8601 format: yyyy-MM-ddTHH:mm:ss",
  "timestamp": "2024-07-10T10:30:00Z"
}

// Access denied for specific event
{
  "error": "Unauthorized",
  "message": "You don't have permission to view this event",
  "timestamp": "2024-07-10T10:30:00Z"
}
```

## Testing

### Test Scenarios
1. **Public Access Tests**
   - Filter public events only
   - Verify private events are hidden

2. **Authenticated User Tests**
   - See own private events
   - Filter by all parameters
   - Verify other users' private events are hidden

3. **Admin Tests**
   - Access all events
   - Filter by any visibility
   - Verify admin override works

4. **Edge Cases**
   - Empty result sets
   - Invalid parameter handling
   - Boundary date conditions

## Performance Considerations

### Caching Strategy
- **Cacheable**: Public upcoming events (high read, low write)
- **Cache Eviction**: On event create/update/delete
- **Cache Keys**: Include filter parameters for proper isolation

### Query Optimization
- **Indexed Columns**: All filterable fields are indexed
- **Lazy Loading**: Related entities loaded only when needed
- **Pagination**: Prevents large result sets

### Monitoring Recommendations
- Track query execution times
- Monitor cache hit rates
- Log slow queries for optimization

## Migration Notes

### Breaking Changes
- None - new endpoint added alongside existing ones

### New Features
- Enhanced filtering capabilities
- Authentication-aware results
- Improved security model

### Backward Compatibility
- All existing endpoints remain unchanged
- Existing client code continues to work
- New features available through new endpoint

## Usage Examples

### Frontend Integration
```javascript
// Basic event search
const events = await fetch('/events?location=conference&size=10');

// Date range filtering
const upcomingEvents = await fetch(
  `/events?startDate=${new Date().toISOString()}&size=20`
);

// Authenticated private event access
const myEvents = await fetch('/events?visibility=PRIVATE', {
  headers: { 'Authorization': `Bearer ${token}` }
});
```

### API Client Integration
```java
// Spring WebClient example
WebClient.builder()
  .baseUrl("https://api.eventmanager.com")
  .build()
  .get()
  .uri(uriBuilder -> uriBuilder
    .path("/events")
    .queryParam("location", "new york")
    .queryParam("startDate", LocalDateTime.now())
    .queryParam("size", 20)
    .build())
  .retrieve()
  .bodyToMono(PagedEventResponse.class);
```
