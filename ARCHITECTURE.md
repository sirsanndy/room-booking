# Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Vue.js 3)                     │
│                        http://localhost:5173                     │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │  Login.vue   │  │  Rooms.vue   │  │MyBookings.vue│          │
│  │  Signup.vue  │  │RoomDetail.vue│  │              │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                  │                  │                  │
│         └──────────────────┴──────────────────┘                  │
│                            │                                     │
│                    ┌───────▼───────┐                            │
│                    │  Vue Router   │                            │
│                    └───────┬───────┘                            │
│                            │                                     │
│         ┌──────────────────┼──────────────────┐                 │
│         │                  │                  │                 │
│  ┌──────▼──────┐  ┌────────▼────────┐  ┌─────▼─────┐          │
│  │ Auth Store  │  │  Room Store     │  │ Booking   │          │
│  │  (Pinia)    │  │   (Pinia)       │  │  Store    │          │
│  └──────┬──────┘  └────────┬────────┘  └─────┬─────┘          │
│         │                  │                  │                 │
│         └──────────────────┴──────────────────┘                 │
│                            │                                     │
│                    ┌───────▼───────┐                            │
│                    │  API Service  │                            │
│                    │    (Axios)    │                            │
│                    └───────┬───────┘                            │
└────────────────────────────┼────────────────────────────────────┘
                             │ HTTP + JWT
                             │ (REST API)
┌────────────────────────────▼────────────────────────────────────┐
│                     BACKEND (Spring Boot 3)                      │
│                      http://localhost:8080                       │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Security Layer                         │  │
│  │  ┌────────────────┐  ┌──────────────────────────────┐    │  │
│  │  │JWT Auth Filter │→ │ CustomUserDetailsService     │    │  │
│  │  └────────────────┘  └──────────────────────────────┘    │  │
│  └──────────────────────────────────────────────────────────┘  │
│                             │                                   │
│  ┌──────────────────────────▼──────────────────────────────┐  │
│  │                    Controllers                            │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │  │
│  │  │    Auth      │ │  MeetingRoom │ │   Booking    │     │  │
│  │  │  Controller  │ │  Controller  │ │  Controller  │     │  │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │  │
│  └─────────┼────────────────┼────────────────┼─────────────┘  │
│            │                │                │                 │
│  ┌─────────▼────────────────▼────────────────▼─────────────┐  │
│  │                      Services                            │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │  │
│  │  │ AuthService  │ │RoomService   │ │BookingService│     │  │
│  │  │              │ │              │ │ (w/ Locking) │     │  │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │  │
│  └─────────┼────────────────┼────────────────┼─────────────┘  │
│            │                │                │                 │
│  ┌─────────▼────────────────▼────────────────▼─────────────┐  │
│  │                    Repositories                          │  │
│  │  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐     │  │
│  │  │    User      │ │ MeetingRoom  │ │   Booking    │     │  │
│  │  │  Repository  │ │  Repository  │ │  Repository  │     │  │
│  │  │              │ │(w/ Lock)     │ │(w/ Lock)     │     │  │
│  │  └──────┬───────┘ └──────┬───────┘ └──────┬───────┘     │  │
│  └─────────┼────────────────┼────────────────┼─────────────┘  │
│            │                │                │                 │
│  ┌─────────▼────────────────▼────────────────▼─────────────┐  │
│  │              Spring Data JPA / Hibernate                 │  │
│  └──────────────────────────┬───────────────────────────────┘  │
└─────────────────────────────┼──────────────────────────────────┘
                              │ JDBC
┌─────────────────────────────▼──────────────────────────────────┐
│                    DATABASE (PostgreSQL 15)                     │
│                      localhost:5432                             │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────────┐  ┌──────────┐                  │
│  │  users   │  │meeting_rooms │  │ bookings │                  │
│  │          │  │              │  │          │                  │
│  │ • id     │  │ • id         │  │ • id     │                  │
│  │ • username  │ • name       │  │ • room_id│  ←─────┐         │
│  │ • password  │ • capacity   │  │ • user_id│  ←─┐   │         │
│  │ • email  │  │ • location   │  │ • start  │    │   │         │
│  │ • roles  │  │ • available  │  │ • end    │    │   │         │
│  │          │  │ • version    │  │ • status │    │   │         │
│  │          │  │              │  │ • version│    │   │         │
│  └──────────┘  └──────────────┘  └──────────┘    │   │         │
│       ▲                                           │   │         │
│       └───────────────────────────────────────────┘   │         │
│                                                       │         │
│  Indexes:                                             │         │
│  • idx_room_time (room_id, start_time, end_time)     │         │
│  • idx_user_booking (user_id, start_time) ────────────┘         │
│                                                                 │
│  Locking Strategy:                                              │
│  • PESSIMISTIC_WRITE on meeting_rooms and bookings             │
│  • SERIALIZABLE transaction isolation                          │
│  • Optimistic locking via @Version field                       │
└─────────────────────────────────────────────────────────────────┘
```

## Race Condition Prevention Flow

```
User 1                          User 2                          Database
  │                               │                                 │
  ├─ Book Room A (2-3pm) ─────────┤                                 │
  │                               ├─ Book Room A (2-3pm)            │
  │                               │                                 │
  ▼                               ▼                                 ▼
