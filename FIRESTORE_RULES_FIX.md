# Firestore Rules - Correct Format

## The Error
You're getting: `Line 2: mismatched input 'match' expecting {'function', 'import', 'service', 'rules_version'}`

This means you have `match` on line 2, but it should be `service cloud.firestore {` first.

## Correct Rules (Copy-Paste Ready)

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

## How to Fix

1. **Delete everything** in the Rules editor
2. **Copy the entire block above** (from `rules_version = '2';` to the final `}`)
3. **Paste it** into the Rules editor
4. **Click Publish**

## What Went Wrong

The rules structure must be:
```
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {
    // All your match rules go here
  }
}
```

You probably copied only the inner `match` rules without the outer `service cloud.firestore` wrapper.

## Test After Publishing

1. Wait for rules to deploy
2. Test admin: Add a route → Should work
3. Test driver: "Drop All" students → Should work

The rules are now correctly formatted! 🚀
