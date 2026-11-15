# CASEZERO AI Community Moderator

## What it does

When a member submits a community investigation, CASEZERO screens the title, findings and source notes with OpenAI's `omni-moderation-latest` moderation endpoint.

- If the safety check passes and source notes are present, the submission is marked `APPROVED` and becomes visible on its linked public case page.
- If the model flags the text, the submission is marked `PENDING_REVIEW` and stays private in the existing administrator review queue.
- If the API key is missing, the API is unavailable, the response cannot be read, or source notes are missing, the system fails closed and keeps the submission private as `PENDING_REVIEW`.
- Obvious email addresses, Pakistan mobile numbers and CNIC-like number patterns are also held for review.

The existing administrator approval/rejection workflow for imported case reports is not changed.

## Configure the API key

The application does not contain an API key. Create your own API key in the OpenAI developer platform and keep it private. Never commit it to GitHub or paste it into source code.

Set the environment variable before starting CASEZERO.

**Windows PowerShell (current terminal only):**

```powershell
$env:CASEZERO_OPENAI_API_KEY="paste-your-own-key-here"
.\mvnw.cmd spring-boot:run
```

Replace the placeholder with your key locally. Do not include quotation marks inside the key.

If you start the application without the variable, community submissions remain private in the existing review queue. The rest of CASEZERO continues to work.

## Important limitations

This is AI content-safety screening, not a fact-checking or criminal-investigation model. A passing moderation result does not establish that a member's allegations, timeline, identity claims or conclusions are true. The UI must continue to describe community submissions as user research, not verified facts. Moderation decisions can be wrong; flagged items remain available to an administrator for review.

The current integration screens text fields only. It does not inspect uploaded evidence files or images. Do not describe it as image moderation.

## Test checklist

1. Start CASEZERO without the API key and submit a sourced investigation. It should remain private with status `PENDING_REVIEW`.
2. Configure a valid API key and restart the application.
3. Submit a sourced, non-harmful test investigation. It should pass the text moderation check and appear on the linked case page.
4. Submit a test containing content that the moderation model flags. It should remain private in the administrator review queue.
5. Confirm that imported case-report review and the existing case pages still work.

## Cost and account note

The moderation endpoint is offered without a per-token charge by OpenAI, but an OpenAI developer account and API key are still required. Account setup, verification and availability are controlled by OpenAI and cannot be completed by CASEZERO. Check current provider terms before relying on this for a public deployment.
