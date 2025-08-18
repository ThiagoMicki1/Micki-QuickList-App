I. Project Title / Description
Project Title: QuickList

Project Description: The QuickList app will allow users to create, edit, and share simple grocery lists with family or friends. Users can add, remove, or check off items on a list, and the view will be instantly updated for all users that are viewing the list.

II. Problem Solving
Grocery lists are often kept on paper, or users will use a note app to keep track of items they need to purchase. These are both easy to lose and cannot be shared with others in a convenient way. An app that allows multiple users to edit a single list in real-time makes this task much easier, so there is always the most recent list to reference, and users do not buy duplicate items.

III. Platform
The platform chosen for this app is a native Android app, which will be distributed on the Google Play Store. This will allow the app to take full advantage of the features of the Android OS and the devices themselves, as well as narrow the focus of the class for this development.

IV. Front-end / Back-end Support

Front-end:

Technology: The front-end technology stack will consist of the Java programming language. The user interface will be constructed with XML layout files for all screens and layout construction.

UI/UX: The app will have a very clean and minimalist design. Emphasis will be placed on a simple list that is easy to read and large buttons to add or check off items, with tappable targets that are easy to use on all sizes of devices.

Back-end:

Technology: The back-end services for this app will be provided by Firebase. This will be used to create a Firestore real-time database, as well as Firebase Authentication to allow users to sign in to the app. Firebase will be integrated with the specific Android SDK.

Data: The database will store user accounts and a collection of grocery lists. Each list will store the list name, which user created it, and an array of items, each with a status such as checked or unchecked.

V. Functionality

User Authentication: Users should be able to sign up for a new account and log in.

Create a New List: Users should be able to create a new grocery list and give it a name, such as “Weekly Shop” or “Pantry Restock.”

Add / Remove Items: Users should be able to add a new item to the list. Users should be able to remove an item when it is no longer needed.

Check Off Items: Items can be checked off as “checked” with a single tap, ideally with a visual indicator that the item has been purchased.

Real-time Sharing: Users should be able to share their list with another user by providing a unique link to the list or email. The second user should then be able to view the list, with all changes updated in real-time, and make changes to the list that are instantly reflected for all other users viewing or editing the same list.

VI. Design (Wireframes)
Screen 1: Login/Signup
Description: Simple screen with text fields for an email and password.

Screen 2: Home Dashboard
Description: A screen that displays a list of all the user’s grocery lists. The user can tap on an item to open it or a plus button to create a new list.

Screen 3: Grocery List View
Description: This is the primary screen for the app. It displays a list of items. Each item has a checkbox (or similar tap target) next to it. There is an input field and a button to add new items.
