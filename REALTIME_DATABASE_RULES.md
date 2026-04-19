# Realtime Database Security Rules Setup

## Current Issue
- Location updates from mobile driver are being written to Realtime DB but parent cannot read them
- The map shows default location instead of actual bus location

## Root Cause
Realtime Database security rules are either:
1. Missing/default (all access denied)
2. Not properly configured for authenticated users

## Solution: Set These Rules in Firebase Console

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your `SchoolBusApp` project
3. Go to **Realtime Database** (not Firestore)
4. Click on **Rules** tab

### Step 2: Replace with These Rules

```json
{
  "rules": {
    ".read": false,
    ".write": false,
    "buses": {
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null",
        "lat": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "lng": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "timestamp": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "isSharing": {
          ".read": "auth != null",
          ".write": "auth != null"
        },
        "serviceStarted": {
          ".read": "auth != null",
          ".write": "auth != null"
        }
      }
    }
  }
}
```

### Step 3: Publish Rules
1. Click **Publish** button
2. Confirm the warning (this allows all authenticated users to read/write)

## Important Notes
- These rules allow any authenticated user to read all bus locations
- For production, you should add role-based rules (driver can only write to their assigned bus, parent can only read their bus, etc.)
- The key is **"auth != null"** which means "user is logged in"

## Why This Works
1. **LocationForegroundService** (runs on driver's mobile): Writes `lat`, `lng`, `timestamp` to `/buses/{busId}/` - this now works because `auth != null`
2. **ParentDashboardActivity** (runs on parent's mobile): Reads from `/buses/{busId}/` - this now works because `auth != null`

## Verification Checklist
- [ ] Rules are published in Firebase Console
- [ ] Both driver and parent are logged in with their Firebase accounts
- [ ] Driver starts location sharing (notification appears in system tray)
- [ ] Check Firebase Realtime DB console to see `/buses/bus1/` has lat, lng, timestamp
- [ ] Parent dashboard map should now update with actual location from mobile

## If Map Still Doesn't Update
Check these:
1. **Driver side**: Open Logcat → search "LocationForegroundService" → see location updates
2. **Parent side**: Open Logcat → search "ParentDashboard" → see if onDataChange is called
3. **Firebase console**: Check `/buses/{busId}/` to confirm lat/lng values exist
4. **Network**: Ensure both devices have internet (WiFi or cellular data)
5. **Permissions**: Driver app has location permissions granted

