import java.util.Scanner
import kotlin.system.exitProcess
import java.io.File
import java.time.Instant


// Import all members from AppState enum class
import AppState.*

// Import all members from UserLoginResult sealed class
import UserLoginResult.*

// Book class represents a book in the library
data class Book(
    var title: String, 
    var author: String, 
    var isbn: String, 
    var available: Boolean = true,
    var borrowedBy: String? = null,
    var dueDate: Instant? = null
)

//Users  are the readers
data class Users(
    val username: String,
    val pass: String,
    var numberOfBooksRead: Int
)

//Librarians
data class Librarians(
    val username: String,
    val pass: String
)

enum class AppState {
    LOGIN_SIGNUP,
    READER_DASHBOARD,
    LIBRARIAN_DASHBOARD,
    EXIT
}

sealed class UserLoginResult {
    object StayOnLogin : UserLoginResult() // Failed login/signup, stay on login screen
    object ExitApp : UserLoginResult()     // User chose to exit from the initial menu
    data class ReaderLoggedIn(val user: Users) : UserLoginResult()
    data class LibrarianLoggedIn(val librarian: Librarians) : UserLoginResult()
}



class Library {
    val userManager = LogInSignUp()
    var currentUsers = userManager.retrieveReaders().toMutableList()
    val books = loadBooksFromCSV("books.csv").toMutableList()
    private val scanner = Scanner(System.`in`)
    val reviews = mutableMapOf<String, MutableList<String>>()

    fun readBook(query: String, username: String): Boolean {
        val searchResults = books.filter { it.title.equals(query, ignoreCase = true) ||
                                      it.author.equals(query, ignoreCase = true) ||
                                      it.isbn.equals(query, ignoreCase = true) }
        
        if (searchResults.isEmpty()) {
            println("No books found for '$query'")
            return false 
        }

        println("\n--- Search Results ---")
        searchResults.forEachIndexed { index, book ->
            println("${index + 1}. Title: ${book.title}, Author: ${book.author}, ISBN: ${book.isbn}, Available: ${book.available}")
        }
        println("----------------------\n")
        
        val selectedBook: Book? = if (searchResults.size == 1) {
            searchResults.first()
        } else {
            println("Enter the number of the book you want to read (or 0 to cancel):")
            val choice = scanner.nextLine().toIntOrNull()
            if (choice != null && choice > 0 && choice <= searchResults.size) {
                searchResults[choice - 1]
            } else {
                println("Invalid selection or cancelled.")
                null
            }
        }

        if (selectedBook == null) {
            return false 
        }

        if (!selectedBook.available) {
            println("Sorry, '${selectedBook.title}' is currently not available.")
            return false
        }


        val currentUser = currentUsers.find { it.username.equals(username, ignoreCase = true) }
        if (currentUser == null) {
            println("Error: User '$username' not found in system.")
            return false 
        }


        println("+-------------------------------+")
        println("|           READING...          |") 
        println("+-------------------------------+")
        
        println("Congratulations! You have just finished reading '${selectedBook.title}'")
        currentUser.numberOfBooksRead++
        return saveReadChanges(currentUsers)
    }

    
    //Main Dashboard 
    fun MainDashBoard(username: String): AppState {
        val scanner = Scanner(System.`in`)
        var choice : Int? = null

        while(true){
            println("+-------------------------------+")
            println("|   LIBRARY MAIN DASHBOARD      |") 
            println("+-------------------------------+")
            println("|   (1) Browse Books            |")
            println("|   (2) Reader of the Week      |")
            println("|   (3) Back to Reader Menu     |") 
            println("+-------------------------------+")
            print("Select(1-3): ")

            try
            {
                choice = scanner.nextInt()
                if(choice !in 1..3){
                    println("Invalid choice. Please select a number between 1 and 3.")
                    scanner.nextLine()
                }
                else{
                    break
                }
            }
            catch(e: Exception){
                println("Error: Invalid input. Please enter a valid number.")
                scanner.nextLine()
            }
        }
        return when (choice) {
            1 -> LibraryMethods(username) 
            2 -> {
                readerOfTheWeek()
                READER_DASHBOARD 
            }
            3 -> READER_DASHBOARD 
            else -> READER_DASHBOARD 
        }
    }

    fun LibraryMethods(username: String): AppState {
        var choice : Int? = null
        while(true){
            println("+-------------------------------+")
            println("|   LIBRARY MANAGEMENT SYSTEM   |")
            println("+-------------------------------+")
            println("|  (1) Display all books        |")
            println("|  (2) Read a book              |")
            println("|  (3) Back                     |")
            println("+-------------------------------+")
            print("Select(1-3): ")

            try
            {
                choice = scanner.nextInt()
                scanner.nextLine()
                if(choice !in 1..3){
                    println("Invalid choice. Please select a number between 1 and 4.")
                }
                else{
                    break
                }
            }
            catch(e: Exception){
                println("Error: Invalid input. Please enter a valid number.")
                scanner.nextLine()
            }
        }

        when(choice){
            1 -> {
                displayBooks()
                return LibraryMethods(username) 
            }
            2 -> { 
                println("+-------------------------------+")
                println("|   LIBRARY MANAGEMENT SYSTEM   |")
                println("+-------------------------------+")
                print("Enter book title & author, or ISBN only to read: ")
                val query = scanner.nextLine()
                println("Searching for '$query'...")
                readBook(query, username)
                return LibraryMethods(username)
            }
            3 -> {
                return READER_DASHBOARD 
            }
            else -> {
                return LibraryMethods(username) 
            }
        }
    }

    fun readerOfTheWeek() {
        var reader = loadReaderOfTheWeek()
        println("+----------------------------------------------------------------+")
        println("|                      READER OF THE WEEK                        |")
        println("+----------------------------------------------------------------+")
        if(reader != null){
            println("CONGRATULATIONS TO ${reader.username} who read ${reader.numberOfBooksRead} books!")
        } else {
            println("No reader of the week found or an error occurred.")
        }
        println("Press Enter to continue...")
        scanner.nextLine() 
        readLine()
    }


