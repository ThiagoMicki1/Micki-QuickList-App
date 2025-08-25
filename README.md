I. Project Title / Description 
Project Title: QuickList 
Due date for final submission: Feature set is frozen now; final deliverable is due in Week 7 (v1.0).

Project description: QuickList will allow users to create, edit, and share simple grocery lists with family or friends. Users will be able to add, remove, or check off items on a list, and the view will be instantly updated for all users that are viewing the list.

II. Problem Solving 
Grocery lists are typically created on paper or with a notes app in order to track items users need to purchase. Both of these options are easily lost and cannot be shared with others in a convenient manner. An app that allows multiple users to edit a single list in real-time simplifies this process so there is always the most recent list available and users are not buying duplicate items.

III. Platform 
The platform to be used for this app is a native Android app, which will be distributed through the Google Play Store. This will allow the app to make the most of the features of the Android OS and the devices themselves, as well as narrow the focus of the class for this development.

IV. Front-end / Back-end Support 

Front-end: 

Technology: The front-end technology stack will consist of the Java programming language. The user interface will be constructed with XML layout files for all screens and layout construction.

UI/UX: The app will have a very clean and minimalist design. Emphasis will be placed on a simple list that is easy to read and large buttons to add or check off items with tappable targets that are easy to use on all sizes of devices.

Back-end: 

Technology: The back-end services for this app will be provided by Firebase. This will be used to create a Firestore real-time database, as well as Firebase Authentication to allow users to sign in to the app. Firebase will be integrated with the specific Android SDK.

Data: The database stores user accounts and shared lists. Each list keeps metadata and membership, 
and items are stored as documents in a subcollection for real-time updates.

• lists/{listId}: { name, createdBy, members[], createdAt, pinned:boolean, archived:boolean, color:"#RRGGBB", emoji:String }
• lists/{listId}/items/{itemId}: { text, createdBy, createdAt, checked:boolean, quantity:int } 

V. Functionality 

User Authentication: Users should be able to sign up for a new account and log in.

Create a New List: Users should be able to create a new grocery list and give it a name, such as “Weekly Shop” or “Pantry Restock.”

Add / Remove Items: Users should be able to add a new item to the list. Users should be able to remove an item when it is no longer needed.

Check Off Items: Items can be checked off as “checked” with a single tap, ideally with a visual indicator that the item has been purchased.

Real-time Sharing: Users should be able to share their list with another user by providing a unique link to the list or email. The second user should then be able to view the list, with all changes updated in real-time, and make changes to the list that are instantly reflected for all other users viewing or editing the same list.

• Item Quantities: Each item stores a quantity with quick +/– controls
• Swipe Actions: Left = toggle checked; Right = delete with UNDO 
• Sort Items A–Z: Optional alphabetical sort in the list view 
• Home Enhancements: Search, Sort (A–Z/Recent), Pin/Archive lists, per-list color and emoji
• Visual Feedback: Empty states and confetti when a list reaches 100% complete

VI. Design (Wireframes) 
Screen 1: Login/Signup 
Description: Simple screen with text fields for an email and password.

Screen 2: Home Dashboard 
Description: A screen that displays a list of all the user’s grocery lists. The user can tap on an item to open it or a plus button to create a new list.

Screen 3: Grocery List View 
Description: This is the primary screen for the app. It displays a list of items. Each item has a checkbox (or similar tap target) next to it. There is an input field and a button to add new items.

VII. Week 4 Update — Changes Since Last Submission 
This week focused on user experience and list/item productivity improvements:

• Home (Lists): 
– Floating Action Button (FAB) to create lists 
– Search + Sort (A–Z / Recent), pin, archive 
– Per-list color and emoji 
– Row overflow menu: Open / Share / Pin / Archive / Set color / Set emoji
– Sign out in toolbar and an empty state for first-time users

• List (Items): 
– Item quantity (+/–) with default of 1 
– Swipe left = toggle checked; Swipe right = delete with UNDO
– Optional A–Z sort for items 
– Confetti shown when all items are checked 

• Data model extensions: 
– lists/{listId}: { name, createdBy, members[], createdAt, pinned:boolean, archived:boolean, color:"#RRGGBB", emoji:String }
– lists/{listId}/items/{itemId}: { text, createdBy, createdAt, checked:boolean, quantity:int } 

• Security & sharing: 
– Members can read/update; only the creator can delete a list
– Sharing implemented via email lookup (invite links planned earlier are deferred)

VIII. Versioned Changelog 
• v0.1 (Week 2): Outline and stack decisions (Android + Firebase), initial screens defined
• v0.2 (Week 3): MVP running — Auth, create/open lists, add/check/delete items, email-based sharing
• v0.3 (Week 4 — Current): UX pass — FAB, search/sort (A–Z/Recent), pin, archive, color/emoji, row menu, sign out; items get quantity (+/–), swipe gestures with UNDO, optional A–Z sort; confetti on 100% complete
• Planned (toward Week 7 Final): 
– v0.4–v0.6: Documentation polish, QA, and bug fixes only (feature freeze)
– v1.0 (Week 7): Final submission (same feature set as Week 4 with fixes)
