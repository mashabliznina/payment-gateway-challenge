# Payment Gateway
This project implements a payment gateway service that processes card payments through an external bank API.
The service validates payment requests, sends payment instructions to an external bank, stores processed payments, and allows retrieval of payment details by id.

## How It Works
The service:
- Validates incoming payment requests;
- Sends payment details to an external bank provider;
- Processes authorized and rejected payment responses;
- Stores payment details in an in-memory repository;
- Returns masked card information (last four digits only);
- Allows retrieving payment details using a payment ID;
- Handles bank failures.

## Requirements
- JDK 21
- Docker

## API Endpoints

### Process payment
```bash
POST /api/process-payment
```
Processes a card payment through the external bank.
Request Body Example
```bash
{
  "cardNumber": "2222405343248877",
  "expiryMonth": 11,
  "expiryYear": 2027,
  "currency": "GBP",
  "amount": 100,
  "cvv": "123"
}
```
### Get payment by id
```bash
GET /payment/{id}
```
Retrieves a previously processed payment.

## Setup & Run Locally
### 1. Clone the repository
### 2. Run Docker container to start Bank simulator
```bash
docker-compose up
```
### 3. Run the service locally
```bash
./gradlew bootRun
```
The application will start on: http://localhost:8070

### 4. Running tests 
```bash
./gradlew clean test
```
#### 3.1 To run tests with gradlew via terminal make sure you're using java 21:
```export JAVA_HOME=$(/usr/libexec/java_home -v 21)```