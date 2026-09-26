# MDE by SMN — AI Face Studio

Android client for an authorized face-swap / identity-generation backend.

## Modes
- Fast Swap — InSwapper-compatible backend
- High Fidelity — multi-model backend
- Restore + Enhance — restoration/upscaling stage
- Generative Identity — identity-preserving generation backend

## Android app
The app lets you select a source face and target image, choose an engine, set identity strength, enable restoration/upscaling, and submit the job to a configurable inference endpoint.

## Backend contract
POST JSON to the configured URL:

`{"engine":"...","strength":0.8,"restore":true,"upscale":true,"source_image_base64":"...","target_image_base64":"..." }`

The backend can return its job/result response according to the inference provider. The current client reports the backend response; a provider-specific result-download adapter can be added next.

## Build
GitHub Actions builds the debug APK on pushes to `main` or manually from the Actions tab.

The heavy AI models run on the backend. Do not bundle proprietary model weights unless you have the required rights/licenses.
