# Stop Notification Feature - Testing Guide

## Prerequisites
- Android device/emulator with Android 8.0+ (Android 13+ for full notification support)
- Two emulator instances (one for driver, one for parent) OR two physical devices
- Firebase project configured with:
  - Firestore database
  - Realtime Database
  - Authentication (with test users set up)

## Test Data Setup

### 1. Create Test Bus with Route and Stops
**In Firebase Console:**
1. Go to Firestore
2. Create collection: `routes`
3. Add document with fields:
   ```
   - name: "Test Route"
   - stops: array of stops
   ```

4. In the stops array, add each stop as a map:
   ```
   Stop 1:
   - order: 1
   - name: "School Gate"
   - lat: 28.6139
   - lng: 77.2090
   
   Stop 2:
   - order: 2
   - name: "Park Entrance"
   - lat: 28.6200
   - lng: 77.2150
   
   Stop 3:
   - order: 3
   - name: "Hospital Junction"
   - lat: 28.6260
   - lng: 77.2100
   ```

### 2. Create Test Bus
In `buses` collection:
```
- busId: "BUS01"
- driverEmail: "driver@test.com"
- route: "Test Route"
```

### 3. Create Test Students
In `students` collection:
```
Student 1:
- studentId: "STU001"
- name: "John Doe"
- class: "10A"
- parentUid: "parent_uid_1"
- busId: "BUS01"

Student 2:
- studentId: "STU002"
- name: "Jane Smith"
- class: "10B"
- parentUid: "parent_uid_2"
- busId: "BUS01"
```

### 4. Create Test Users
In `users` collection:
```
For parent_uid_1:
- role: "parent"
- email: "parent1@test.com"

For parent_uid_2:
- role: "parent"
- email: "parent2@test.com"

For driver:
- role: "driver"
- email: "driver@test.com"
```

## Testing Steps

### Part 1: Setup and Preparation

1. **Start with two emulator instances:**
   - Emulator 1: Driver account
   - Emulator 2: Parent account

2. **On Emulator 1 (Driver):**
   - Launch app
   - Login as `driver@test.com`
   - Go to Driver Dashboard
   - Verify "BUS01" is shown as assigned bus

3. **On Emulator 2 (Parent):**
   - Launch app
   - Login as `parent1@test.com`
   - Go to Parent Dashboard
   - Should see student "John Doe" with bus "BUS01"

### Part 2: Location Sharing Test

1. **On Driver Emulator:**
   - Click "Start Sharing" button on Driver Dashboard
   - Verify status changes to "Sharing live location"
   - Click "View Route"
   - Should see map with route markers and stops

2. **Verify permissions:**
   - Dialog should appear requesting:
     - Location permission (if not granted)
     - Notification permission (Android 13+)
   - Grant all permissions

3. **On parent emulator (background):**
   - Open Logcat
   - Filter by: `com.example.schoolbusapp`

### Part 3: Stop Detection Test

**Method 1: Using Google Maps Emulator Controls**

1. In Android Studio, open "Extended controls" of driver emulator
2. Go to "Location" tab
3. Set coordinates to first stop: `28.6139, 77.2090`
4. Driver device should move to this location on the map

**Method 2: Using Logcat Simulation**
1. Run: `adb emu geo fix 77.2090 28.6139`
   (This sets location to first stop - School Gate)

### Part 4: Verify Notifications

1. **After setting driver location to first stop (School Gate):**
   - Wait 5-10 seconds for location update
   - On driver device: Should see toast: "⚠️ 1. School Gate is nearby!"
   - Check system tray on driver device

2. **On Parent Device:**
   - Open notification panel (swipe down from top)
   - Should see notification with:
     ```
     Title: "Bus Reached Stop"
     Text: "BUS01 has reached School Gate"
     Extended: "Your child's bus has reached School Gate. Student: John Doe"
     ```

3. **Verify notification is clickable:**
   - Tap notification
   - Should open ParentDashboardActivity
   - Should show live bus location

### Part 5: Multi-Stop Test

1. **Move to second stop (Park Entrance):**
   - Driver location: `28.6200, 77.2150`
   - Wait for location update
   - Driver device: Toast should appear "⚠️ 2. Park Entrance is nearby!"
   - Parent device: New notification should appear for second stop

2. **Move to third stop (Hospital Junction):**
   - Driver location: `28.6260, 77.2100`
   - Verify notification appears for third stop
   - Toast on driver device should show

### Part 6: Duplicate Notification Prevention