    fun loadReaderOfTheWeek(): Users? {
        val allUsers = userManager.retrieveReaders() 
        return allUsers.maxByOrNull { it.numberOfBooksRead }
    }

    fun displayBooks() {
    // Always reload books from CSV to get the latest availability
    books.clear()
    books.addAll(loadBooksFromCSV("books.csv"))
    println("+----------------------------------------------------------------+")
    println("|                  LIBRARY MANAGEMENT SYSTEM                     |")    
    println("+----------------------------------------------------------------+")
    println("| Title                  | Author         | ISBN         | Avail |")
    println("+----------------------------------------------------------------+")
    books.forEach { book ->
        println("| ${book.title.padEnd(22)} | ${book.author.padEnd(13)} | ${book.isbn.padEnd(12)} | ${if (book.available) "true " else "false  "} |")
    }
    println("+----------------------------------------------------------------+")
}
    
    fun loadBooksFromCSV(filePath: String): List<Book> {
    val books = mutableListOf<Book>()
    try {
        File(filePath).useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    val title = parts[0]
                    val author = parts[1]
                    val isbn = parts[2]
                    val available = parts[3].trim().equals("TRUE", ignoreCase = true)
                    books.add(Book(title, author, isbn, available))
                }
            }
        }
    } catch (e: Exception) {
        println("Error reading CSV: ${e.message}")
    }
    return books
}

    private fun saveReadChanges(users: List<Users>): Boolean{
        val USER_CSV_FILE = File("user.csv")
        try{
            val lines = mutableListOf<String>()
            lines.add("username,password,numberOfBooksRead") 

            users.forEach { user ->
                lines.add("${user.username},${user.pass},${user.numberOfBooksRead}")
            }

            USER_CSV_FILE.writeText(lines.joinToString("\n")) 
            println("User data saved successfully.")
            return true
        } catch (e: Exception) {
            println("Error saving user data to CSV: ${e.message}")
            return false
        }
    }
    
    fun searchBook(query: String): List<Book> {
        val results = books.filter { it.title.equals(query, ignoreCase = true) ||
                                      it.author.equals(query, ignoreCase = true) ||
                                      it.isbn.equals(query, ignoreCase = true) }
        return if (results.isEmpty()) {
            println("No books found for '$query'")
            emptyList()
        } else {
            println("\n--- Search Results ---")
            results.forEach { println("Found: ${it.title} by ${it.author}, ISBN: ${it.isbn}, Available: ${it.available}") }
            println("----------------------\n")
            results
        }
    }

    fun saveBook(book: Book) {
        val file = File("books.csv")
        val line = listOf(
            book.title,
            book.author,
            book.isbn,
            book.available.toString(),
        ).joinToString(",")

        file.appendText("$line\n")
    }

    fun saveAllBooks() {
        try {
            File("books.csv").printWriter().use { out ->
                out.println("Title,Author,ISBN,Available") // header
                books.forEach { book ->
                    val availability = if (book.available) "TRUE" else "FALSE"
                    out.println("${book.title},${book.author},${book.isbn},$availability")
                }
            }
        } catch (e: Exception) {
            println("Error saving books: ${e.message}")
        }
    }

    fun addBook(book: Book) {
        books.add(book)
        saveAllBooks()
    }

    fun appendBookToCSV(book: Book, filePath: String) {
        try {
            File(filePath).appendText(
                "${book.title},${book.author},${book.isbn},${if (book.available) "TRUE" else "FALSE"}\n"
            )
        } catch (e: Exception) {
            println("Error appending book to CSV: ${e.message}")
        }
    }

    fun removeBookByISBN(isbn: String): Boolean {
        val removed = books.removeIf { it.isbn == isbn }
        if (removed) saveAllBooks()
        return removed
    }

    fun getBookByISBN(isbn: String): Book? {
        return books.find { it.isbn == isbn }
    }

    fun normalizeISBN(input: String): String {
        return if (input.startsWith("978-")) input else "978-$input"
    }

    fun saveBooksToCSV(books: List<Book>, filePath: String) {
        try {
            File(filePath).printWriter().use { out ->
                out.println("Title,Author,ISBN,Available") 
                for (book in books){
                    out.println("${book.title},${book.author},${book.isbn},${book.available.toString().uppercase()}")
                }
            }
        } catch (e: Exception) {
            println("Error writing to CSV: ${e.message}")
        }
    }

}



class LogInSignUp{
    var retrievedUsers = retrieveReaders().toMutableList()
    var retrievedLibrariansUsers = retrievedLibrarians().toMutableList()

    fun InitUserAndGetUser(): UserLoginResult  {
        val scanner = Scanner(System.`in`)
        var choice : Int? = null

        
        while(true){
            println("+-------------------------------------------------+")
            println("|           LIBRARY MANAGEMENT SYSTEM             |")
            println("+-------------------------------------------------+")
            println("|           (1) Log In                            |")
            println("|           (2) Sign Up                           |")
            println("|           (3) Exit                              |")
            println("+-------------------------------------------------+")
            print("Select(1-3): ")
            try
            {
                choice = scanner.nextInt()
                if(choice !in 1..3){
                    println("Invalid choice. Please select a number between 1 and 3.")
                    scanner.nextLine()
                }
                else{
                    break
                }
            }
            catch(e: Exception){
                println("Error: Invalid input. Please enter a valid number.")
                scanner.nextLine()
            }
        }
        
        return when(choice){
            1 -> { //Log In
                val userType = readerOrLibrarian()
                when(userType){
                    1-> logInReader() 
                    2-> logInLibrarian()
                    else ->{
                        println("You must specify what type of user you are")
                        StayOnLogin
                    }
                }
            }
            2 -> { //Sign UP
                println("Sign Up")
                val userType = readerOrLibrarian()
                when (userType) {
                    1 -> signUpReader() 
                    2 -> signUpLibrarian()
                    else ->{
                        println("You must specify what type of user you are")
                        StayOnLogin 
                    }
                }
        
            }
            3 -> ExitApp
            else -> StayOnLogin
        }
    }

