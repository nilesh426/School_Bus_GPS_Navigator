# Google Directions API Setup Guide

## Problem
The route polyline is showing "Route API error" because the Directions API is not enabled for your Google Cloud project.

## Solution: Enable Directions API

### Step 1: Go to Google Cloud Console
1. Open: https://console.cloud.google.com
2. Sign in with your Google account

### Step 2: Select Your Project
1. Click the project dropdown at the top
2. Select the project associated with your API key (AIzaSyArZRF0VSvsMa9_i2s5pPx1v8wRHDTzv1s)
3. If you don't know which project, go to **APIs & Services > Credentials** and look for the key

### Step 3: Enable Directions API
1. Go to **APIs & Services > Library** (left sidebar)
2. Search for **"Maps Routes API"** or **"Directions API"**
3. Click on it
4. Click **ENABLE** button (blue button at top)

### Step 4: Verify
1. Go back to **APIs & Services > Enabled APIs**
2. Look for "Directions API" or "Maps Routes API" in the list
3. It should show as enabled

### Step 5: Test in Your App
1. Rebuild your project: `Build → Rebuild Project`
2. Run the app
3. Select a route in the Admin map
4. The blue polyline should now appear following roads

## Troubleshooting

### If you still see errors, check Logcat:
1. Run the app
2. Open **Logcat** at the bottom of Android Studio
3. Filter by **"RouteAPI"**
4. Look for the actual error message

### Common Errors:

| Error | Solution |
|-------|----------|
| **Request denied** | API is not enabled. Follow steps above. |
| **Zero results** | The stops are not accessible by road. Try adding stops on actual roads. |
| **Invalid request** | Stop coordinates format is wrong. Check `stop.lat` and `stop.lng` values. |
| **Quota exceeded** | You've hit the free API limit. Upgrade your Google Cloud project. |

## If API Still Fails (Fallback Option)

If you can't enable the Directions API, the code will use a direct polyline connection (straight lines between stops) as a fallback. This is less professional but still functional.

---

**Once you enable the API, routes will follow actual roads automatically!** ✅

