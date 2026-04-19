# Firestore Rules - Final Correct Format

## The Error
"Line 6: The 'match' body must contain at least one declaration."

This happens because functions are defined inside the `match` block. Functions must be defined at the service level, outside match blocks.

## Correct Rules Structure

```javascript
rules_version = '2';

service cloud.firestore {
  // Helper functions defined at service level
  function isAdmin(uid) {
    return get(/databases/$(database)/documents/users/$(uid)).data.role == 'admin';
  }

  function isDriver(uid) {
    return get(/databases/$(database)/documents/users/$(uid)).data.role == 'driver';
  }

  function isParent(uid) {
    return get(/databases/$(database)/documents/users/$(uid)).data.role == 'parent';
  }

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

## Key Changes

- **Functions moved outside**: `isAdmin()`, `isDriver()`, `isParent()` are now defined at the service level, before the main `match` block
- **Clean structure**: Functions first, then the main documents match block
- **No extra content**: Make sure there's no extra text like "1" or duplicate rules_version

## Test After Publishing

1. Wait for rules to deploy (green checkmark)
2. Test admin: Add a route → Should work ✅
3. Test driver: "Drop All" students → Should work ✅

This structure follows Firebase's official rules syntax! 🚀
