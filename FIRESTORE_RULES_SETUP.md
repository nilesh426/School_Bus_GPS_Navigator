# Firestore Security Rules Setup for Route & Stops Feature

## Current Issue
The app is getting `PERMISSION_DENIED` when trying to write to the `routes` collection:
```
Stream closed with status: Status{code=PERMISSION_DENIED, description=Missing or insufficient permissions}
Write failed at routes/PJ06eGOU0NXGCEujqTYl
```

## Solution: Update Firestore Security Rules

### Step 1: Navigate to Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com)
2. Select your **SchoolBusApp** project
3. Navigate to **Firestore Database** → **Rules** tab

### Step 2: Update Security Rules

Replace the existing rules with the following to support the Route & Stops feature:

```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    
    // Allow read access to users collection for role-based auth
    match /users/{uid} {
      allow read: if request.auth != null;
      allow create: if request.auth != null; // Self-registration for parents
      allow update, delete: if request.auth.uid == uid || isAdmin(uid);
    }

    // Students collection - admin only (create, update, delete)
    match /students/{document=**} {
      allow read: if request.auth != null;
      allow create, update, delete: if isAdmin(request.auth.uid);
    }

    // Buses collection - admin only (create, update, delete)
    match /buses/{document=**} {
      allow read: if request.auth != null;
      allow create, update, delete: if isAdmin(request.auth.uid);
    }

    // Routes collection - admin only (create, update, delete, list)
    match /routes/{document=**} {
      allow read: if request.auth != null;
      allow create, update, delete: if isAdmin(request.auth.uid);
    }

    // Student attendance - read by parent, write by driver/admin
    match /student_attendance/{document=**} {
      allow read: if request.auth != null;
      allow create, update, delete: if isDriver(request.auth.uid) || isAdmin(request.auth.uid);
    }

    // Helper function to check if user is admin
    function isAdmin(uid) {
      return get(/databases/$(database)/documents/users/$(uid)).data.role == 'admin';
    }

    // Helper function to check if user is driver
    function isDriver(uid) {
      return get(/databases/$(database)/documents/users/$(uid)).data.role == 'driver';
    }

    // Helper function to check if user is parent
    function isParent(uid) {
      return get(/databases/$(database)/documents/users/$(uid)).data.role == 'parent';
    }

    // Default deny all
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### Step 3: Publish Rules
1. Click **Publish** button
2. Confirm the changes
3. Wait for the rules to deploy (usually takes a few seconds)

## What Changed

1. **New `routes` collection rule**: Added full support for admin-only CRUD operations
2. **Helper functions**: Created `isAdmin()`, `isDriver()`, `isParent()` functions for role-based access
3. **Read access**: All authenticated users can read routes (needed for driver/parent views later)
4. **Write access**: Only admins can create, update, delete routes

## Testing the Fix

1. **Rebuild and run** the app:
   ```bash
   ./gradlew installDebug
   ```

2. **Test admin flow**:
   - Login as admin
   - Go to Admin Dashboard → Manage Routes
   - Add a new route → Should succeed now
   - Add stops to the route → Should succeed

3. **Expected Success**:
   - Toast message: "Route added successfully"
   - Route appears in the list

## Firestore Structure After Fix

Your Firestore will have:
```
users/{uid}
  - email: string
  - role: "admin" | "driver" | "parent"

buses/{busId}
  - busId: string
  - route: string
  - driverEmail: string
  - routeId: string

routes/{routeId}
  - name: string
  - stops: array[
      {
        order: number,
        name: string,
        lat: number,
        lng: number
      }
    ]

students/{studentId}
  - name: string
  - studentClass: string
  - parentUid: string
  - busId: string
  - feeStatus: string
  - feePlan: string

student_attendance/{attendanceId}
  - studentId: string
  - busId: string
  - date: date
  - boardedAt: timestamp
  - droppedAt: timestamp
  - status: string
```

## Troubleshooting

**Still getting PERMISSION_DENIED?**
1. Clear browser cache and refresh Firebase Console
2. Verify user role in Firestore: `users/{uid}` should have `role: "admin"`
3. Check that rules are published (check deploy status in Rules tab)
4. Restart the app after rules are published

**Want to test with relaxed rules temporarily?**
Replace with this for development only:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    allow read, write: if request.auth != null;
  }
}
```
⚠️ **Important**: Switch back to strict rules before production!

## Next Steps

After routes are working:
- [ ] Test assigning routes to buses
- [ ] Verify route data persists correctly
- [ ] Prepare driver dashboard to display assigned route
- [ ] Prepare map integration to show route stops

