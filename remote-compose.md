   Remote Compose: Server-Side UI & Deployment Guide

  With androidx.compose.remote, the "server" doesn't actually run an active rendering engine. Instead, you create a Kotlin JVM script (or a small JVM backend app) that uses
  the remote-creation-compose module to define your UI and compile it into a binary .rc file.

  You then upload this .rc file to a standard CDN (like AWS CloudFront or Cloudflare), which your Android app downloads.
  ──────
  ## 1. The Server-Side Compose Code

  To build the UI document, you create a standard Kotlin JVM project. This is completely separate from your Android app.

  Dependencies needed in your JVM project (build.gradle.kts):

    dependencies {
        implementation("androidx.compose.remote:remote-creation-compose:1.0.0-alpha21")
        implementation("androidx.compose.remote:remote-creation-core:1.0.0-alpha21")
    }

  The UI Generator Script (OnboardingGenerator.kt):
  Here is the robust, "Kotlin-Compose way" to define your remote onboarding UI. Notice how it looks almost identical to standard Jetpack Compose!

    import androidx.compose.remote.creation.compose.RemoteDocumentBuilder
    import androidx.compose.remote.creation.core.RemoteDocument
    import androidx.compose.remote.creation.core.modifiers.*
    import androidx.compose.remote.creation.compose.components.*
    import java.io.File

    fun main() {
        // 1. Create a document builder
        val documentBuilder = RemoteDocumentBuilder()

        // 2. Define your UI just like standard Compose
        documentBuilder.build {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFFF3F4F6)) // Light gray background
            ) {
                Text(
                    text = "Welcome to Multiple Alarms!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937), // Dark text
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Set different alarms for every situation in your life. Server-driven updates mean we can change this text without an app update!",
                    fontSize = 16.sp,
                    color = Color(0xFF4B5563)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // SDUI Button - The interaction callback would need to be handled
                // by a predefined string tag your app listens for
                Button(
                    onClick = { /* In SDUI, you usually attach a string action tag here to trigger an event in the client app */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text(text = "Get Started", color = Color.White)
                }
            }
        }

        // 3. Serialize to the binary format
        val document: RemoteDocument = documentBuilder.document
        val binaryData: ByteArray = document.toByteArray()

        // 4. Save the payload to a file
        val outputFile = File("outputs/onboarding.rc")
        outputFile.parentFile.mkdirs()
        outputFile.writeBytes(binaryData)

        println("Successfully generated onboarding.rc (${binaryData.size} bytes)")
    }
  ──────
  ## 2. Deploying via GitHub Actions

  Since your SDUI file (onboarding.rc) is just a static binary asset, deploying it is incredibly cheap and easy. You don't need a Node.js backend or a Docker container. You
  just build the file and upload it to an S3 bucket attached to a CDN (CloudFront).

  Create .github/workflows/deploy_sdui.yml in your repository:

    name: Deploy SDUI Payload

    on:
      push:
        branches:
          - main
        paths:
          # Only run this action when the server-side UI code changes
          - 'server-ui-builder/**'

    jobs:
      build-and-deploy:
        runs-on: ubuntu-latest
        steps:
          - name: Checkout Repository
            uses: actions/checkout@v4

          - name: Set up JDK 21
            uses: actions/setup-java@v4
            with:
              java-version: '21'
              distribution: 'temurin'

          - name: Build Remote Document Payload (.rc)
            working-directory: ./server-ui-builder
            # Assuming you set up a gradle task called 'generateUiPayload' that runs OnboardingGenerator.kt
            run: ./gradlew generateUiPayload

          - name: Configure AWS Credentials
            uses: aws-actions/configure-aws-credentials@v4
            with:
              aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
              aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
              aws-region: us-east-1

          - name: Upload to S3
            working-directory: ./server-ui-builder
            # Uploads the generated .rc file to your S3 bucket
            run: |
              aws s3 cp outputs/onboarding.rc s3://your-sdui-bucket/ui/onboarding.rc --cache-control "max-age=3600"

          - name: Invalidate CloudFront Cache
            # Forces the CDN edge nodes to fetch the new file immediately so users see it
            run: |
              aws cloudfront create-invalidation --distribution-id ${{ secrets.CLOUDFRONT_DIST_ID }} --paths "/ui/onboarding.rc"

  ### How this architecture works:

  1. You make a change to the UI in OnboardingGenerator.kt and push to GitHub.
  2. GitHub Actions compiles the Kotlin script into an onboarding.rc binary.
  3. The action pushes the binary to AWS S3 and clears the CloudFront CDN cache.
  4. The next time a user opens the app, the Android app makes a request to https://d138545829.cloudfront.net/ui/onboarding.rc, downloads the new binary, and Jetpack
  Compose natively renders the new design without you ever deploying a new APK!