┌─────────────────────┐   ┌─────────────────────┐         ┌──────────────┐
│ Transaction 1 START │   │ Transaction 2 START │         │              │
└─────────┬───────────┘   └──────────┬──────────┘         │              │
          │                          │                     │              │
          ├─ SELECT ... FOR UPDATE ──┼─────────────────────┤              │
          │                          │                     │  LOCK Room A │
          │ ✅ Lock acquired         │                     │              │
          │                          │                     │              │
          │                          ├─ SELECT ... FOR UPDATE             │
          │                          │                     │              │
          │                          │ ⏳ WAITING...       │              │
          │                          │                     │              │
          ├─ Check overlaps          │                     │              │
          │ ✅ No conflicts          │                     │              │
          │                          │                     │              │
          ├─ INSERT booking          │                     │              │
          │ ✅ Booking created       │                     │              │
          │                          │                     │              │
          ├─ COMMIT ─────────────────┼─────────────────────┤              │
          │                          │                     │  UNLOCK      │
          │ ✅ Transaction complete  │                     │              │
          │                          │                     │              │
          │                          │ ✅ Lock acquired    │  LOCK Room A │
          │                          │                     │              │
          │                          ├─ Check overlaps     │              │
          │                          │ ❌ CONFLICT FOUND!  │              │
          │                          │                     │              │
          │                          ├─ ROLLBACK           │              │
          │                          │                     │  UNLOCK      │
          │                          │ ❌ Error: Already   │              │
          │                          │    booked           │              │
          ▼                          ▼                     ▼              │
    ✅ SUCCESS                  ❌ FAILURE                                │
```

## Security Flow

```
┌──────────────┐
│   Browser    │
└──────┬───────┘
       │ 1. POST /api/auth/signin
       │    { username, password }
       ▼
┌──────────────────────────┐
│   AuthController         │
└──────┬───────────────────┘
       │ 2. Authenticate
       ▼
┌──────────────────────────┐
│  AuthenticationManager   │
└──────┬───────────────────┘
       │ 3. Load user
       ▼
┌──────────────────────────┐
│CustomUserDetailsService  │
└──────┬───────────────────┘
       │ 4. Verify password
       │    (BCrypt)
       ▼
┌──────────────────────────┐
│   JwtTokenProvider       │
│   Generate JWT           │
└──────┬───────────────────┘
       │ 5. Return token
       ▼
┌──────────────┐
│   Browser    │
│ Store token  │
└──────┬───────┘
       │ 6. Subsequent requests
       │    Authorization: Bearer <token>
       ▼
┌──────────────────────────┐
│ JwtAuthenticationFilter  │
│  • Extract token         │
│  • Validate signature    │
│  • Check expiration      │
└──────┬───────────────────┘
       │ 7. Set SecurityContext
       ▼
┌──────────────────────────┐
│   Controllers            │
│   (Protected endpoints)  │
└──────────────────────────┘
```

## Data Flow - Creating a Booking

```
┌─────────────┐
│   Vue UI    │ User fills form
└──────┬──────┘
       │ BookingRequest
       ▼
┌─────────────┐
│BookingStore │ Pinia state management
└──────┬──────┘
       │ HTTP POST
       ▼
┌─────────────┐
│ API Service │ Axios + JWT header
└──────┬──────┘
       │ REST API
       ▼
┌─────────────────────┐
│BookingController    │ @PostMapping
└──────┬──────────────┘
       │ Validate & extract user
       ▼
