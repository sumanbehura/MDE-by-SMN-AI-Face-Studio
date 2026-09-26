# MDE by SMN AI Backend

FastAPI GPU inference service for the Android client.

## API
GET /health
POST /generate

Environment:
- MDE_API_KEY (optional)
- MDE_SWAP_MODEL (path to a model you are licensed to deploy)
- MDE_FACE_MODEL
- MDE_DET_SIZE

The Android client already sends the JSON contract expected by /generate.

## Model licensing
InsightFace code is MIT licensed, but pretrained model files have separate terms. The distributed pretrained models are stated by InsightFace to be for non-commercial research, and InsightFace asks users to contact them for licensing/support for inswapper models. Only deploy weights you are authorized to use.

The Dockerfile targets NVIDIA GPU infrastructure.
