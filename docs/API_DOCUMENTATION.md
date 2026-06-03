# API Endpoint Documentation

Base URL through gateway:

```text
http://localhost:8080
```

## 1. Login

```http
POST /auth/api/auth/login
Content-Type: application/json
```

Request:

```json
{
  "username": "employee1",
  "password": "password"
}
```

Response `200 OK`:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userId": 1,
  "role": "EMPLOYEE",
  "expiresInSeconds": 7200
}
```

## 2. View Leave Balance

```http
GET /leaves/api/leaves/employees/{employeeId}/balances
Authorization: Bearer <token>
```

Response `200 OK`:

```json
[
  { "leaveType": "CASUAL", "totalAllocated": 12, "used": 0, "remaining": 12 },
  { "leaveType": "SICK", "totalAllocated": 10, "used": 0, "remaining": 10 },
  { "leaveType": "PRIVILEGE", "totalAllocated": 15, "used": 0, "remaining": 15 }
]
```

## 3. Apply for Leave

```http
POST /leaves/api/leaves/applications
Authorization: Bearer <token>
Content-Type: application/json
```

Request:

```json
{
  "employeeId": 1,
  "leaveType": "CASUAL",
  "startDate": "2026-07-01",
  "endDate": "2026-07-02",
  "numberOfDays": 2,
  "reason": "Family function",
  "reportingManagerId": 100
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "employeeId": 1,
  "leaveType": "CASUAL",
  "startDate": "2026-07-01",
  "endDate": "2026-07-02",
  "numberOfDays": 2,
  "reason": "Family function",
  "reportingManagerId": 100,
  "status": "PENDING",
  "managerComments": null
}
```

Validation errors:

- `400 Bad Request` for past date, invalid date range, invalid manager, insufficient balance.
- `403 Forbidden` when employee applies for another employee.
- `409 Conflict` for overlapping leave.

## 4. Manager View Leave Requests

```http
GET /leaves/api/leaves/manager/requests?status=PENDING&employeeId=1&from=2026-07-01&to=2026-07-31
Authorization: Bearer <manager-token>
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "employeeId": 1,
    "leaveType": "CASUAL",
    "status": "PENDING"
  }
]
```

## 5. Approve Leave

```http
PATCH /leaves/api/leaves/manager/requests/1/decision
Authorization: Bearer <manager-token>
Content-Type: application/json
```

Request:

```json
{
  "decision": "APPROVED",
  "comments": "Approved"
}
```

Response `200 OK`: updated leave request with `status: APPROVED`.

## 6. Reject Leave

```http
PATCH /leaves/api/leaves/manager/requests/1/decision
Authorization: Bearer <manager-token>
Content-Type: application/json
```

Request:

```json
{
  "decision": "REJECTED",
  "comments": "Insufficient handover details"
}
```

Response `200 OK`: updated leave request with `status: REJECTED`.

## 7. View Leave History

```http
GET /leaves/api/leaves/employees/1/history?status=APPROVED&page=0&size=10
Authorization: Bearer <token>
```

Response: Spring Page JSON containing leave requests.

## 8. Notification History

```http
GET /notifications/api/notifications/1
```

Response:

```json
[
  {
    "userId": 1,
    "title": "Leave approved",
    "message": "Your leave request #1 has been approved",
    "createdAt": "2026-06-02T10:00:00Z"
  }
]
```