    fun readerOrLibrarian(): Int {
        val scanner = Scanner(System.`in`)
        var choice : Int? = null

        while(true){
            println("+-------------------------------------------------+")
            println("|                  TYPE OF USER                   |")
            println("+-------------------------------------------------+")
            println("|                 (1) Reader                      |")
            println("|                 (2) Librarian                   |")
            println("|                 (3) Back                        |")
            println("+-------------------------------------------------+")
            print("Select(1-3): ")
            try
            {
                choice = scanner.nextInt()
                if(choice !in 1..3) println("Invalid choice. Please select a number between 1 and 2.")
                else return choice
            }
            catch(e: Exception){
                println("Error: Invalid input. Please enter a valid number.")
                scanner.nextLine()
            }
        }
    }

    fun logInReader(): UserLoginResult {
        val scanner = Scanner(System.`in`)
        println("+-------------------------------------------------+")
        println("|                  Log In                         |")
        println("+-------------------------------------------------+")
        print("Username: ")
        var username = scanner.nextLine()
        print("Password: ")
        var password = scanner.nextLine()

        val foundUser = retrievedUsers.find { it.username == username && it.pass == password }
        return if (foundUser != null){
            println("Reader login successful.")
            ReaderLoggedIn(foundUser) 
        } else {
            println("No existing account or incorrect credentials.")
            println("Sign up first or try again.")
            StayOnLogin 
        } 
    }


    fun logInLibrarian(): UserLoginResult {
        println("Log In Librarian")
        val scanner = Scanner(System.`in`)
        println("+-------------------------------------------------+")
        println("|                  Log In                         |")
        println("+-------------------------------------------------+")
        print("Username: ")
        var username = scanner.nextLine()
        print("Password: ")
        var password = scanner.nextLine()

        val foundLibrarian = retrievedLibrariansUsers.find { it.username == username && it.pass == password }
        return if (foundLibrarian != null){
            println("Librarian login successful.")
            LibrarianLoggedIn(foundLibrarian) //
        } else {
            println("No existing account or incorrect credentials.")
            println("Sign up first or try again.")
            StayOnLogin 
        }
    }

    fun signUpReader(): UserLoginResult {
        println("Sign Up Reader")
        val scanner = Scanner(System.`in`)  

        println("+-------------------------------------------------+")
        println("|                  Sign Up                        |")
        println("+-------------------------------------------------+")
        println("| Note: Credentials are case sensitive so you must|")
        println("| remember yours.                                 |")
        println("+-------------------------------------------------+")
        print("Set Username: ")
        var username = scanner.nextLine()

        print("Set Password: ")
        var password = scanner.nextLine()

        if (retrievedUsers.any { it.username == username }) { 
            println("Username already exists! Try a different one.")
            return StayOnLogin
        }   

        saveSignUpReaderAcc(username, password)
        val newUser = Users(username, password, 0)
        retrievedUsers.add(newUser) 
        println("Account created successfully for $username.")
        return ReaderLoggedIn(newUser)
    }

    fun signUpLibrarian(): UserLoginResult {
        println("Sign Up Librarian")
        val scanner = Scanner(System.`in`)

        println("+-------------------------------------------------+")
        println("|                  Sign Up                        |")
        println("+-------------------------------------------------+")
        println("| Note: Credentials are case sensitive so you must|")
        println("| remember yours.                                 |")
        println("+-------------------------------------------------+")
        print("Set Username: ")
        var username = scanner.nextLine()

        print("Set Password: ")
        var password = scanner.nextLine()
        
        if (retrievedLibrariansUsers.any { it.username == username }) { 
            println("Username already exists! Try a different one.")
            return StayOnLogin
        }

        saveSignUpLibrarianAcc(username, password)
        val newLibrarian = Librarians(username, password)
        retrievedLibrariansUsers.add(newLibrarian) 
        println("Account created successfully for $username.")
        return LibrarianLoggedIn(newLibrarian) 
    }