┌─────────────────────┐
│BookingService       │ @Transactional(SERIALIZABLE)
│                     │
│ 1. Validate times   │ ✅ Check start < end
│ 2. Get user         │ ✅ Load from DB
│ 3. Lock room        │ 🔒 PESSIMISTIC_WRITE
│ 4. Check overlaps   │ 🔒 Lock overlapping bookings
│ 5. Create booking   │ ✅ If no conflicts
│ 6. Save to DB       │ 💾 Persist
└──────┬──────────────┘
       │ BookingResponse
       ▼
┌─────────────┐
│   Vue UI    │ Show success/error
└─────────────┘
```

## Technologies & Versions

```
┌─────────────────────────────────────────────┐
│            Frontend Stack                    │
├─────────────────────────────────────────────┤
│ Vue.js 3.4.0         │ Reactive framework   │
│ TypeScript 5.3.3     │ Type safety          │
│ Pinia 2.1.7          │ State management     │
│ Vue Router 4.2.5     │ Navigation           │
│ Axios 1.6.2          │ HTTP client          │
│ Vite 5.0.8           │ Build tool           │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│            Backend Stack                     │
├─────────────────────────────────────────────┤
│ Java 17              │ Language             │
│ Spring Boot 3.2.0    │ Framework            │
│ Spring Security      │ Authentication       │
│ Spring Data JPA      │ ORM                  │
│ Hibernate            │ JPA implementation   │
│ JWT 0.12.3           │ Token-based auth     │
│ BCrypt               │ Password hashing     │
│ Maven 3.8+           │ Build tool           │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│         Infrastructure Stack                 │
├─────────────────────────────────────────────┤
│ PostgreSQL 15        │ Database             │
│ Docker               │ Containerization     │
│ Docker Compose       │ Multi-container      │
└─────────────────────────────────────────────┘
```

## File Organization

```
softwareseni/
│
├── .github/
│   └── copilot-instructions.md    # AI assistant context
│
├── backend/                        # Spring Boot Application
│   ├── src/main/
│   │   ├── java/com/meetingroom/booking/
│   │   │   ├── BookingApplication.java      # Main entry point
│   │   │   ├── config/                      # Configuration classes
│   │   │   │   ├── SecurityConfig.java      # JWT, CORS, auth
│   │   │   │   └── DataInitializer.java     # Sample data loader
│   │   │   ├── controller/                  # REST endpoints
│   │   │   │   ├── AuthController.java      # /api/auth/*
│   │   │   │   ├── MeetingRoomController.java  # /api/rooms/*
│   │   │   │   └── BookingController.java   # /api/bookings/*
│   │   │   ├── dto/                         # Request/Response objects
│   │   │   ├── entity/                      # JPA Entities
│   │   │   │   ├── User.java               # User table
│   │   │   │   ├── MeetingRoom.java        # Rooms table
│   │   │   │   └── Booking.java            # Bookings table
│   │   │   ├── repository/                  # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── MeetingRoomRepository.java  # w/ locking
│   │   │   │   └── BookingRepository.java   # w/ overlap queries
│   │   │   ├── security/                    # Auth components
│   │   │   │   ├── JwtTokenProvider.java    # Token ops
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── CustomUserDetailsService.java
│   │   │   ├── service/                     # Business logic
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── MeetingRoomService.java
│   │   │   │   └── BookingService.java      # Race condition handling
│   │   │   └── exception/
│   │   │       └── GlobalExceptionHandler.java
│   │   └── resources/
│   │       └── application.properties       # Config
│   └── pom.xml                              # Dependencies
│
├── frontend/                        # Vue.js Application
│   ├── src/
│   │   ├── views/                          # Page components
│   │   │   ├── Login.vue
│   │   │   ├── Signup.vue
│   │   │   ├── Rooms.vue
│   │   │   ├── RoomDetail.vue
│   │   │   └── MyBookings.vue
│   │   ├── stores/                         # Pinia state
│   │   │   ├── auth.ts                     # Authentication
│   │   │   ├── room.ts                     # Room data
│   │   │   └── booking.ts                  # Booking data
│   │   ├── services/
│   │   │   └── api.ts                      # Axios HTTP client
│   │   ├── types/
│   │   │   └── index.ts                    # TypeScript types
│   │   ├── router/
│   │   │   └── index.ts                    # Vue Router
│   │   ├── App.vue                         # Root component
│   │   ├── main.ts                         # Entry point
│   │   └── style.css                       # Global styles
│   ├── index.html
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── docker-compose.yml               # PostgreSQL container
├── README.md                        # Full documentation
├── QUICKSTART.md                    # Quick start guide
├── SUMMARY.md                       # Project summary
├── TESTING.md                       # Test checklist
└── .gitignore
```
