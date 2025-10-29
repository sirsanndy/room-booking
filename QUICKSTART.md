# Quick Start Guide

## 🚀 Getting Started

### Step 1: Start the Database

Start PostgreSQL using Docker:
```bash
docker-compose up -d
```

Wait a few seconds for PostgreSQL to initialize.

### Step 2: Start the Backend

Open a new terminal and run:
```bash
cd backend
./mvnw spring-boot:run
```

Wait until you see: "Started BookingApplication in X seconds"

### Step 3: Start the Frontend

Open another terminal and run:
```bash
cd frontend
npm run dev
```

### Step 4: Access the Application

Open your browser and navigate to: **http://localhost:5173**

## 👤 Login Credentials

Use these demo accounts:
- Username: `john` | Password: `password123`
- Username: `jane` | Password: `password123`

Or create a new account by clicking "Sign up"

## 🧪 Testing Race Condition Prevention

To test that the system prevents race conditions:

1. Open **two different browser windows** (or use incognito mode for the second)
2. Login as `john` in the first window
3. Login as `jane` in the second window
4. In both windows, navigate to the same meeting room
5. Try to book the **same time slot** in both windows **simultaneously**
6. **Result**: Only ONE booking will succeed, the other will get an error message "Room is already booked for the selected time period"

This demonstrates the pessimistic locking preventing double bookings!

## 📝 Features to Test

1. **View Rooms**: Browse available meeting rooms
2. **Book a Room**: Select a room and create a booking
3. **View Bookings**: Check your bookings on "My Bookings" page
4. **Cancel Booking**: Cancel a future booking
5. **Overlap Prevention**: Try to book a room that's already booked - it will be rejected
6. **Concurrent Booking**: Test with two users simultaneously (see above)

## 🛑 Stopping the Application

1. Stop frontend: Press `Ctrl+C` in the frontend terminal
2. Stop backend: Press `Ctrl+C` in the backend terminal
3. Stop database: `docker-compose down`

## 🔧 Troubleshooting

### Backend won't start
- Make sure PostgreSQL is running: `docker ps`
- Check if port 8080 is available

### Frontend won't start
- Make sure port 5173 is available
- Run `npm install` in the frontend directory if needed

### Database connection error
- Ensure Docker is running
- Run `docker-compose down` then `docker-compose up -d`

## 📚 API Documentation

The backend exposes REST APIs at `http://localhost:8080/api/`

See README.md for complete API endpoint documentation.