    fun retrieveReaders(): List<Users>{
        val users = mutableListOf<Users>()
        try {
            File("user.csv").useLines { lines ->
                lines.drop(1).forEach { line -> 
                    val parts = line.split(",")
                    if (parts.size >= 3) {
                        val username = parts[0]
                        val pass = parts[1]
                        val numberOfBooksRead = parts[2].toIntOrNull() ?: 0 
                        users.add(Users(username, pass, numberOfBooksRead))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error reading CSV: ${e.message}")
        }
        return users
    }

    fun retrievedLibrarians(): List<Librarians>{
        val users = mutableListOf<Librarians>()
        try {
            File("librarian.csv").useLines { lines ->
                lines.drop(1).forEach { line -> 
                    val parts = line.split(",")
                    if (parts.size >= 2) {
                        val username = parts[0]
                        val pass = parts[1]
                        users.add(Librarians(username, pass))
                    }
                }
            }
        } catch (e: Exception) {
            println("Error reading CSV: ${e.message}")
        }
        return users
    }

    fun isLogInReader(username : String, password : String): Boolean {
        return retrievedUsers.any { it.username == username && it.pass == password }
    }

    fun isLogInLibrarian(username : String, password : String): Boolean {
        return retrievedLibrariansUsers.any { it.username == username && it.pass == password }
    }

    fun saveSignUpReaderAcc(username: String, password: String) {
        val file = File("user.csv")
        val newLine = "$username,$password,0" 

        try {
            if (!file.exists()) {
                file.writeText("readerName,password,numberOfBooksRead\n")
            }

            if (!file.readText().endsWith("\n")) {
                file.appendText("\n")
            }   

            file.appendText("$newLine\n")
            println("User account for '$username' created successfully.")
        } catch (e: Exception) {
            println("Error saving account: ${e.message}")
        }
        return
    }

    fun saveSignUpLibrarianAcc(username: String, password: String) {
        val file = File("librarian.csv")
        val newLine = "$username,$password" 

        try {
            if (!file.exists()) {
                file.writeText("readerName,password\n")
            }

            if (!file.readText().endsWith("\n")) {
                file.appendText("\n")
            }   

            file.appendText("$newLine\n")
            println("User account for '$username' created successfully.")
        } catch (e: Exception) {
            println("Error saving account: ${e.message}")
        }
    }

}


class userReader(var username: String, var password: String){

    init {
        println("+---------------------------------------+")
        println("|       LIBRARY MANAGEMENT SYSTEM       |")
        println("+---------------------------------------+")
        println("Initialized reader dashboard...") 
    }

    fun readerPage(): AppState{ 
        println("+---------------------------------------+")
        println("WELCOME  $username!")
        println("+---------------------------------------+")
        
        val scanner = Scanner(System.`in`)
        var choice : Int? = null

        while (true) {
            println("+---------------------------------------+")
            println("|       LIBRARY MANAGEMENT SYSTEM       |")
            println("+---------------------------------------+")
            println("|  (1) View Library                     |")
            println("|  (2) Publish a Book                   |")
            println("|  (3) Borrow a Book                    |")
            println("|  (4) Return a Book                    |")
            println("|  (5) Rate a Book                      |")
            println("|  (6) View Borrowed Books              |")
            println("|  (7) View Reading History             |")
            println("|  (8) Add to Favorites                 |")
            println("|  (9) Leave a Review                   |")
            println("| (10) Manage your Account              |")
            println("| (11) Log Out                          |")
            println("+---------------------------------------+")
            print("Select(1-11): ")
            
            try {
                choice = scanner.nextInt()
                if(choice !in 1..11){
                    println("Invalid choice. Please select a number between 1 and 11.")
                    scanner.nextLine()
                }
            } catch(e: Exception){
                println("Error: Invalid input. Please enter a valid number.")
                scanner.nextLine()
            }
            
            when(choice) {
                1 -> {
                    println("Library")
                    val library = Library()
                    library.MainDashBoard(username)
                }
                2 -> publishBook()
                3 -> borrowBook()
                4 -> returnBook()
                5 -> rateBook()
                6 -> viewBorrowedBooks()
                7 -> viewReadingHistory()
                8 -> addToFavorites()
                9 -> leaveReview()
                10 -> {
                    val deleted = manageAccount(scanner)
                    if (deleted) {
                        println("Returning to main menu after account deletion...")
                        return LOGIN_SIGNUP
                    }
                }
                11 -> {
                    println("Logging out...")
                    return LOGIN_SIGNUP 
                }
                else -> { 
                    println("Invalid choice. Please select a number between 1 and 11.")
                }
            }
        }
    }

    private fun changeUsername(scanner: Scanner) {
        print("Enter new username: ")
        val newUsername = scanner.nextLine().trim()

        if (newUsername.isEmpty()) {
            println("Username cannot be empty.")
            return
        }

        val file = File("user.csv")
        val lines = file.readLines().toMutableList()
        val header = lines.firstOrNull() ?: return

        if (lines.drop(1).any { it.split(",")[0] == newUsername }) {
            println("Username \"$newUsername\" is already taken.")
            return
        }

        for (i in 1 until lines.size) {
            val parts = lines[i].split(",").toMutableList()
            if (parts[0] == username) {
                parts[0] = newUsername
                lines[i] = parts.joinToString(",")
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")

        updateUsernameInFile("library-data/borrowed_books.csv", newUsername)
        updateUsernameInFile("library-data/reading_history.csv", newUsername)
        updateUsernameInFile("library-data/ratings.csv", newUsername)
        updateUsernameInFile("library-data/favorites.csv", newUsername)
        updateUsernameInFile("library-data/reviews.csv", newUsername)

        println("Username changed from \"$username\" to \"$newUsername\" successfully.")
        username = newUsername
    }

    private fun updateUsernameInFile(filePath: String, newUsername: String) {
        val file = File(filePath)
        if (!file.exists()) return

        val lines = file.readLines().toMutableList()
        if (lines.isEmpty()) return

        val header = lines[0]
        val updatedLines = mutableListOf(header)

        for (i in 1 until lines.size) {
            val parts = lines[i].split(",").toMutableList()
            if (parts[0] == username) {
                parts[0] = newUsername
            }
            updatedLines.add(parts.joinToString(","))
        }

        file.writeText(updatedLines.joinToString("\n") + "\n")
    }

    private fun changePassword(scanner: Scanner) {
        print("Enter your current password: ")
        val currentPass = scanner.nextLine()

        val file = File("user.csv")
        val lines = file.readLines().toMutableList()
        val header = lines.firstOrNull() ?: return

        val userIndex = lines.indexOfFirst {
            it.split(",")[0] == username && it.split(",")[1] == currentPass
        }

        if (userIndex == -1) {
            println("Incorrect current password.")
            return
        }

        print("Enter new password: ")
        val newPass1 = scanner.nextLine()

        print("Confirm new password: ")
        val newPass2 = scanner.nextLine()

        if (newPass1 != newPass2) {
            println("Passwords do not match.")
            return
        }

        val parts = lines[userIndex].split(",").toMutableList()
        parts[1] = newPass1
        lines[userIndex] = parts.joinToString(",")
        file.writeText(lines.joinToString("\n") + "\n")

        println("Password changed successfully.")
    }

    private fun confirmDelete(scanner: Scanner): Boolean {
        println("WARNING: This will permanently delete your account and all associated data.")
        print("Type 'DELETE' to confirm or anything else to cancel: ")
        val confirmation = scanner.nextLine()
        return confirmation.equals("DELETE", ignoreCase = true)
    }

    private fun deleteAccount() {
        fun deleteFromFile(filePath: String) {
            val file = File(filePath)
            if (!file.exists()) return

            val lines = file.readLines()
            val filtered = lines.filterIndexed { i, line ->
                if (i == 0) true else !line.startsWith("$username,")
            }
            file.writeText(filtered.joinToString("\n") + "\n")
        }

        val userFile = File("user.csv")
        if (userFile.exists()) {
            val lines = userFile.readLines()
            val filtered = lines.filterIndexed { i, line ->
                if (i == 0) true else !line.split(",")[0].equals(username, ignoreCase = true)
            }
            userFile.writeText(filtered.joinToString("\n") + "\n")
        }

        deleteFromFile("library-data/borrowed_books.csv")
        deleteFromFile("library-data/reading_history.csv")
        deleteFromFile("library-data/ratings.csv")
        deleteFromFile("library-data/favorites.csv")
        deleteFromFile("library-data/reviews.csv")
    }

    private fun updateBooksReadInCSV(newCount: Int) {
        val file = File("user.csv")
        val lines = file.readLines().toMutableList()
        for (i in 1 until lines.size) {
            val parts = lines[i].split(",")
            if (parts.size >= 3 && parts[0] == username) {
                lines[i] = "${parts[0]},${parts[1]},$newCount"
                break
            }
        }
        file.writeText(lines.joinToString("\n") + "\n")
    }

    private fun getBooksRead(): Int {
        try {
            File("user.csv").useLines { lines ->
                lines.drop(1).forEach { line ->
                    val parts = line.split(",")
                    if (parts.size >= 3 && parts[0] == username) {
                        return parts[2].toIntOrNull() ?: 0
                    }
                }
            }
        } catch (_: Exception) {}
        return 0
    }

    private fun getBorrowedBooks(): List<Book> {
        val books = mutableListOf<Book>()
        val file = File("library-data/borrowed_books.csv")

        if (!file.exists()) return books

        file.useLines { lines ->
            val allLines = lines.toList()
            allLines.drop(1).forEach { line ->
                val parts = line.trim().split(",")
                if (parts.size >= 5) {
                    val fileUsername = parts[0].trim()
                    val currentUsername = username.trim()

                    if (fileUsername == currentUsername) {
                        val title = parts[1].trim()
                        val author = parts[2].trim()
                        val isbn = parts[3].trim()
                        val dateBorrowed = parts[4].trim()

                        books.add(Book(title, author, isbn))
                    }
                }
            }
        }

        return books
    }

    private fun getReadingHistory(): List<Book> {
        val books = mutableListOf<Book>()
        val file = File("library-data/reading_history.csv")
        if (!file.exists()) return books

        file.useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 4) {
                    val fileUsername = parts[0].trim()
                    val currentUsername = username.trim()

                    if (fileUsername.equals(currentUsername, ignoreCase = true)) {
                        val title = parts[1].trim()
                        val author = parts[2].trim()
                        val isbn = parts[3].trim()
                        books.add(Book(title, author, isbn))
                    }
                }
            }
        }

        return books
    }

    fun publishBook(){
        val scanner = Scanner(System.`in`)
        var bookName: String = ""
        var isbn: String = ""
        println("+-------------------------------------------+")
        println("|           PUBLISH CONTROL PANEL           |")
        println("+-------------------------------------------+")
        println("| Publish a Book                            |")
        println("+-------------------------------------------+")
        println()

        print("Enter a book name: ")
        bookName = scanner.nextLine()
        while(true){
            try{
                print("Enter isbn number (5-digit): ")
                isbn = scanner.next()
                if(isbn.length != 5){
                    println("Invalid isbn...")
                }
                else {break}
            }
            catch (e: Exception){
                println("Invalid Input...")
            }
        }
        println("Your book \"$bookName\" has been successfully added to the book approval list of librarians.")
    }

    fun borrowBook() {
    val timestamp = Instant.now().toString()
    val library = Library()
    library.displayBooks()
    val scanner = Scanner(System.`in`)
    print("Enter the ISBN of the book you want to borrow: ")
    val isbn = scanner.nextLine()
    val book = library.books.find { it.isbn == isbn && it.available }
    if (book != null) {
        File("library-data/borrowed_books.csv").appendText("$username,${book.title},${book.author},${book.isbn},$timestamp\n")
        File("library-data/reading_history.csv").appendText("$username,${book.title},${book.author},${book.isbn},$timestamp\n")
        val newCount = getBooksRead() + 1
        updateBooksReadInCSV(newCount)
        // Update availability
        book.available = false
        library.saveAllBooks()
        println("You have borrowed \"${book.title}\".")
    } else {
        println("Book not available or not found.")
    }
}

    fun returnBook() {
    println("+-------------------------------------------+")
    println("|               RETURN A BOOK               |")
    println("+-------------------------------------------+")

    val borrowedBooks = getBorrowedBooks()
    if (borrowedBooks.isEmpty()) {
        println("You have no borrowed books.")
        return
    }
    println("Your borrowed books:")
    borrowedBooks.forEachIndexed { idx, book ->
        println("${idx + 1}. ${book.title} by ${book.author} (ISBN: ${book.isbn})")
    }
    val scanner = Scanner(System.`in`)
    print("Enter the number of the book to return: ")
    val idx = scanner.nextInt() - 1
    scanner.nextLine()
    if (idx in borrowedBooks.indices) {
        val book = borrowedBooks[idx]
        val file = File("library-data/borrowed_books.csv")
        val lines = file.readLines().toMutableList()
        val filtered = lines.filterIndexed { i, line ->
            if (i == 0) true else {
                val parts = line.split(",")
                !(parts[0] == username && parts[3] == book.isbn)
            }
        }
        file.writeText(filtered.joinToString("\n") + "\n")
        // Update availability
        val library = Library()
        val bookInLibrary = library.books.find { it.isbn == book.isbn }
        if (bookInLibrary != null) {
            bookInLibrary.available = true
            library.saveAllBooks()
        }
        println("You have returned \"${book.title}\".")
    } else {
        println("Invalid selection.")
    }
}

    fun rateBook() {
        println("+-------------------------------------------+")
        println("|                RATE A BOOK                |")
        println("+-------------------------------------------+")

        val borrowedBooks = getBorrowedBooks()
        if (borrowedBooks.isEmpty()) {
            println("You haven't borrowed any book yet.")
            return
        }

        println("+----------------------------------------------------------------+")
        println("| Title                  | Author         | ISBN                 |")
        println("+----------------------------------------------------------------+")
        borrowedBooks.forEachIndexed { idx, book ->
            println("| ${book.title.padEnd(22)} | ${book.author.padEnd(13)} | ${book.isbn.padEnd(8)} |")
        }
        println("+----------------------------------------------------------------+")
        val scanner = Scanner(System.`in`)
        print("Enter the ISBN of the book you want to rate: ")
        val isbn = scanner.nextLine()
        if (borrowedBooks.none { it.isbn == isbn }) {
            println("You haven't borrowed a book with ISBN $isbn.")
            return
        }
        print("Enter your rating (1-5): ")
        val rating = scanner.nextLine()
        File("library-data/ratings.csv").appendText("$username,$isbn,$rating\n")
        println("Thank you for rating book $isbn with $rating stars!")
    }

    fun viewBorrowedBooks() {
        println("+-------------------------------------------+")
        println("|            YOUR BORROWED BOOKS            |")
        println("+-------------------------------------------+")

        val borrowedBooks = getBorrowedBooks()
        if (borrowedBooks.isEmpty()) {
            println("You have no borrowed books.")
        } else {
            borrowedBooks.forEach { book ->
                println("${book.title} by ${book.author} (ISBN: ${book.isbn})")
            }
        }
        println("Press Enter to continue...")
        readLine()
    }

    fun viewReadingHistory() {
        println("+-------------------------------------------+")
        println("|            YOUR READING HISTORY           |")
        println("+-------------------------------------------+")

        val history = getReadingHistory()
        if (history.isEmpty()) {
            println("No reading history yet.")
        } else {
            history.forEach { book ->
                println("${book.title} by ${book.author} (ISBN: ${book.isbn})")
            }
        }
        println("Press Enter to continue...")
        readLine()
    }

    fun addToFavorites() {
        println("+-------------------------------------------+")
        println("|            ADD TO FAVORITES               |")
        println("+-------------------------------------------+")

        val borrowedBooks = getBorrowedBooks()
        if (borrowedBooks.isEmpty()) {
            println("You haven't borrowed any book yet.")
            return
        }

        println("+----------------------------------------------------------------+")
        println("| Title                  | Author         | ISBN                 |")
        println("+----------------------------------------------------------------+")
        borrowedBooks.forEach { book ->
            println("| ${book.title.padEnd(22)} | ${book.author.padEnd(14)} | ${book.isbn.padEnd(8)} |")
        }
        println("+----------------------------------------------------------------+")

        val scanner = Scanner(System.`in`)
        print("Enter the ISBN of the book to add to favorites: ")
        val isbn = scanner.nextLine()
        if (borrowedBooks.none { it.isbn == isbn }) {
            println("You haven't borrowed a book with ISBN $isbn.")
            return
        }
        val book = borrowedBooks.first { it.isbn == isbn }
        File("library-data/favorites.csv").appendText("$username,${book.title},${book.author},${book.isbn}\n")
        println("\"${book.title}\" added to favorites.")
    }

    fun leaveReview() {
        println("+-------------------------------------------+")
        println("|               LEAVE A REVIEW              |")
        println("+-------------------------------------------+")
        
        val borrowedBooks = getBorrowedBooks()
        if (borrowedBooks.isEmpty()) {
            println("You haven't borrowed any book yet.")
            return
        }

        println("+----------------------------------------------------------------+")
        println("| Title                  | Author         | ISBN                 |")
        println("+----------------------------------------------------------------+")
        borrowedBooks.forEach { book ->
            println("| ${book.title.padEnd(22)} | ${book.author.padEnd(14)} | ${book.isbn.padEnd(8)} |")
        }
        println("+----------------------------------------------------------------+")

        val scanner = Scanner(System.`in`)
        print("Enter the ISBN of the book to review: ")
        val isbn = scanner.nextLine()
        if (borrowedBooks.none { it.isbn == isbn }) {
            println("You haven't borrowed a book with ISBN $isbn.")
            return
        }
        print("Enter your review: ")
        val review = scanner.nextLine()
        File("library-data/reviews.csv").appendText("$username,$isbn,$review\n")
        println("Thank you for creating a review for book $isbn!")
    }

    fun manageAccount(scanner: Scanner): Boolean { 
        while (true) {
            println("+-------------------------------------------+")
            println("|              MANAGE ACCOUNT               |")
            println("+-------------------------------------------+")
            println("| 1. Change Username                        |")
            println("| 2. Change Password                        |")
            println("| 3. Delete Account                         |")
            println("| 4. Back to Main Menu                      |")
            println("+-------------------------------------------+")
            print("Enter your choice: ")

            when (scanner.nextLine().trim()) {
                "1" -> changeUsername(scanner)
                "2" -> changePassword(scanner)
                "3" -> {
                    if (confirmDelete(scanner)) {
                        deleteAccount()
                        println("Account deleted. Exiting...")
                        return true
                    }
                }
                "4" -> return false
                else -> println("Invalid choice. Please try again.")
            }
        }
    }
}


class userLibrarian(var username: String, var pass: String, val library: Library) {

    var originalUsername = username

    
    fun librarianPage(): AppState {
        println("+---------------------------------------+")
        println("        WELCOME  $username!")
        println("+---------------------------------------+")

        val scanner = Scanner(System.`in`)

        while (true) {
            println("+-------------------------------------------+")
            println("|          LIBRARIAN CONTROL PANEL          |")
            println("+-------------------------------------------+")
            println("|  (1) Add a Book                           |")
            println("|  (2) Remove a Book                        |")
            println("|  (3) Update Book Info                     |")
            println("|  (4) View All Books                       |")
            println("|  (5) View Borrowed Books                  |")
            println("|  (6) View Overdue Books                   |")
            println("|  (7) Manage Your Account                  |")
            println("|  (8) View Ratings and Reviews             |")
            println("|  (9) Generate Library Reports             |")
            println("| (10) Log Out                              |")
            println("+-------------------------------------------+")
            print("Select (1-10): ")

            val choice = try {
                scanner.nextInt().also { scanner.nextLine() }
            } catch (e: Exception) {
                scanner.nextLine()
                println("Invalid input. Please enter a number between 1 and 10.")
                continue
            }

            when (choice) {
                1 -> {addBook(scanner)
                    library.saveAllBooks()}
                2 -> removeBook(scanner)
                3 -> updateBookInfo(scanner)
                4 -> { library.displayBooks()
                    }
                5 -> viewBorrowedBooks()
                6 -> viewOverdueBooks()
                7 -> {
                    val deleted = manageAccount(scanner)
                    if (deleted) return LOGIN_SIGNUP
                    save()
                }
                8 -> viewRatingsAndReviews()
                9 -> generateReports()
                10 -> {
                    println("Logging out...")
                    library.saveAllBooks()
                    return LOGIN_SIGNUP
                }
                else -> println("Invalid choice.")
            }
        }
    }

    fun addBook(scanner: Scanner) {
        println("+-------------------------------------------+")
        println("|               ADD A BOOK                  |")
        println("+-------------------------------------------+")
        print("Enter book title: ")
        val title = scanner.nextLine()
        print("Enter author: ")
        val author = scanner.nextLine()
        print("Enter ISBN: ")
        val isbn = scanner.nextLine()
        val normalizedIsbn = library.normalizeISBN(isbn)

        if (title.isBlank() || author.isBlank() || isbn.isBlank()) {
            println("Invalid input. All fields are required.")
            return
        }

        val newBook = Book(title, author, normalizedIsbn, available = true)
        library.addBook(newBook)
        println("Book added successfully.")
        library.saveBook(newBook)
        library.appendBookToCSV(newBook, "books.csv")
    }

    fun removeBook(scanner: Scanner) {
        println("+-------------------------------------------+")
        println("|             REMOVE A BOOK                 |")
        println("+-------------------------------------------+")

        print("Enter ISBN of book to remove: ")
        val isbn = scanner.nextLine()
        if (library.removeBookByISBN(isbn)) {
            println("Book removed.")
            library.saveAllBooks()
        } else {
            println("Book not found.")
        }
    }

    fun updateBookInfo(scanner: Scanner) {
        println("+-------------------------------------------+")
        println("|          UPDATE BOOK INFORMATION          |")
        println("+-------------------------------------------+")
        print("Enter ISBN of the book to update: ")
        val isbn = scanner.nextLine()
        val book = library.getBookByISBN(isbn)
        if (book == null) {
            println("Book not found.")
            return
        }

        print("New title (leave blank to keep '${book.title}'): ")
        val new_Title = scanner.nextLine()
        print("New author (leave blank to keep '${book.author}'): ")
        val new_Author = scanner.nextLine()

        if (new_Title.isNotBlank()) book.title = new_Title
        if (new_Author.isNotBlank()) book.author = new_Author

        println("Book info updated.")   
        library.saveAllBooks()
    }


    fun viewBorrowedBooks() {
        val file = File("library-data/borrowed_books.csv")
        if (!file.exists()) {
            println("No borrowed books.")
            return
        }
        val lines = file.readLines()
        if (lines.size <= 1) {
            println("No borrowed books.")
            return
        }
        println("+-------------------------------------------+")
        println("|        LIST OF BORROWED BOOKS             |")
        println("+-------------------------------------------+")
        println("| User         | Title           | ISBN     |")
        println("+-------------------------------------------+")
        lines.drop(1).forEach { line ->
            val parts = line.split(",")
            if (parts.size >= 4) {
                val user = parts[0]
                val title = parts[1]
                val isbn = parts[3]
                println("| ${user.padEnd(13)}| ${title.padEnd(16)}| ${isbn.padEnd(8)}|")
            }
        }
        println("+-------------------------------------------+")
    }

    fun viewOverdueBooks() {
        val now = Instant.now()
        val overdue = library.books.filter { !it.available && it.dueDate?.isBefore(now) == true }
        if (overdue.isEmpty()) {
            println("No overdue books.")
        } else {
            println("+-------------------------------------------+")
            println("|          LIST OF OVERDUE BOOKS            |")
            println("+-------------------------------------------+")
            overdue.forEach {
                println("${it.title} | ISBN: ${it.isbn}")
            }
        }
    }

    fun viewRatingsAndReviews() {
        val ratingsFile = File("library-data/ratings.csv")
        val reviewsFile = File("library-data/reviews.csv")

        println("+-------------------------------------------+")
        println("|          RATINGS AND REVIEWS              |")
        println("+-------------------------------------------+")

        if (ratingsFile.exists()) {
            println("Ratings:")
            val ratings = ratingsFile.readLines().drop(1)
            if (ratings.isEmpty()) println("No ratings available.")
            else ratings.forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val user = parts[0]
                    val isbn = parts[1]
                    val rating = parts[2]
                    println("User: $user | ISBN: $isbn | Rating: $rating")
                }
            }
        } else {
            println("No ratings available.")
        }

        if (reviewsFile.exists()) {
            println("\nReviews:")
            val reviews = reviewsFile.readLines().drop(1)
            if (reviews.isEmpty()) println("No reviews available.")
            else reviews.forEach { line ->
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val user = parts[0]
                    val isbn = parts[1]
                    val review = parts.subList(2, parts.size).joinToString(",")
                    println("User: $user | ISBN: $isbn | Review: $review")
                }
            }
        } else {
            println("No reviews available.")
        }
    }

    fun generateReports() {
    val totalBooks = library.books.size
    val availableBooks = library.books.count { it.available }

    // Borrowed books (from CSV)
    val borrowedFile = File("library-data/borrowed_books.csv")
    val borrowedLines = if (borrowedFile.exists()) borrowedFile.readLines().drop(1) else emptyList()
    val borrowedBooks = borrowedLines.size

    val overdueBooks = library.books.count { it.dueDate?.isBefore(Instant.now()) == true }

    // Readers
    val users = LogInSignUp().retrieveReaders()
    val totalUsers = users.size
    val mostActiveReader = users.maxByOrNull { it.numberOfBooksRead }

    // Most borrowed book
    val bookBorrowCounts = mutableMapOf<String, Int>()
    borrowedLines.forEach { line ->
        val parts = line.split(",")
        if (parts.size >= 4) {
            val isbn = parts[3]
            bookBorrowCounts[isbn] = bookBorrowCounts.getOrDefault(isbn, 0) + 1
        }
    }
    val mostBorrowedBookIsbn = bookBorrowCounts.maxByOrNull { it.value }?.key
    val mostBorrowedBook = library.books.find { it.isbn == mostBorrowedBookIsbn }

    // Ratings and reviews
    val ratingsFile = File("library-data/ratings.csv")
    val reviewsFile = File("library-data/reviews.csv")
    val totalRatings = if (ratingsFile.exists()) ratingsFile.readLines().drop(1).size else 0
    val totalReviews = if (reviewsFile.exists()) reviewsFile.readLines().drop(1).size else 0

    println("+-------------------------------------------+")
    println("|             LIBRARY REPORT                |")
    println("+-------------------------------------------+")
    println("Total Books: $totalBooks")
    println("Available Books: $availableBooks")
    println("Borrowed Books: $borrowedBooks")
    println("Overdue Books: $overdueBooks")
    println("Total Users: $totalUsers")
    println("Total Borrowed Records: $borrowedBooks")
    println("Total Ratings: $totalRatings")
    println("Total Reviews: $totalReviews")
    if (mostActiveReader != null) {
        println("Most Active Reader: ${mostActiveReader.username} (${mostActiveReader.numberOfBooksRead} books read)")
    }
    if (mostBorrowedBook != null) {
        println("Most Borrowed Book: ${mostBorrowedBook.title} (ISBN: ${mostBorrowedBook.isbn})")
    }
    println("+-------------------------------------------+")
}

    fun manageAccount(scanner: Scanner): Boolean {
        while (true) {
            println("+--------- MANAGE ACCOUNT ----------+")
            println("| 1. Change Username                |")
            println("| 2. Change Password                |")
            println("| 3. Delete Account                 |")
            println("| 4. Back to Main Menu              |")
            println("+-----------------------------------+")
            print("Enter choice: ")

            when (scanner.nextLine().trim()) {
                "1" -> changeUsername(scanner)
                "2" -> changePassword(scanner)
                "3" -> {
                    if (confirmDelete(scanner)) {
                        deleteAccount()
                        return true
                    }
                }
                "4" -> return false
                else -> println("Invalid choice.")
            }
        }
    }

    private fun changeUsername(scanner: Scanner) {
        print("Enter new username: ")
        val newUsername = scanner.nextLine().trim()
        if (newUsername.isBlank()) {
            println("Username cannot be blank.")
            return
        }
        val previous = username
        username = newUsername
        println("Username updated.")

        val oldUsername = originalUsername
        save(oldUsername)

        originalUsername = username
    }

    private fun changePassword(scanner: Scanner) {
        print("Enter new password: ")
        val newPass = scanner.nextLine().trim()
        if (newPass.isBlank()) {
            println("Password cannot be blank.")
            return
        }
        pass = newPass
        println("Password updated.")
        save()
    }

    private fun confirmDelete(scanner: Scanner): Boolean {
        print("Are you sure you want to delete your account? (yes/no): ")
        return scanner.nextLine().trim().lowercase() == "yes"
        save()
    }

    private fun deleteAccount() {
        val file = File("librarian.csv")
        val lines = file.readLines().filterNot { it.startsWith("$username,") }
        file.writeText(lines.joinToString("\n"))
        println("Account removed from system.")
        save()
    }

    private fun save(oldUsername: String = originalUsername) {
        val file = File("librarian.csv")
        val updated = file.readLines().map {
            val parts = it.split(",")
            if (parts[0] == oldUsername) "$username,$pass" else it
        }

        file.writeText(updated.joinToString("\n"))
    }
}

