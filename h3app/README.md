# MDE by SMN × MiniMax H3 Android App

This is the zero-budget Android client for the MiniMax H3 Colab gateway.

1. Start `colab/MDE_MiniMax_H3_Gateway.ipynb` in Google Colab.
2. Copy the `gradio.live` URL printed by the notebook.
3. Open the APK and paste the URL.
4. Generate through the wrapped Hugging Face MiniMax H3 Space.

The gateway has a configurable daily usage limit and resets at midnight IST.

The T4 is not used to load MiniMax H3 itself because the current unquantized H3 deployment requires tens of GiB of model memory and is split across two Spaces.
