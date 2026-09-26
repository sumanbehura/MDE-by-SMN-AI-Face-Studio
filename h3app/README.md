# MDE by SMN × MiniMax H3 Android App

This Android client opens the public MiniMax H3 Hugging Face Space directly.

## Architecture

```
Android app
    ↓
Hugging Face MiniMax H3 Space
    ↓
Generated video + soundtrack
    ↓
Android app
```

There is **no Google Colab T4, gateway, proxy, IP rotation, or intermediate server** in this version.

The H3 Space itself is a large split ZeroGPU deployment: the generator and conditioner remain on Hugging Face. The Android app does not download or run the H3 model locally.

## Use

1. Install the APK.
2. The official Space URL is prefilled.
3. Tap **Open MiniMax H3**.
4. Use the H3 interface normally.
5. Select/upload keyframes if needed, enter the prompt, configure generation options, and generate.

The app uses an Android WebView so the Space's current Gradio interface remains usable without us having to duplicate every H3 control in native Android code.

## Important

Hugging Face controls the Space's actual ZeroGPU capacity and quota. The app must not claim an exact official remaining HF quota unless Hugging Face exposes that information to the client.

Reference Space: https://huggingface.co/spaces/observantdistressed/minimax-h3
