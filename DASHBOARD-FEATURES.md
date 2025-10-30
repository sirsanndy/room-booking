# Dashboard Features Documentation

## Overview
The Dashboard is now fully interactive with calendar-based booking and detailed event viewing capabilities. Users can create bookings directly from the calendar and view comprehensive booking details in a sidebar.

## New Features

### 1. Calendar Click-to-Book 📅

#### How It Works
- **Click any date or time slot** in the calendar to create a new booking
- The booking modal opens with pre-filled date and time based on where you clicked
- Works in all calendar views: Month, Week, and Day

#### Smart Pre-filling
- **Month View**: Date is pre-filled, default time 9 AM - 10 AM
- **Week/Day View**: Both date and time are pre-filled based on clicked slot
- Time slots are hourly (7 AM - 10 PM)

#### Automatic Validations
The system prevents invalid bookings with instant feedback:
- ❌ **Past dates**: "Cannot book dates in the past"
- ❌ **Weekends**: "Cannot book on weekends (Saturday & Sunday)"
- ❌ **Holidays**: "Cannot book on holidays"
- ❌ **Invalid time range**: "End time must be after start time"
- ❌ **Outside business hours**: "Start time must be between 7:00 AM and 10:00 PM"
- ❌ **Daily limit exceeded**: "You have exceeded the daily booking limit of 9 hours"
- ❌ **Double booking**: "You already have a booking in another room during this time"

### 2. Event Detail Sidebar 📋

#### How to Open
- **Click any event** (booking or holiday) on the calendar
- A sidebar slides in from the right showing full details
- Works for both your bookings and holiday events

#### Booking Details Shown
- 📅 **Full Date**: "Monday, October 28, 2025"
- 🕐 **Time Range**: "09:00 AM - 11:00 AM"
- ⏱️ **Duration**: "2 hours" or "1h 30m"
- 📍 **Room Name**: "Conference Room A"
- ✓ **Status**: Badge showing CONFIRMED/CANCELLED/PENDING
- 📄 **Description**: Full booking description if available

#### Available Actions
For booking events:
- **View Room Details**: Navigate to room detail page
- **Cancel Booking**: Cancel with confirmation dialog

For holiday events:
- Shows holiday notice explaining no bookings allowed

### 3. Enhanced Calendar Interface

#### Multiple Views
- **Month View** (dayGridMonth): Overview of entire month
- **Week View** (timeGridWeek): Hourly slots for current week
- **Day View** (timeGridDay): Detailed hourly view for single day

#### Visual Indicators
- 🟦 **Blue Events**: Your confirmed bookings
- 🟥 **Red Events**: Holidays (no booking allowed)
- 🟪 **Gray Background**: Weekends (no booking allowed)
- **Business Hours**: Visible slots between 7 AM - 10 PM
- **Now Indicator**: Red line showing current time (in week/day view)

#### Interactive Features
- Click events to view details
- Click empty slots to create bookings
- Drag to see more details
- Responsive on all screen sizes

## Booking Modal

### Form Fields

#### Required Fields
1. **Meeting Room** ⭐
   - Dropdown of available rooms
   - Shows capacity for each room
   
2. **Date** ⭐
   - Date picker
   - Cannot select dates in the past
   - Pre-filled when clicking calendar
   
3. **Start Time** ⭐
   - Time picker with hourly slots
   - Range: 07:00 - 22:00
   - Pre-filled in week/day view
   
4. **End Time** ⭐
   - Time picker with hourly slots
   - Range: 07:00 - 22:00
   - Must be after start time
   
5. **Title** ⭐
   - Text input
   - Example: "Team Meeting", "Client Presentation"

#### Optional Fields
1. **Description**
   - Multi-line text area
   - Additional details about the meeting

### Validation Rules

#### Client-Side Validation
- All required fields must be filled
- End time must be after start time
- Time range must be within business hours (7 AM - 10 PM)

#### Server-Side Validation
- Weekday only (Monday - Friday)
- Not on holidays
- Room must be available
- No overlapping bookings
- User hasn't exceeded 9-hour daily limit
- User doesn't have another booking at same time

### Error Handling
- Validation errors shown at top of modal
- Clear, actionable error messages
- Form stays open to allow corrections
- Success notification on completion

## Event Sidebar

### Layout

#### Header
- "Event Details" title
- Close button (X)

#### Body
- Event type icon (📅 for bookings, 🎉 for holidays)
- Event title
- Detail sections with labels and values
- All information clearly formatted

#### Footer (for bookings only)
- "View Room Details" button → navigates to room page
- "Cancel Booking" button → cancels with confirmation

### Responsive Design
- **Desktop**: 450px width sidebar, content shifts left
- **Mobile**: Full-screen sidebar, content hidden behind overlay
- Smooth slide-in animation from right
- Click outside to close

## User Experience Enhancements

