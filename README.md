# CASEZERO

**CASEZERO** is a Java-based web application designed to organize and present publicly available information about unresolved cases in Pakistan.

The platform focuses on cases involving missing persons, unidentified persons or human remains, and unsolved crimes. Its goal is to make relevant public information easier to explore and understand.

## Project Objectives

- Organize publicly available information about unresolved cases in Pakistan.
- Provide an accessible interface for exploring case-related information.
- Support secure user registration and authentication.
- Encourage responsible investigation using publicly available sources.

## Technology Stack

| Technology | Purpose |
|---|---|
| Java 17 | Main programming language |
| Spring Boot 4.0.8 | Backend application framework |
| Spring MVC | Web request handling |
| Thymeleaf | Server-side page rendering |
| HTML, CSS and JavaScript | Frontend development |
| Spring Security | Authentication and security |
| Jakarta Bean Validation | Input validation |
| Spring Data JPA | Database operations |
| PostgreSQL | Relational database |
| Maven | Dependency management and build |

## Project Structure

```text
casezero/
├── src/
│   ├── main/
│   │   ├── java/          # Java application source code
│   │   └── resources/
│   │       ├── templates/ # Thymeleaf HTML pages
│   │       ├── static/    # CSS, JavaScript and other static files
│   │       └── application.properties
│   └── test/              # Application tests
├── pom.xml                # Maven dependencies and configuration
├── mvnw
└── mvnw.cmd
```

## Running the Project Locally

### Requirements

- Java Development Kit (JDK) 17
- Git
- Visual Studio Code (recommended)

### Instructions

1. Clone or download this repository.
2. Open the project folder in Visual Studio Code.
3. Configure the PostgreSQL database connection in `application.properties`.
4. Open the integrated terminal in the project directory.
5. Run the application:

**Windows PowerShell:**

```powershell
.\mvnw.cmd spring-boot:run
```

6. Open the application in your browser:

```text
http://localhost:8080
```

## Running Tests

To run the project's tests, execute:

```powershell
.\mvnw.cmd clean test
```

## Database Configuration and Security

The application requires a PostgreSQL database connection.

Database credentials and other sensitive configuration values should be kept private and must not be committed to the public GitHub repository.

## Project Status

CASEZERO is an ongoing academic semester project developed as part of the Software Engineering curriculum.

New features, improvements, and testing will be added as development progresses.

---

**Note:** CASEZERO is intended to organize publicly available information. Information about unresolved cases should be handled responsibly and supported by credible sources.