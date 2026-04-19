't # 🎯 QUICK REFERENCE CARD

## The Problem
Location from real mobile doesn't show on parent map, but notification works.

## Most Likely Cause
**Bus ID Mismatch** - Driver and parent assigned to different buses.

---

## Do This NOW

### 1. Check Firebase
```
Bus in Firestore (driver assigned):  bus1
Bus in Firestore (student assigned): bus2  ← DIFFERENT!
```

### 2. Fix It
Change student's busId to match driver's bus.

### 3. Rebuild
```bash
./gradlew clean assembleDebug
./gradlew installDebug
```

### 4. Test & Check Logs
```
Filter: LocationForegroundService
Look for: BUS_ID_FROM_DRIVER=bus1

Filter: ParentDashboard
Look for: busId=bus1

Compare: Should be SAME!
```

### 5. Verify Map Works
- Driver starts sharing
- Parent sees marker on map
- Marker moves every 5 seconds
- "Bus Status: Live" shows

---

## If Still Not Working

### Check In Order:
1. ✅ Bus IDs match in Firebase?
2. ✅ Driver logs show LOCATION_UPDATE?
3. ✅ Firebase Realtime DB has `/buses/{busId}/lat` and `lng`?
4. ✅ Parent logs show `onDataChange called`?
5. ✅ Parent logs show lat/lng values?
6. ✅ Map is not null?
7. ✅ Location permissions granted on driver?

### Realtime DB Rules
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

---

## Logcat Filter Guide

### To See Driver Logs:
```
Filter: LocationForegroundService
```

### To See Parent Logs:
```
Filter: ParentDashboard
```

### Expected Log Sequence:
```
Driver starts sharing:
D/LocationForegroundService: BUS_ID_FROM_DRIVER=bus1
D/LocationForegroundService: LOCATION_UPDATE: lat=28.6, lng=77.1
D/LocationForegroundService: Firebase update successful

Parent opens map:
D/ParentDashboard: busId=bus1
D/ParentDashboard: onDataChange called for bus: bus1
D/ParentDashboard: Lat: 28.6, Lng: 77.1
D/ParentDashboard: Updating marker position

Result:
✅ Marker visible on map
✅ Marker updates every 5 seconds
✅ Bus Status: Live
```

---

## Common Issues & Quick Fixes

| Issue | Fix |
|-------|-----|
| onDataChange not called | Check busId matches + Realtime DB rules |
| Lat/Lng null | Wait 5 sec + check permissions + check Firebase path |
| Map marker doesn't appear | Check map loaded + location valid + busId matches |
| Permission denied error | Fix Realtime DB rules (see above) |
| Bus status always offline | Check if timestamp updating in Firebase every 5 sec |

---

## 📱 Device Setup

### Driver (Real Mobile)
- [ ] GPS enabled
- [ ] Location permissions granted
- [ ] Internet connection active
- [ ] App logged in as driver

### Parent (Emulator)
- [ ] Internet connection active
- [ ] App logged in as parent
- [ ] Map loaded
- [ ] Student assigned to bus

---

## 🔧 Critical Checks

**Before testing again:**

- [ ] Rebuilt: `./gradlew clean assembleDebug`
- [ ] Reinstalled on both devices
- [ ] Bus IDs verified in Firebase (MUST match)
- [ ] Realtime DB rules updated
- [ ] Driver device GPS enabled
- [ ] Location permissions granted

---

## ⏱️ Timeline

- Build: 2 min
- Test: 3 min
- Check logs: 2 min
- Fix if needed: 5 min
- **Total: ~12 minutes**

---

## 🎯 Success Criteria

✅ All must be true:
- [ ] Driver logs show `BUS_ID_FROM_DRIVER=X`
- [ ] Parent logs show `busId=X` (same as above)
- [ ] Parent logs show `onDataChange called`
- [ ] Parent logs show lat/lng values
- [ ] Map shows marker
- [ ] Marker animates every 5 seconds
- [ ] Bus Status shows "Live"

---

## 📞 When Stuck

Share:
1. Driver logs (filter: LocationForegroundService)
2. Parent logs (filter: ParentDashboard)
3. Bus ID from each (should match)
4. Screenshot of Firebase Realtime DB `/buses/{busId}/`

This identifies issue in 2 minutes!

---

## 📚 Detailed Guides

- **ACTION_PLAN.md** - Step-by-step what to do
- **EXACT_LOGS_GUIDE.md** - What logs to look for
- **FIREBASE_LOCATION_CHECKLIST.md** - Verification checklist
- **LOCATION_DEBUGGING_GUIDE.md** - Detailed debugging

---

## 💡 Remember

The fact that:
- Driver writes successfully (timestamp updates)
- Notification arrives (connection OK)
- Map doesn't update (wrong path)

→ **Almost certainly Bus ID mismatch!**

Check that first, save 80% of debugging time!

---

**Ready? Start with ACTION_PLAN.md! 🚀**