fun ExitPage(){
    println("+---------------------------------------------------+")
    println("|   THANK YOU FOR USING LIBRARY MANAGEMENT SYSTEM!  |")
    println("+---------------------------------------------------+")
    println("|   DEVELOPERS:   IRWEN FRONDA                      |")
    println("|                 AXEL AARON ARCELETA               |")
    println("|                 JOHN CARLO TRAJICO                |")
    println("|                                                   |")
    println("|   Year&Section: BSCS-2A                           |")
    println("|                                                   |")
    println("|   Project Details are uploaded in README.md file  |")
    println("+---------------------------------------------------+")
    exitProcess(0)
}


fun main() {
    val loginSignUp = LogInSignUp()
    var currentAppState: AppState = LOGIN_SIGNUP 
    val library = Library()
    var activeReader: Users? = null
    var activeLibrarian: Librarians? = null

    while (currentAppState != EXIT) { 
        when (currentAppState) {
            LOGIN_SIGNUP -> { 
                val result = loginSignUp.InitUserAndGetUser()
                when (result) { 
                    is ReaderLoggedIn -> {
                        currentAppState = userReader(result.user.username, result.user.pass).readerPage()
                    }
                    is LibrarianLoggedIn -> { 
                        currentAppState = userLibrarian(result.librarian.username, result.librarian.pass, library).librarianPage()
                    }
                    ExitApp -> { 
                        currentAppState = EXIT 
                    }
                    StayOnLogin -> {}
                }
            }

            READER_DASHBOARD -> {
                if (activeReader != null) {
                    currentAppState = userReader(activeReader.username, activeReader.pass).readerPage()
                    if (currentAppState == LOGIN_SIGNUP) {
                        activeReader = null
                    }
                } else {
                    
                    println("Error: Reader dashboard reached without active reader. Returning to login.")
                    currentAppState = LOGIN_SIGNUP
                }
            }
            LIBRARIAN_DASHBOARD -> {
                
                if (activeLibrarian != null) {
                    
                    currentAppState = userLibrarian(activeLibrarian.username, activeLibrarian.pass, library).librarianPage()
                    
                    if (currentAppState == LOGIN_SIGNUP) { 
                        activeLibrarian = null
                    }
                } else {
                    println("Error: Librarian dashboard reached without active librarian. Returning to login.")
                    currentAppState = LOGIN_SIGNUP
                }
            }
            EXIT -> {
            }
        }
    }
    ExitPage() 
}
