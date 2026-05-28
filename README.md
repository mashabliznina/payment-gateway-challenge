# Payment Gateway
This project implements a payment gateway service that securely processes card payments through an external bank API.
The service validates payment requests, sends payment instructions to an external bank, stores processed payments, and allows retrieval of payment details by ID.

## How It Works
The service:
- Validates incoming payment requests;
- Sends payment details to an external bank provider;
- Processes authorized and rejected payment responses;
- Stores payment details in an in-memory repository;
- Returns masked card information (last four digits only);
- Allows retrieving payment details using a payment ID;
- Handles bank failures and persistence errors gracefully.

## Requirements
- JDK 21
- Docker

## Setup & Run Locally
### 1. Clone the repository
### 2. Clone the repository

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator

### 3. Running tests 
#### 3.1 To run tests with gradlew via terminal make sure you're using java 21:
```export JAVA_HOME=$(/usr/libexec/java_home -v 21)```
## API Documentation
For documentation openAPI is included, and it can be found under the following url: **http://localhost:8090/swagger-ui/index.html**

**Feel free to change the structure of the solution, use a different library etc.**