# Library Management System (Kotlin)

A project in compliance with CS221L-M / CS222-M (Programming Language: Design and Implementation)  
Submitted to: Sir Edward Cruz

---

## Group Members & Contributions

- **Fronda, Irwen**  
  Spearheaded the project and created the backbone of the program.

- **Arceleta, Axel Aaron**  
  Finished the functionalities and methods for the user/reader experience.

- **Trajico, John Carlo**  
  Finished the functionalities and methods for the librarian experience.

---

## Project Overview

This is a console-based Library Management System written in Kotlin. It allows users to register as readers or librarians, borrow and return books, rate and review books, and manage their accounts. Librarians can manage the book inventory and generate reports.

---

## Walkthrough / What to Expect

1. **Startup**  
   When you run the program, you will be greeted with a main menu where you can log in, sign up, or exit.

2. **User Types**  
   - **Reader/User:**  
     - Can browse and search books  
     - Borrow and return books  
     - Rate and review books  
     - View reading history and borrowed books  
     - Add books to favorites  
     - Manage their account (change username/password, delete account)
   - **Librarian:**  
     - Can add, remove, and update book information  
     - View all books, borrowed books, and overdue books  
     - View ratings and reviews  
     - Generate library reports  
     - Manage their account

3. **Navigation**  
   - Menus are navigated by entering the number corresponding to your choice.
   - Input is validated; invalid entries will prompt you to try again.

4. **Data Persistence**  
   - All user, book, and transaction data are stored in CSV files.
   - Changes (like borrowing a book or updating your account) are saved automatically.

5. **Exiting**  
   - You can log out or exit the program at any time from the main menus.

---

## How to Run

1. Make sure you have Kotlin installed.
2. Compile and run `Library.kt`:
   ```sh
   kotlinc Library.kt -include-runtime -d Library.jar
   java -jar Library.jar
   ```

---