# Inventory Manager - Android Application

**CS-499 Computer Science Capstone Enhancement Project**  
**Developer:** Tristen Bradney  
**Institution:** Southern New Hampshire University

## Project Overview

Inventory Manager is an Android mobile application designed to help users track and manage inventory items efficiently. This project has been enhanced as part of the CS-499 capstone to demonstrate advanced skills in software design, algorithms, and security.

## Features

### Core Functionality
- **User Authentication:** Secure login and registration system with encrypted password storage
- **Inventory Management:** Full CRUD operations (Create, Read, Update, Delete) for inventory items
- **Search & Sort:** Advanced search with binary search algorithm and multiple sorting options
- **Priority Tracking:** Automatic identification of low-stock items using priority queue
- **SMS Notifications:** Optional SMS alerts for low inventory levels (requires user permission)

### Enhanced Features (CS-499 Capstone)
- **MVVM Architecture:** Clean separation of concerns using Model-View-ViewModel pattern
- **Optimized Algorithms:** Binary search (O(log n)), efficient sorting, and priority queues
- **Database Security:** SQLCipher encryption, parameterized queries, bcrypt password hashing
- **Audit Trail:** Comprehensive logging of all data modifications
- **Input Validation:** Multi-layer validation to prevent malicious input

## Technical Stack

- **Language:** Java
- **Platform:** Android (API Level 21+)
- **Architecture:** MVVM (Model-View-ViewModel)
- **Database:** SQLite with SQLCipher encryption
- **Security:** bcrypt for password hashing, parameterized queries
- **UI Components:** RecyclerView, LiveData, ViewModel, Material Design

## Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK (API Level 21 or higher)
- JDK 8 or higher

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/tbradn/CS499-Computer-Science-Capstone.git
   ```

2. Open the project in Android Studio

3. Sync Gradle files (Android Studio will prompt you)

4. Build and run the application on an emulator or physical device

### Dependencies
Key dependencies are managed through Gradle:
- Android Architecture Components (ViewModel, LiveData)
- SQLCipher for Android (database encryption)
- bcrypt library (password hashing)
- Material Design Components

## Usage

### First Time Setup
1. Launch the application
2. Create a new account using the registration screen
3. Log in with your credentials

### Managing Inventory
1. **Add Item:** Click the '+' button and fill in item details (name, quantity, description)
2. **View Items:** Scroll through the inventory list on the main screen
3. **Search:** Use the search bar to find specific items
4. **Sort:** Tap the sort icon to organize by name, quantity, or priority
5. **Update Item:** Tap an item to edit its details
6. **Delete Item:** Swipe left on an item or use the delete button

### Priority Alerts
Items with quantity below the threshold will appear highlighted and in the priority queue for restocking.

## Enhancements (CS-499 Capstone)

### Enhancement 1: Software Design and Engineering
- Refactored from monolithic activity-based architecture to MVVM pattern
- Implemented Repository pattern for data abstraction
- Added LiveData for reactive UI updates
- Improved code modularity and testability

### Enhancement 2: Algorithms and Data Structures
- Implemented binary search for O(log n) search time
- Added multi-criteria sorting (name, quantity, priority, date)
- Integrated priority queue using min-heap for low-stock alerts
- Optimized database queries with proper indexing

### Enhancement 3: Databases and Security
- Replaced plaintext passwords with bcrypt hashing
- Implemented SQLCipher for database encryption (AES-256)
- Converted all queries to parameterized statements (SQL injection prevention)
- Added comprehensive audit trail for all data modifications
- Implemented multi-layer input validation

## Security Features

- **Password Security:** bcrypt hashing with salt (12 rounds)
- **Database Encryption:** AES-256 encryption via SQLCipher
- **SQL Injection Protection:** All queries use parameterized statements
- **Input Validation:** Client-side, ViewModel, and database-level validation
- **Session Management:** Secure session handling with automatic timeout
- **Audit Logging:** All data changes tracked with user and timestamp

## Testing

### Running Unit Tests
```bash
./gradlew test
```

### Test Coverage
- ViewModel logic tests
- Repository pattern tests
- Algorithm correctness tests (binary search, sorting, priority queue)
- Security validation tests (password hashing, SQL injection attempts)

## Performance

- Search: O(log n) with binary search on sorted data
- Sort: O(n log n) using optimized TimSort
- Priority queue: O(log n) insertion/extraction
- Database queries optimized with proper indexing

## Future Enhancements

Potential improvements for future iterations:
- Cloud sync for multi-device access
- Barcode scanning for quick item addition
- Analytics dashboard for inventory trends
- Export functionality (CSV, PDF reports)
- Two-factor authentication
- Biometric authentication support

## Known Issues

- SMS permissions must be manually enabled on Android 6.0+
- Database encryption key recovery not implemented (lost key = lost data)

## Contributing

This is a capstone project and not currently open for contributions. However, feedback and suggestions are welcome via the GitHub issues page.

## Academic Context

This project was developed as part of the CS-499 Computer Science Capstone at Southern New Hampshire University. It demonstrates proficiency in:
- Software design and engineering
- Algorithms and data structures
- Database management and security
- Mobile application development
- Professional documentation and communication

## Contact

**Tristen Bradney**
- Email: tbradney@yahoo.com
- GitHub: https://github.com/tbradn
- LinkedIn: https://www.linkedin.com/in/tristen-bradney-28a64721b/

## Acknowledgments

- Southern New Hampshire University Computer Science Department
- Android Developers Documentation
- OWASP Security Guidelines
- Open-source community for SQLCipher and bcrypt libraries

---

**Note:** This README describes the technical implementation. For detailed enhancement narratives and project reflections, please visit the [ePortfolio](https://tbradn.github.io).
