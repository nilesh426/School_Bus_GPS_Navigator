🚌 SchoolBusApp
🚸 Smart School Transportation & Safety System
📌 Overview
SchoolBusApp is a modern Android-based transportation management system designed to enhance student safety, operational efficiency, and real-time communication between schools and parents.

It provides a complete solution for:

📍 Live Bus Tracking
🧑‍🎓 Student Attendance Monitoring
💳 Secure Fee Payments
☁️ Real-time Data Synchronization
✨ Features
🧑‍🎓 1. Student Boarding & Attendance Tracking
Status	Indicator	Description
🔴 Not Marked	Red	Student has not boarded
🔵 Boarded	Blue	Student has boarded the bus
🟢 Dropped	Green	Student has reached destination

Key Functionalities:

⏱ Automatic timestamp logging (e.g., 10:30 AM)
🔄 Real-time status updates
🚫 Prevents duplicate actions
🎯 Smart button enable/disable logic
📍 2. Live Location Tracking
Feature	Description
🗺 Google Maps SDK	Displays real-time bus location
📡 GPS Tracking	Accurate tracking using Play Services
👨‍👩‍👧 Parent Access	Parents can monitor bus live
🏫 Admin Control	School can track routes
☁️ 3. Secure Backend & Sync
Technology	Purpose
🔐 Firebase Auth	Secure login system
🔄 Firestore	Real-time database
⚡ Realtime DB	Instant data sync
📦 Firebase BOM	Version stability
💳 4. Digital Fee Payment
Feature	Benefit
💰 Razorpay Integration	Easy payments
🔐 Secure Checkout	Safe transactions
📱 In-app Payments	No external apps required
🛠 Tech Stack
Category	Technology
💻 Language	Kotlin
🎨 UI	Material Components, ConstraintLayout
☁️ Backend	Firebase (Auth, Firestore, Realtime DB)
📍 Maps	Google Maps SDK
📡 Location	Play Services Location
💳 Payments	Razorpay SDK
🏗 Architecture	MVVM
📂 Project Structure
File	Description
BoardingStudentAdapter.kt	Handles attendance logic & UI updates
item_boarding_student.xml	Layout for student item UI
BoardingStudent.kt	Data model for student
🚀 Getting Started
📋 Prerequisites
Android Studio (Ladybug or newer)
Firebase Project
Google Cloud Project (Maps enabled)
Razorpay Account
⚙️ Installation
1️⃣ Clone Repository
git clone https://github.com/your-username/SchoolBusApp.git
2️⃣ Add Firebase Config

Place google-services.json inside:

app/
3️⃣ Add Maps API Key

In local.properties:

MAPS_API_KEY=your_api_key_here
4️⃣ Dependencies Used
firebase-bom:34.9.0
play-services-maps:18.2.0
razorpay:checkout:1.6.26
5️⃣ Run Project
Sync Gradle
Click ▶ Run
Use emulator or real device
📸 Screenshots

Add images like this:

/screenshots/login.png
/screenshots/boarding.png
/screenshots/map.png
/screenshots/payment.png
🔮 Future Enhancements
🔔 Push Notifications
📊 Attendance Analytics
🧠 AI Route Optimization
📴 Offline Mode
🧾 Payment Reports
🤝 Contributing
# Fork the repo
# Create a new branch
git checkout -b feature/YourFeature

# Commit changes
git commit -m "Add feature"

# Push to GitHub
git push origin feature/YourFeature

Then create a Pull Request 🚀

📄 License

Licensed under the MIT License

💡 Final Note

✔ Ideal for:

🎓 Final Year Projects
🏫 School Deployment
📱 Android + Firebase Learning
