# Meeting Room Booking Application

A full-stack web application for booking meeting rooms with concurrency control and security features.

## Features

- 🔐 **User Authentication & Authorization** - JWT-based secure authentication
- � **Dashboard with Calendar View** - Google Calendar-style interface showing bookings and holidays
- �📅 **Room Booking System** - Book meeting rooms for specific time periods
- 🚫 **Overlap Prevention** - Prevents double-booking of rooms
- ⚡ **Race Condition Prevention** - Pessimistic locking ensures only one user can book at a time
- 🕐 **Time Restrictions** - Bookings limited to 7am-10pm on weekdays only
- � **Holiday Management** - Automatic blocking of bookings on holidays
- �🏢 **Room Management** - View available meeting rooms with details
- � **Booking Management** - View and cancel your bookings
- 📈 **Analytics & Statistics** - View booking trends and most popular rooms
- 🎨 **Modern UI** - Responsive Vue.js interface with FullCalendar integration

## Technology Stack

### Backend
- **Java 17**
- **Spring Boot 3.3.0**
- **Spring Security** with JWT
- **Spring Data JPA** with Hibernate
- **PostgreSQL** database
- **Maven** build tool

### Frontend
- **Vue.js 3** with TypeScript
- **Vue Router** for navigation
- **Pinia** for state management
- **Axios** for API calls
- **Vite** build tool

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- PostgreSQL 15 or higher (or Docker)
- Maven 3.8 or higher

## Getting Started

### 1. Start PostgreSQL Database

Using Docker (recommended):
```bash
docker-compose up -d
```

Or install PostgreSQL manually and create a database named `meetingroom_db`.

### 2. Run Backend

```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`

## Default Credentials

The application comes with sample users:

- Username: `john` | Password: `password123`
- Username: `jane` | Password: `password123`

## API Endpoints

### Authentication
- `POST /api/auth/signup` - Register a new user
- `POST /api/auth/signin` - Login

### Dashboard
- `GET /api/dashboard` - Get dashboard data with calendar events and statistics

### Meeting Rooms
- `GET /api/rooms` - Get all rooms
- `GET /api/rooms/available` - Get available rooms
- `GET /api/rooms/{id}` - Get room by ID

### Bookings
- `POST /api/bookings` - Create a new booking (with time restrictions validation)
- `GET /api/bookings/my-bookings` - Get user's bookings
- `GET /api/bookings/room/{roomId}` - Get bookings for a room
- `DELETE /api/bookings/{id}` - Cancel a booking
- `GET /api/bookings/check-availability` - Check room availability

## Booking Time Restrictions

The application enforces the following time restrictions for bookings:

- **Business Hours**: 7:00 AM - 10:00 PM only
- **Weekdays Only**: Monday to Friday (no weekend bookings)
- **Holiday Blocking**: No bookings allowed on official holidays
- **Single Day**: Cannot book multiple days in one booking

These restrictions are validated both on the frontend and backend for security.

## Race Condition Prevention

The application uses **pessimistic locking** at the database level to prevent race conditions:

1. When a user attempts to book a room, the system acquires a `PESSIMISTIC_WRITE` lock on the room record
2. It then checks for overlapping bookings with another lock
3. Only if no overlaps exist, the booking is created
4. The transaction uses `SERIALIZABLE` isolation level for maximum consistency

This ensures that even if multiple users try to book the same room simultaneously, only one will succeed.

## Project Structure

```
softwareseni/
├── backend/
│   ├── src/main/java/com/meetingroom/booking/
│   │   ├── config/          # Security and app configuration
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data repositories
│   │   ├── security/        # JWT and security components
│   │   └── service/         # Business logic
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── components/      # Vue components
│   │   ├── views/           # Page views
│   │   ├── stores/          # Pinia stores
│   │   ├── services/        # API services
│   │   ├── types/           # TypeScript types
│   │   └── router/          # Vue Router config
│   └── package.json
└── docker-compose.yml

```

## Security Features

- JWT-based authentication
- BCrypt password hashing
- CORS configuration
- Stateless session management
- Role-based access control
- SQL injection prevention via JPA
- XSS protection

## Database Schema

### Users
- id, username, password, email, full_name, roles, enabled, created_at, updated_at

### Meeting Rooms
- id, name, capacity, description, location, available, facilities, created_at, updated_at, version

### Bookings
- id, room_id, user_id, start_time, end_time, title, description, status, created_at, updated_at, version

## Development

### Backend Development
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend Development
```bash
cd frontend
npm run dev
```

### Build for Production

Backend:
```bash
cd backend
./mvnw clean package
java -jar target/booking-backend-1.0.0.jar
```

Frontend:
```bash
cd frontend
npm run build
```

## Testing

Test the race condition prevention:
1. Open two browser windows
2. Login as different users
3. Try to book the same room for overlapping times
4. Only one booking should succeed

## License

This project is licensed under the MIT License.

## Author

Created for Software Seni technical assessment.
