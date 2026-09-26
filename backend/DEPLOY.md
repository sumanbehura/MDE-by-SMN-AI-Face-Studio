# Deploying the MDE backend

The Android app needs a reachable HTTPS URL for the generate endpoint.

1. Use NVIDIA GPU infrastructure or another compatible GPU runtime.
2. Provide a properly licensed face-swap model at the path configured by MDE_SWAP_MODEL.
3. Build the image from backend/Dockerfile.
4. Run it with port 7860 exposed and set MDE_API_KEY.
5. Verify GET /health.
6. Put the HTTPS base URL ending in /generate into the Android app's Backend & API settings.

Hugging Face supports Docker Spaces and GPU-backed Spaces, but compute/GPU availability and pricing depend on the account and selected hardware. Do not put model weights or API keys into this Git repository.

For commercial deployment, obtain the appropriate model licenses before enabling the service.
