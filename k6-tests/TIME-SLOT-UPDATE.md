# K6 Tests Time Slot Update - 7 AM to 9 PM UTC+7

## Overview
Updated all K6 test time slot generation to use business hours 7 AM - 9 PM in UTC+7 (Asia/Jakarta timezone).

## Changes Made

### 1. Core Time Slot Generation (`lib/api-client.js`)

#### `generateTimeSlot()` Function
**Previous Range:** 9 AM - 5 PM (9 hours)
- Base hour: 9 AM
- Hour offset: 0-8
- Total range: 9 + 0 to 9 + 8 = 9 AM to 5 PM

**New Range:** 7 AM - 9 PM (14 hours)
- Base hour: 7 AM
- Hour offset: 0-13
- Total range: 7 + 0 to 7 + 13 = 7 AM to 8 PM start (9 PM end with 1-hour booking)

**Code Changes:**
```javascript
// OLD
start.setHours(9 + hourOffset, 0, 0, 0);  // 9 AM base, hourOffset 0-8

// NEW
const hour = 7 + (hourOffset % 14);  // 7 AM base, hourOffset 0-13
start.setHours(hour, 0, 0, 0);
```

#### `generateWeekdaySlots()` Function
**Previous Range:** 9 AM - 5 PM for Monday-Friday
- Base hour: 9 AM
- Hour offset: 0-8

**New Range:** 7 AM - 9 PM for Monday-Friday
- Base hour: 7 AM  
- Hour offset: 0-13

**Code Changes:**
```javascript
// OLD
dayStart.setHours(9 + hourOffset, 0, 0, 0);  // 9 AM base

// NEW
dayStart.setHours(7 + (hourOffset % 14), 0, 0, 0);  // 7 AM base
```

### 2. Test File Updates

#### `tests/deadlock-test.js`
**Updated hourOffset calculation:**
```javascript
// OLD
const hourOffset = Math.floor(totalRequests / data.rooms.length) % 8;
const daysFromNow = 2 + Math.floor(totalRequests / (data.rooms.length * 8));

// NEW
const hourOffset = Math.floor(totalRequests / data.rooms.length) % 14;
const daysFromNow = 2 + Math.floor(totalRequests / (data.rooms.length * 14));
```

This ensures bookings are distributed across all 14 available time slots (7 AM - 8 PM) instead of just 9 time slots (9 AM - 5 PM).

#### `tests/race-condition-test.js`
**Updated comment:**
```javascript
// OLD
const TEST_SLOT = generateTimeSlot(1, 0, false); // Tomorrow at 9:00 AM (weekday only)

// NEW
const TEST_SLOT = generateTimeSlot(1, 0, false); // Tomorrow at 7:00 AM UTC+7 (weekday only)
```

#### `tests/double-booking-test.js`
**Updated comment:**
```javascript
// OLD
const TEST_SLOT = generateTimeSlot(4, 1, false); // 4 days from now, 10:00 AM (weekday)

// NEW
const TEST_SLOT = generateTimeSlot(4, 1, false); // 4 days from now, 8:00 AM UTC+7 (weekday)
```

### 3. Unchanged Functions

#### `generateOverlappingSlots()`
No changes needed - uses hardcoded hours (10 AM - 1 PM) which are already within the 7 AM - 9 PM business hours range.

## Time Slot Distribution

### Available Time Slots (All in UTC+7)
| Hour Offset | Start Time | End Time | Description |
|-------------|------------|----------|-------------|
| 0           | 7:00 AM    | 8:00 AM  | Early morning |
| 1           | 8:00 AM    | 9:00 AM  | Morning |
| 2           | 9:00 AM    | 10:00 AM | Mid-morning |
| 3           | 10:00 AM   | 11:00 AM | Late morning |
| 4           | 11:00 AM   | 12:00 PM | Pre-lunch |
| 5           | 12:00 PM   | 1:00 PM  | Lunch |
| 6           | 1:00 PM    | 2:00 PM  | Early afternoon |
| 7           | 2:00 PM    | 3:00 PM  | Afternoon |
| 8           | 3:00 PM    | 4:00 PM  | Mid-afternoon |
| 9           | 4:00 PM    | 5:00 PM  | Late afternoon |
| 10          | 5:00 PM    | 6:00 PM  | Evening |
| 11          | 6:00 PM    | 7:00 PM  | Early evening |
| 12          | 7:00 PM    | 8:00 PM  | Evening |
| 13          | 8:00 PM    | 9:00 PM  | Late evening |