1. **Verify no duplicate notifications:**
   - Move back to "School Gate" coordinates
   - Should NOT receive duplicate notification
   - Only ONE notification per stop per session

2. **Reset test:**
   - Close and reopen app
   - Go back to "School Gate"
   - Should receive notification again (new session)

### Part 7: Multiple Student Test

1. **Second Parent Device (if available):**
   - Login as parent2@test.com
   - Move driver to first stop again
   - Second parent should ALSO receive notification for "Jane Smith"

2. **Verify both parents get notifications:**
   - Check both devices' notification centers
   - Both should have received stop notifications

### Part 8: Stop Reached When Sharing OFF

1. **Stop sharing location:**
   - Driver clicks "Stop Sharing" button
   - Move location to a stop coordinate

2. **Verify NO notification sent:**
   - Should not see any toast or notification
   - Feature should only work when sharing is active

## Expected Behaviors

### ✅ Should Happen
- Toast appears on driver's device when within 150m of stop
- Notification appears on parent's device with stop name
- Notification persists until dismissed
- Tapping notification opens ParentDashboardActivity
- Each stop gets only ONE notification per session
- All students on bus get notifications

### ❌ Should NOT Happen
- Duplicate notifications for same stop
- Notifications when location sharing is OFF
- Notifications for other bus stops
- App crash when notification is sent
- Permission errors in logcat

## Troubleshooting

### Issue: No notifications appearing

**Check 1: Location updates**
- Logcat should show:
  ```
  lat: X.XXXX
  lng: Y.YYYY
  timestamp: XXXXXXXXX
  ```
- If not showing, location sharing isn't working

**Check 2: Notification permission**
- On Android 13+, grant POST_NOTIFICATIONS permission
- Check in Settings > Apps > SchoolBusApp > Permissions

**Check 3: Firestore data**
- Verify students collection has correct busId
- Verify parentUid exists in document
- Check rules allow reading students collection

**Check 4: Distance calculation**
- Current proximity radius is 150 meters
- Must be exactly within this radius
- Use extended controls to set precise coordinates

### Issue: Toast not appearing on driver

**Check 1: Location service running**
- Verify LocationForegroundService is active
- Check device foreground service settings

**Check 2: Logcat output**
```
Filter: "SchoolBusApp"
Should see: "Updated buses/{busId} with lat/lng"
```

### Issue: Wrong student name in notification

**Check 1: Firestore students document**
- Verify "name" field exists
- Check spelling and case

**Check 2: Verify busId matching**
```
Student's busId == Driver's busId
Must be exact match
```

## Performance Testing

1. **Multiple Students (10+ on same bus):**
   - Move driver to stop
   - All 10+ notifications should appear
   - No significant delay

2. **Multiple Stops (5+ on route):**
   - Move through all stops
   - Each stop should trigger notification
   - No freezing or lag

3. **Long Duration Test (30+ minutes):**
   - Keep location sharing active
   - Move through multiple stops multiple times
   - App should remain stable

## Security Testing

1. **Parent cannot see other students:**
   - Login as parent1 
   - Should NOT get notification for parent2's child
   - Only get notification for assigned children

2. **Driver cannot receive notifications:**
   - Driver should NOT receive stop notifications
   - Only parents should receive them

3. **Unauthorized access:**
   - Without proper busId/studentId, no notifications
   - Firestore rules should enforce this

## Cleanup

1. **After testing:**
   - Remove test routes
   - Remove test students
   - Remove test buses
   - Remove test users

2. **OR create separate Firebase project:**
   - Use separate Firebase project for testing
   - Don't affect production data

## Test Report Template

```
Test Date: ___________
Tester: ___________
Device: ___________ (Model, Android Version)

Tests Passed:
☐ Stop detection works
☐ Notification appears on parent device
☐ Toast appears on driver device
☐ Notification content is correct
☐ Multiple stops work
☐ No duplicate notifications
☐ Sharing OFF prevents notifications
☐ Click notification opens dashboard

Issues Found:
[List any issues here]

Logs:
[Attach relevant logcat output]
```

## Success Criteria

Feature is working correctly when:
1. ✅ Driver's location is updated every 5 seconds
2. ✅ Toast appears on driver within 150m of stop
3. ✅ Notification appears on parent's device
4. ✅ Notification includes correct stop name and student
5. ✅ No duplicate notifications per session
6. ✅ Works for multiple students on same bus
7. ✅ Works for multiple stops on route
8. ✅ Works on Android 8.0 through 14
9. ✅ No app crashes
10. ✅ Notification permission requested on Android 13+

All 10 criteria met = Feature ready for production! 🎉