### Visual Feedback
- **Hover Effects**: Cards lift slightly on hover
- **Loading States**: Disabled buttons show "Creating..." or "Cancelling..."
- **Success Messages**: Alert notifications for completed actions
- **Error Messages**: Clear, specific error descriptions

### Accessibility
- Keyboard navigation support
- Focus management in modals
- Clear button labels
- High contrast colors

### Performance
- Lazy loading of room data
- Efficient event filtering
- Smooth animations (CSS transitions)
- Responsive to all screen sizes

## Code Architecture

### State Management
```typescript
// Modal state
const showBookingModal = ref(false)
const bookingForm = ref({ roomId, date, startTime, endTime, title, description })
const isSubmitting = ref(false)

// Sidebar state
const showEventSidebar = ref(false)
const selectedEvent = ref<CalendarEvent | null>(null)
const isCancelling = ref(false)
```

### Event Handlers
- `handleDateClick(arg)` - Opens booking modal with pre-filled data
- `handleEventClick(arg)` - Opens event sidebar with selected event
- `handleCreateBooking()` - Validates and submits booking
- `handleCancelBooking()` - Cancels booking with confirmation

### Validation Logic
- Date validation (past, weekend, holiday checks)
- Time validation (business hours, range checks)
- Form validation (required fields, format checks)
- Server error handling with user-friendly messages

## Usage Examples

### Creating a Booking from Calendar

#### Method 1: Month View
1. Click on desired date (e.g., October 30)
2. Modal opens with date pre-filled
3. Select room from dropdown
4. Choose start time: 09:00
5. Choose end time: 11:00
6. Enter title: "Team Standup"
7. Click "Create Booking"

#### Method 2: Week View
1. Navigate to week view
2. Click on time slot (e.g., Wednesday 2 PM)
3. Modal opens with date AND time pre-filled
4. Select room
5. Adjust time if needed
6. Enter title
7. Click "Create Booking"

#### Method 3: Day View
1. Navigate to day view for specific date
2. Click on exact hour (e.g., 3 PM - 4 PM)
3. Modal opens with precise time slot
4. Select room and add details
5. Submit

### Viewing Booking Details
1. Click any blue event on calendar
2. Sidebar slides in from right
3. View all booking information
4. Click "View Room Details" to see room info
5. Click "Cancel Booking" if needed
6. Click X or outside to close

### Cancelling a Booking
1. Click the booking event on calendar
2. Sidebar opens with details
3. Click "Cancel Booking" button
4. Confirm in dialog: "Are you sure?"
5. Booking is cancelled
6. Calendar refreshes automatically
7. Success message appears

## Integration with Backend

### API Endpoints Used
- `GET /api/dashboard` - Load calendar events and stats
- `GET /api/rooms/available` - Load available rooms for booking
- `POST /api/bookings` - Create new booking
- `DELETE /api/bookings/{id}` - Cancel booking

### Data Flow
1. **Page Load**: Fetch dashboard data and available rooms
2. **Calendar Render**: Transform events to FullCalendar format
3. **Date Click**: Validate date, open modal
4. **Event Click**: Find event data, open sidebar
5. **Create Booking**: Submit form, refresh dashboard
6. **Cancel Booking**: Delete booking, refresh dashboard

### Caching
- Redis caches calendar events (1-hour TTL)
- Cache invalidated on booking creation/cancellation
- Automatic refresh after any booking change

## Troubleshooting

### Common Issues

#### "Cannot book on weekends"
- **Cause**: Clicked on Saturday or Sunday
- **Solution**: Select a weekday (Monday - Friday)

#### "Cannot book dates in the past"
- **Cause**: Selected date is before today
- **Solution**: Choose today or future date

#### "Failed to create booking"
- **Causes**: 
  - Room already booked for that time
  - Exceeded 9-hour daily limit
  - Have another booking at same time
- **Solution**: Choose different room, time, or date

#### Modal not opening
- **Cause**: Clicked on weekend/holiday/past date
- **Solution**: Check browser console for error message

#### Sidebar not showing
- **Cause**: Event doesn't exist in dashboard data
- **Solution**: Refresh page, check if event is still valid

## Browser Compatibility
- ✅ Chrome 90+
- ✅ Firefox 88+
- ✅ Safari 14+
- ✅ Edge 90+

## Mobile Support
- Fully responsive design
- Touch-friendly click targets
- Full-screen modals on small screens
- Optimized sidebar for mobile

## Future Enhancements
- [ ] Recurring bookings
- [ ] Drag-and-drop to reschedule
- [ ] Multi-room booking
- [ ] Team member invitations
- [ ] Email notifications
- [ ] Export to Google Calendar/Outlook
- [ ] Booking templates
- [ ] Quick filters (My Bookings, All Bookings, Holidays)

## Related Documentation
- [QUICKSTART.md](QUICKSTART.md) - Setup instructions
- [REDIS-CACHING.md](REDIS-CACHING.md) - Caching implementation
- [TESTING.md](TESTING.md) - Testing procedures
- [README.md](README.md) - Project overview
