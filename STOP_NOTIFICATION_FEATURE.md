# Stop Notification Feature - Implementation Guide

## Overview
This feature notifies parents whenever the school bus reaches a designated stop on the route. When a driver's bus comes within 150 meters of a stop, all parents whose children are on that bus will receive a notification with the stop name and student information.

## What Was Added

### 1. **NotificationHelper.kt** (New File)
Utility class that handles creating and sending notifications to parents.

**Key Features:**
- Creates notification channels (required for Android 8.0+)
- Generates unique notification IDs for each stop
- Sends high-priority notifications that notify parents
- Includes stop name, bus ID, and student name in notification
- Clicking notification opens ParentDashboardActivity

**Main Method:**
```kotlin
fun sendStopNotification(
    stopName: String,
    busId: String,
    parentUid: String,
    studentName: String
)
```

### 2. **StopDetectionManager.kt** (New File)
Core manager class that detects when driver reaches stops and triggers notifications.

**Key Features:**
- Tracks visited stops to avoid duplicate notifications
- Calculates distance between driver and stops
- Sends notifications only when driver is within 150m of a stop
- Resets visited stops when needed (for new days/routes)
- Only notifies each stop once per session

**Main Method:**
```kotlin
fun checkAndNotifyStopsReached(
    driverLocation: LatLng,
    stops: List<Stop>,
    busId: String,
    onStopReached: ((Stop) -> Unit)? = null
)
```

**How it works:**
1. Receives current driver location from GPS
2. Gets all stops from the route
3. Calculates distance to each stop
4. If distance < 150 meters AND stop not yet visited:
   - Queries all students on the bus from Firestore
   - Gets parent UID for each student
   - Sends notification to each parent
   - Marks stop as visited to prevent duplicate notifications

### 3. **Updated DriverRouteMapActivity.kt**
Modified to integrate stop detection and notifications.

**Changes:**
- Added `stopDetectionManager` instance variable
- Added `currentBusId` to track which bus is being driven
- Initialize StopDetectionManager in onCreate()
- Store bus ID when loading route data
- Call `stopDetectionManager.checkAndNotifyStopsReached()` when checking proximity
- Reset visited stops in onDestroy() for clean lifecycle management
- Added callback to show UI toast when stop is reached

## How It Works (Complete Flow)

1. **Driver starts sharing location**
   - LocationForegroundService updates driver location every 5 seconds in Realtime DB
   - DriverRouteMapActivity listens for location updates

2. **Location updates received**
   - Each new location triggers `checkProximityToStops()`
   - StopDetectionManager checks distance to all stops

3. **Driver approaches a stop (< 150m)**
   - StopDetectionManager detects proximity
   - Queries Firestore for students on this bus
   - For each student, retrieves parent UID
   - Sends notification to parent with:
     - "Bus Reached Stop" title
     - Stop name and bus ID
     - Student name
   - Marks stop as visited

4. **Parent receives notification**
   - High-priority notification appears on parent's device
   - Can tap notification to open ParentDashboardActivity
   - Shows real-time updates about bus and student location

## Data Flow Diagram

```
LocationForegroundService
       ↓
(Updates /buses/{busId} lat/lng)
       ↓
DriverRouteMapActivity
       ↓
(Listens to location updates)
       ↓
checkProximityToStops()
       ↓
StopDetectionManager
       ├→ Calculate distance
       ├→ Check if within 150m
       └→ If yes, query students
             ↓
          (Get from Firestore: students collection)
             ↓
          For each student:
             ├→ Get parentUid
             └→ NotificationHelper.sendStopNotification()
                    ↓
             Android NotificationManager
                    ↓
             Parent Device (receives notification)
```

## Firebase Collections Used

### students (Read)
- Fields accessed:
  - `name` - student name for notification
  - `parentUid` - to identify parent for notification
  - `busId` - to filter students for specific bus

Query:
```
students where busId == currentBusId
```

### routes (Read)
- Already being read for stop information
- Used by StopDetectionManager to get stop details

## Notification Content Example

When bus reaches "School Gate" stop:

**Title:** Bus Reached Stop

**Short Text:** BUS01 has reached School Gate

**Long Text:**
```
Your child's bus has reached School Gate.
Student: John Doe
```

**Action:** Tap to open ParentDashboardActivity for live tracking

## Proximity Radius
- **Default:** 150 meters
- **Location:** `StopDetectionManager.STOP_PROXIMITY_RADIUS`
- **Can be customized** by changing the constant value

## Key Features Implemented

✅ **Stop Detection:** Automatically detects when driver reaches stops
✅ **Parent Notifications:** Sends notifications to all parents with children on bus
✅ **Duplicate Prevention:** Tracks visited stops to avoid duplicate notifications
✅ **Student Context:** Includes student name in notification
✅ **UI Integration:** Toast message also shown to driver
✅ **Clean Lifecycle:** Resets visited stops when activity closes
✅ **High Priority:** Notifications are set to high priority for visibility
✅ **Click Action:** Notification opens ParentDashboardActivity

## Testing Checklist

1. ✅ Driver starts sharing location from DriverDashboardActivity
2. ✅ Driver navigates to DriverRouteMapActivity to see route
3. ✅ Drive near first stop (within 150m)
4. ✅ Verify toast message appears on driver's device
5. ✅ Switch to parent device/emulator
6. ✅ Verify notification appears with stop name and student name
7. ✅ Tap notification to verify it opens ParentDashboardActivity
8. ✅ Drive away and back to stop - should NOT send duplicate notification
9. ✅ Continue to next stop - notification should appear for new stop

## Files Modified/Created

| File | Type | Change |
|------|------|--------|
| NotificationHelper.kt | Created | New notification utility |
| StopDetectionManager.kt | Created | New stop detection logic |
| DriverRouteMapActivity.kt | Modified | Integrated stop detection |

## No Breaking Changes

✅ All existing functionality preserved:
- Location sharing still works
- Route display still works
- Polyline drawing still works
- Driver dashboard unchanged
- Parent dashboard unchanged
- ManageBoarding unchanged

## Future Enhancements

1. Add notification sound/vibration preferences
2. Store notification history in Firestore
3. Allow parents to disable notifications
4. Add distance countdown to next stop
5. Integrate with FCM (Firebase Cloud Messaging) for more reliable delivery
6. Add notification badges showing unread count
7. Allow admin to configure proximity radius per route

## Troubleshooting

### Notifications not appearing
1. Check if students exist in Firestore with correct busId
2. Verify parentUid field is populated in students collection
3. Ensure StopDetectionManager.checkAndNotifyStopsReached() is being called
4. Check logcat for any exceptions

### Duplicate notifications
1. StopDetectionManager automatically prevents duplicates
2. Visited stops are tracked and reset on activity destroy
3. If needed, manually call `stopDetectionManager.resetVisitedStops()`

### Stop not being detected
1. Verify stop coordinates are correct in routes collection
2. Check if proximity radius (150m) is appropriate for your use case
3. Ensure driver's GPS is enabled and location is accurate
4. Check distance calculations in calculateDistance() method

