# Location Update Debugging Guide

## 🔍 **STEP 1: Build and Deploy**

```bash
./gradlew clean assembleDebug
```

Install on both your **real mobile** (driver) and **emulator** (parent):
```bash
./gradlew installDebug
```

---

## 📱 **STEP 2: Open Logcat**

In Android Studio:
1. Bottom panel → **Logcat**
2. Filter: Type `LocationForegroundService` in the search box
3. Keep this window open

---

## 🧪 **STEP 3: Test on Mobile (Driver)**

1. **Open app on REAL MOBILE** (driver account)
2. Login as driver
3. See "Assigned Bus: **[BUS_ID]**"
4. **Click "Start Sharing Location"**

### ✅ Look for these logs:
```
D/LocationForegroundService: ===== LOCATION SERVICE STARTED =====
D/LocationForegroundService: BUS_ID_FROM_DRIVER=bus1
D/LocationForegroundService: Writing location updates to: /buses/bus1/
D/LocationForegroundService: LOCATION_UPDATE: lat=28.123, lng=77.456
D/LocationForegroundService: Firebase update successful
```

**WRITE DOWN the BUS_ID** - this is important!

---

## 👨‍👩‍👧 **STEP 4: Test on Emulator (Parent)**

1. **Open app on EMULATOR** (parent account)
2. Login as parent
3. You should see bus details

### ✅ Look for these logs:
```
D/ParentDashboard: showStudentDetails called for: John Doe, busId=bus1
D/ParentDashboard: About to start live tracking with busId=bus1
D/ParentDashboard: Starting live tracking for bus: bus1
D/ParentDashboard: ValueEventListener attached
D/ParentDashboard: onDataChange called for bus: bus1
D/ParentDashboard: Lat: 28.123, Lng: 77.456, Timestamp: 1713181000000
D/ParentDashboard: Updating marker position to: LatLng(lat/lng: (28.123, 77.456))
```

---

## 🚨 **TROUBLESHOOTING**

### **Issue 1: Bus ID Mismatch**
If in logs you see:
```
BUS_ID_FROM_DRIVER=bus1
busId=bus2  // ← DIFFERENT!
```

**Solution:**
- Go to Firebase Console → Firestore
- Check "buses" collection
- Find the **same busId** for both driver and student

---

### **Issue 2: Lat/Lng are null**
```
D/ParentDashboard: Lat: null, Lng: null
```

**Causes:**
- Driver location not being written yet (wait 5 seconds)
- Bus ID mismatch (see Issue 1)
- Permissions not granted on driver device

**Solution:**
1. Check driver logs for `LOCATION_UPDATE`
2. Go to Firebase → Realtime Database
3. Look for `/buses/{busId}/lat` and `/buses/{busId}/lng`
4. If empty, driver didn't write anything yet

---

### **Issue 3: onDataChange Not Called**
```
// NO onDataChange logs appear
```

**Causes:**
- Wrong bus ID in parent
- Realtime Database rules issue
- Bus ID not created in Realtime Database yet

**Solution:**
1. Go to Firebase → Realtime Database
2. You should see `/buses/{busId}` node
3. If not, driver needs to start sharing first
4. Check Realtime Database rules (should allow `.read: true`)

---

### **Issue 4: Firebase Update Failed**
```
E/LocationForegroundService: Firebase update failed: Permission denied
```

**Solution:**
1. Go to Firebase Console → Realtime Database → Rules
2. Use these rules:
```json
{
  "rules": {
    "buses": {
      "$busId": {
        ".read": true,
        ".write": true
      }
    }
  }
}
```
3. Click **Publish**

---

## 📊 **STEP 5: Compare Bus IDs**

**From LocationForegroundService log:**
```
BUS_ID_FROM_DRIVER=bus1
```

**From ParentDashboard log:**
```
busId=bus1
```

✅ **They must match!**

If they don't match:
1. Go to Firebase → Firestore
2. Check `students` collection
3. Find the student assigned to parent
4. Check the `busId` field
5. Make sure driver is assigned to the **same bus**

---

## 🔧 **Firebase Checks**

1. **Go to Firebase Console**
2. Click **Realtime Database**
3. You should see:
```
buses/
├── bus1/
│   ├── lat: 28.123
│   ├── lng: 77.456
│   ├── timestamp: 1713181000000
│   ├── isSharing: true
│   └── serviceStarted: 1713181000000
```

If you don't see lat/lng updating, **driver is not writing**.

---

## ✅ **SUCCESS INDICATORS**

All of these should be true:

- [ ] Driver logs show `BUS_ID_FROM_DRIVER=bus1`
- [ ] Driver logs show `LOCATION_UPDATE: lat=...`
- [ ] Driver logs show `Firebase update successful`
- [ ] Parent logs show `busId=bus1` (same as driver)
- [ ] Parent logs show `onDataChange called`
- [ ] Parent logs show `Lat: 28.123, Lng: 77.456`
- [ ] Map marker appears and moves
- [ ] Firebase Realtime Database shows `/buses/bus1/lat` and `lng` updating

---

## 📞 **Share These Logs**

If it's still not working, share:
1. **Driver (LocationForegroundService) logs** - at least 10 lines
2. **Parent (ParentDashboard) logs** - at least 10 lines
3. **Bus ID values** from both

Example:
```
Driver logs:
D/LocationForegroundService: BUS_ID_FROM_DRIVER=bus1

Parent logs:
D/ParentDashboard: showStudentDetails called for: John, busId=bus1
D/ParentDashboard: Starting live tracking for bus: bus1
```

This will help identify the exact issue!

---

## 🚀 **QUICK FIX CHECKLIST**

Before testing again:

- [ ] Rebuilt project
- [ ] Reinstalled on both devices
- [ ] Both logged in with correct accounts
- [ ] Driver phone has GPS enabled
- [ ] Parent student has correct busId in Firestore
- [ ] Driver is assigned to same bus in Firestore
- [ ] Realtime Database rules allow `.read: true` and `.write: true` on `/buses`

