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

## 9. Summarize Text as Markdown

```http
POST /leaves/api/summaries
Authorization: Bearer <token>
Content-Type: application/json
```

Environment required by `leave-service`:

```text
OPENAI_API_KEY=<your-api-key>
OPENAI_MODEL=gpt-4.1-mini
```

Request:

```json
{
  "text": "Long text to summarize...",
  "focus": "Optional area to emphasize"
}
```

Response `200 OK`:

```json
{
  "summaryMarkdown": "## Summary\n\n- ...\n\n## Key Takeaways\n\n- ...",
  "model": "gpt-4.1-mini"
}
```

Errors:

- `400 Bad Request` when `text` is empty.
- `503 Service Unavailable` when `OPENAI_API_KEY` is not configured.
- `502 Bad Gateway` when the upstream OpenAI request fails.

## 10. AI Leave Request Review

Read-only manager assistant that reviews a leave request and recommends the next action. This API does not approve or reject the leave; the manager still uses the existing decision endpoint.

```http
POST /leaves/api/ai/leave-requests/{requestId}/review
Authorization: Bearer <manager-token>
Content-Type: application/json
```

Environment required by `leave-service`:

```text
OPENAI_API_KEY=<your-api-key>
OPENAI_MODEL=gpt-4.1-mini
```

Request:

```json
{
  "additionalContext": "Critical release week. Prefer approval only if there is no team coverage risk."
}
```

Response `200 OK`:

```json
{
  "recommendation": "APPROVE",
  "confidence": "HIGH",
  "reasoningMarkdown": "## Review\n\n- Employee has sufficient leave balance.\n- No overlapping team leaves were found.\n\n## Recommendation\n\nApprove the request.",
  "suggestedManagerComment": "Approved. Please complete handover before leave.",
  "model": "gpt-4.1-mini"
}
```

Possible `recommendation` values:

- `APPROVE`
- `REJECT`
- `NEEDS_MORE_INFO`

Possible `confidence` values:

- `LOW`
- `MEDIUM`
- `HIGH`

Errors:

- `403 Forbidden` when the caller is not a manager or the request does not belong to the manager's team.
- `404 Not Found` when the leave request does not exist.
- `503 Service Unavailable` when `OPENAI_API_KEY` is not configured.
- `502 Bad Gateway` when the upstream OpenAI request fails or does not return valid review JSON.
