# Live Location Debug Guide - Real Device Testing

## Your Current Issue
✓ Driver shares location from mobile  
✓ Data is written to Realtime DB (you confirmed this)  
✗ Parent map shows default location instead of actual location

## Step-by-Step Debugging

### Step 1: Update Realtime Database Rules in Firebase Console
**This is the MOST LIKELY fix!**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select **SchoolBusApp** project
3. Go to **Realtime Database** → **Rules**
4. Replace ALL content with:
```json
{
  "rules": {
    ".read": false,
    ".write": false,
    "buses": {
      "$busId": {
        ".read": "auth != null",
        ".write": "auth != null"
      }
    }
  }
}
```
5. Click **Publish**

---

### Step 2: Verify Logs from Driver (Location Sending)

**On the driver's mobile:**

1. Start the app
2. Open Android Studio → Logcat
3. Filter: `LocationForegroundService`
4. Click "Start Sharing" button
5. Look for these logs (should appear every 5 seconds):
```
===== LOCATION UPDATE RECEIVED =====
Latitude: 37.4221...
Longitude: -122.0841...
Accuracy: 5.123
Bus ID: bus1
✓ Location update successful - Written to: /buses/bus1/
Data: lat=37.4221, lng=-122.0841
```

**❌ If you see instead:**
```
❌ Location update FAILED
Error: Permission denied
Check Firebase Realtime Database rules allow 'auth != null'
```
→ **Your Realtime DB rules are not set correctly** (go back to Step 1)

---

### Step 3: Verify Logs from Parent (Location Reading)

**On the parent's mobile:**

1. Open the app
2. Go to Parent Dashboard
3. Open Android Studio → Logcat
4. Filter: `ParentDashboard`
5. You should see logs like:
```
===== STARTING LIVE BUS TRACKING =====
Bus ID: bus1
Current User UID: xvXslcrSKueqiZtOfc2CeBEn5Fq2
Is User Authenticated: true
✓ ValueEventListener attached to: buses/bus1
=== onDataChange called for bus: bus1 ===
Snapshot exists: true, children count: 3
Child key: lat = 37.4221
Child key: lng = -122.0841
Child key: timestamp = 1713398523000
✓ Retrieved - Lat: 37.4221, Lng: -122.0841, Timestamp: 1713398523000
✓ Creating new marker at: LatLng(37.4221, -122.0841)
```

**❌ If you see instead:**
```
=== onDataChange called for bus: bus1 ===
Snapshot exists: false, children count: 0
⚠ LOCATION IS NULL - lat: null, lng: null
```
→ Either driver isn't sharing location, OR parent can't read due to rules

**❌ If you see:**
```
❌ DATABASE ERROR: Permission denied
```
→ Realtime DB rules are wrong (go back to Step 1)

---

### Step 4: Verify Firebase Console - Realtime Database

1. Go to Firebase Console → **Realtime Database**
2. Look for `/buses/bus1/` path
3. You should see:
```
buses
└── bus1
    ├── lat: 37.4221
    ├── lng: -122.0841
    ├── timestamp: 1713398523000
    └── isSharing: true
```

If it's empty or `bus1` doesn't exist → Driver isn't sharing location

---

### Complete Checklist

- [ ] **Realtime DB rules updated** with `"auth != null"`
- [ ] **Driver authenticated**: Logged in with email/password
- [ ] **Parent authenticated**: Logged in with email/password
- [ ] **Driver location permission**: Granted in app settings
- [ ] **Driver has internet**: WiFi or mobile data
- [ ] **Parent has internet**: WiFi or mobile data
- [ ] **Student assigned to bus1**: Check Firestore `students` collection
- [ ] **Location logs show ✓ successful**: Check Logcat
- [ ] **Firebase console shows lat/lng data**: Check Realtime DB
- [ ] **Parent log shows onDataChange called**: Check Logcat
- [ ] **Map marker appears**: Check if lat/lng are not null

---

## Common Issues & Fixes

### Issue: "Permission denied" error in logs
**Fix**: Update Realtime DB rules (Step 1)

### Issue: onDataChange never called, but rules are correct
**Fix**: Ensure driver app is running and has location permissions granted

### Issue: onDataChange called but lat/lng are null
**Fix**: Driver not sharing location - click "Start Sharing" on driver dashboard

### Issue: Map shows default Google location
**Fix**: Likely one of the above - follow steps 1-4 in order

### Issue: Works on emulator but not real device
**Fix**: Usually rules issue. Also check network connectivity.

---

## Production Rules (Future)

For production with role-based access:
```json
{
  "rules": {
    "buses": {
      "$busId": {
        ".read": "auth != null",
        ".write": "root.child('users').child(auth.uid).child('role').val() == 'driver' && auth.uid != null"
      }
    }
  }
}
```

This restricts writes to only drivers.

---

## Still Not Working?

1. **Copy the complete logs** from both driver and parent
2. **Check Firebase console** for any error messages
3. **Verify authentication** works (login/logout works fine)
4. **Test on emulator first** to isolate the issue
5. **Ensure Google Maps API key** is correct in AndroidManifest.xml

