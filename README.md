I. Project Title & Description

Project: QuickList
Description: Real-time shared checklist app for Android. This app allows multiple users to create and share dynamic checklists instantly across devices while receiving updates through notifications. Week-7 final, frozen feature set.

II. Problem

Paper notes or generic notes apps are easily lost; hard to share; result in duplicate purchases. QuickList allows multiple people to edit the same list in real time to always have the latest version and eliminate duplicate purchases.

III. Platform

Target: Native Android app (AndroidX + Material Components)

Distribution: Google Play–ready (debug build for course submission)

Why Android: Deep OS integration, established UI patterns, tight scope

IV. Front-End / Back-End Support
A. Front-End

Language & UI: Java + XML layouts; Material toolbar; large tap targets

UX highlights:
a. Home: FAB create, search, sort (A–Z/Recent), pin, archive, per-list color & emoji; overflow menu (Open/Share/Pin/Archive/Color/Emoji); empty state; Sign out
b. List: add items with quantity (+/-), check/uncheck, swipe left = toggle, swipe right = delete with UNDO, optional A–Z sort, confetti when all items complete.

B. Back-End

Firebase Authentication (email/password)

Cloud Firestore for real-time sync (listeners on lists and items)

Data model (final):
lists/{listId} → { name, createdBy, members[], createdAt, pinned:boolean, archived:boolean, color:"#RRGGBB", emoji:String }
lists/{listId}/items/{itemId} → { text, createdBy, createdAt, checked:boolean, quantity:int }

C. Security (effective behavior)

Signed-in users only

Members may read/update a list and its items

Creator/owner may delete the list.

V. Functionality (Final Scope)

A. Auth: Sign up, log in, sign out.
B. Lists (Home): Create, open, pin, archive, search, sort; per-list color & emoji; overflow menu actions; empty state.
C. Items (List): Add text + quantity, check/uncheck; swipe to toggle/delete with UNDO; optional A–Z sort; confetti when 100% complete.
D. Sharing: Invite by email (membership updates).
E. Real-time: Firestore listeners keep views in sync.

VI. Design (Wireframes)

A. Screen 1: Login/Signup — email/password fields, primary action button.

B. Screen 2: Home — Material toolbar, list cards w/emoji/color stripe, FAB, search field, overflow menus

C. Screen 3: List — items w/checkbox, label, quantity (+/-); bottom add row; swipe gestures; share button

VII. Versioned Changelog (Summary)

v0.1 (Week 2): Problem, platform, stack, basic screens

v0.2 (Week 3): MVP — Auth, list create/open, add/check/delete items, share by email

v0.3 (Week 4): UX pass — Home (FAB, search/sort/pin/archive/color/emoji, overflow, sign out) and List (swipe + UNDO, quantities, A–Z sort, confetti)

v1.0 (Week 7 Final): Same feature set as Week 4 plus bug-fixes, docs, and packaging; frozen scope

VIII. How to Build & Run

Prereqs: Android Studio (JDK 17)

Firebase: add your app/google-services.json

Sync & Run: Gradle sync → run on device/emulator

Login flow: create account → login → create/open lists → share by email

IX. Known Limitations (Final)

Invite links (Dynamic Links) and widgets are out-of-scope for v1.0

Offline persistence left at defaults (online-first)

No server Cloud Functions; cascade deletes are performed client-side
