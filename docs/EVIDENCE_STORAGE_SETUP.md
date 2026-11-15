# CASEZERO private evidence storage setup

This milestone stores investigation attachments in a **private Supabase Storage bucket**. The Java application sends uploads to Supabase from the server; the secret key must never be placed in HTML, JavaScript, GitHub, or a public repository.

## 1. Create the private bucket

1. Open the CASEZERO Supabase project.
2. Open **Storage** and choose **New bucket**.
3. Name it exactly: `casezero-evidence`.
4. Keep **Public bucket** switched OFF.
5. Set the maximum file size to **20 MB**.
6. If the dashboard asks for allowed MIME types, use:

```text
image/jpeg
image/png
image/webp
application/pdf
video/mp4
video/webm
video/quicktime
```

The app accepts JPG/JPEG, PNG, WEBP, PDF, MP4/M4V, WEBM and MOV files. It checks the file extension and basic file signature on the server as well.

## 2. Set the storage connection in PowerShell

Find the **Project URL** and a server-side **Secret key** in the Supabase project settings. Older projects may show the legacy `service_role` key instead.

Do not send the key to anyone or paste it into a source file. In the same PowerShell terminal where you run CASEZERO, enter:

```powershell
$env:CASEZERO_SUPABASE_URL = "https://YOUR_PROJECT_REF.supabase.co"
$env:CASEZERO_SUPABASE_SECRET_KEY = "PASTE_YOUR_SECRET_KEY_HERE"
$env:CASEZERO_SUPABASE_STORAGE_BUCKET = "casezero-evidence"
```

Replace the first two example values with your own values. Keep the key private. These variables apply to that PowerShell window; if you close it, set them again in the new terminal.

Then start the application:

```powershell
.\mvnw.cmd spring-boot:run
```

The database connection already used by CASEZERO is separate. Do not replace your existing PostgreSQL connection settings with the Storage URL.

## 3. Create the attachment metadata table if it is not created automatically

The new JPA entity may create this table automatically when the project's Hibernate schema setting is `update`. If it does not appear in Supabase after starting the app, open the Supabase SQL Editor and run the script in:

`database/create_investigation_attachments.sql`

## 4. Test the feature

1. Sign in to CASEZERO.
2. Open a published case and choose **Start an Investigation**.
3. Save the draft. The editor will reopen with the evidence uploader.
4. Upload one image, one PDF and one short video (each no larger than 20 MB).
5. Try pasting a copied image after clicking the paste instruction.
6. Reopen the draft and confirm the attachments are still listed.
7. Preview and download a file, then remove a test attachment.
8. Submit the draft and confirm its status changes to **PENDING REVIEW**. It should remain private.

If the upload page says storage is not configured, check that all three environment variables were set in the same PowerShell window that starts Spring Boot.

## Security notes

- The bucket must remain private.
- The secret key is used only by the Spring Boot server and bypasses Storage RLS. The controller therefore checks that the signed-in CASEZERO account owns the investigation and attachment before upload, preview, download or deletion.
- The current milestone does not include an admin review/approval screen. A submitted investigation is marked pending review; it is not automatically approved or made public.
- A 20 MB limit is deliberately used for this semester-project version so video uploads stay small and manageable.

## Spring Boot multipart limits

Keep the following settings in your local `src/main/resources/application.properties` file so Spring Boot accepts the supported 20 MB evidence files. Do not commit database passwords or Supabase secret keys from that file.

```properties
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=25MB
spring.servlet.multipart.file-size-threshold=2KB
```

These settings replace the older `MultipartConfigFactory` Java configuration, which is not available in this Spring Boot 4 setup.