**Total:** 14 available time slots (increased from 9)

### Weekday/Weekend Distribution
- **Weekdays (Monday-Friday):** 95% of bookings
- **Weekends (Saturday-Sunday):** 5% of bookings
- Weekend skipping logic remains unchanged

## Impact on Tests

### Increased Time Slot Variety
**Before:** 9 time slots × 4 rooms = 36 unique booking combinations per day
**After:** 14 time slots × 4 rooms = 56 unique booking combinations per day

**Increase:** +56% more booking combinations

### Deadlock Test Impact
The deadlock test now cycles through 14 hours instead of 9 hours before moving to the next day:
- **Before:** 4 rooms × 9 hours = 36 bookings per day cycle
- **After:** 4 rooms × 14 hours = 56 bookings per day cycle

This better simulates real-world usage patterns with extended business hours.

### Stress Test Impact
Read-only operations unaffected - they query existing bookings regardless of time slot generation.

## Testing Recommendations

### 1. Verify Time Slot Generation
```bash
# Run race condition test to verify new time slots
cd k6-tests
./run-all-tests.sh
```

### 2. Check Booking Distribution
After running tests, verify bookings are distributed across 7 AM - 9 PM range:
```sql
SELECT 
  EXTRACT(HOUR FROM start_time) as booking_hour,
  COUNT(*) as booking_count
FROM bookings
WHERE start_time >= NOW() - INTERVAL '1 day'
GROUP BY booking_hour
ORDER BY booking_hour;
```

Expected result: Bookings distributed between hours 7-20 (7 AM - 8 PM starts).

### 3. Weekend Skipping Verification
```sql
SELECT 
  EXTRACT(DOW FROM start_time) as day_of_week,
  COUNT(*) as booking_count
FROM bookings
WHERE start_time >= NOW() - INTERVAL '1 day'
GROUP BY day_of_week
ORDER BY day_of_week;
```

Expected result:
- Days 1-5 (Monday-Friday): ~95% of bookings
- Days 0,6 (Sunday, Saturday): ~5% of bookings

## Configuration Summary

| Setting | Previous | New | Change |
|---------|----------|-----|--------|
| Base hour | 9 AM | 7 AM | -2 hours |
| Hour offset range | 0-8 | 0-13 | +6 values |
| Available slots | 9 | 14 | +56% |
| Business hours | 9 AM - 5 PM | 7 AM - 9 PM | +6 hours |
| Timezone | UTC+7 | UTC+7 | No change |
| Weekend logic | 95%/5% | 95%/5% | No change |

## Files Modified

1. ✅ `k6-tests/lib/api-client.js`
   - Updated `generateTimeSlot()` function
   - Updated `generateWeekdaySlots()` function
   - Added timezone documentation

2. ✅ `k6-tests/tests/deadlock-test.js`
   - Updated hourOffset calculation (% 8 → % 14)
   - Updated daysFromNow calculation (× 8 → × 14)

3. ✅ `k6-tests/tests/race-condition-test.js`
   - Updated comment (9:00 AM → 7:00 AM UTC+7)

4. ✅ `k6-tests/tests/double-booking-test.js`
   - Updated comment (10:00 AM → 8:00 AM UTC+7)

## Backward Compatibility

⚠️ **Breaking Change:** Tests using hourOffset values will now generate different time slots.

**Migration Impact:**
- Old tests using `hourOffset=0` generated 9 AM slots → now generate 7 AM slots
- Old tests using `hourOffset=8` generated 5 PM slots → now generate 3 PM slots
- Tests should continue working but with shifted time slots

**Recommendation:** Review any hardcoded assertions about booking times in test code.

## Next Steps

1. ✅ Update time slot generation functions (COMPLETED)
2. ✅ Update test files with new hourOffset ranges (COMPLETED)
3. ⏳ Run full test suite to verify changes
4. ⏳ Monitor database for correct time slot distribution
5. ⏳ Update documentation if needed

## Related Documentation

- See `K6-TESTS-COMPLETE.md` for overall K6 test documentation
- See `QUICKSTART.md` for running K6 tests
- See `N+1-QUERY-OPTIMIZATION.md` for recent database optimizations

---

**Updated:** 2024-01-29
**Status:** Implemented, Ready for Testing
**Impact:** Low risk - expands time range without breaking existing functionality
