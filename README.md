# Movie Matcher – Swipe-Based Multiplayer Movie Picker

A web app where two (or more) users can enter a room and swipe through randomly selected TMDB movies. When multiple users swipe right on the same movie, it's a match! Perfect for couples or friends who can't decide what to watch.

## How It Works

- One player creates a room (a 4-character code is generated).
- The second player joins using the same code.
- Both players receive the **same randomized list of movies** (in different orders).
- Swiping "like" or "skip" progresses through the shared list.
- Every 5 swipes, a new batch of 5 movies is generated from TMDB using a seed system.
- When two players like the same movie → It’s a Match!
- The movie list is “endless” due to dynamic seed-based pagination.
  
## Tech Stack

- **Frontend**: Angular
- **Backend**: Spring Boot
- **Database**: PostgreSQL (for user, room, and swipe data)
- **Cache**: Redis (for TMDB data caching)
- **External API**: [TMDB API](https://www.themoviedb.org/documentation/api)

---

## Local Development

### Prerequisites

- Java 17+
- Node.js + npm
- PostgreSQL
- Redis
- TMDB API Key

## Development Roadmap
Feature                         | Status   | Notes                                        |
| ------------------------------- | -------- | -------------------------------------------- |
| TMDB fetch logic with seeds     | Done   | Using discover pages + index-based batching  |
| Database schema & persistence   | Done  | Store users, rooms, swipes                   |
| Swipe logging per player        | Done  | Persist liked/skipped state                  |
| Matching logic                  | Done  | Detect when 2+ users like the same movie     |
| Redis caching for TMDB results  | Next  | Required for deployment to prevent API abuse |
| Frontend match UI               | Next  | Show when both users like the same movie     |
| Multi-user support (>2 players) | Later | Expand room to handle N players              |
