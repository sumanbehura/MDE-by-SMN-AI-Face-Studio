import base64, os
from pathlib import Path
from typing import Optional
import cv2, numpy as np
from fastapi import FastAPI, HTTPException, Header
from fastapi.responses import JSONResponse
from pydantic import BaseModel

APP_NAME="MDE by SMN AI Backend"; API_KEY=os.getenv("MDE_API_KEY","")
MODEL_PATH=os.getenv("MDE_SWAP_MODEL","/models/inswapper_128.onnx")
DET_SIZE=int(os.getenv("MDE_DET_SIZE","640"))
app=FastAPI(title=APP_NAME,version="0.1.0"); _analyzer=None; _swapper=None

class RequestBody(BaseModel):
    engine:str="Fast Swap — InSwapper"; strength:float=0.8; restore:bool=True; upscale:bool=False
    source_image_base64:str; target_image_base64:str

def check_key(a:Optional[str]):
    if API_KEY and a!=f"Bearer {API_KEY}": raise HTTPException(401,"Invalid API key")
def dec(s):
    try:
        x=cv2.imdecode(np.frombuffer(base64.b64decode(s),np.uint8),cv2.IMREAD_COLOR)
        if x is None: raise ValueError()
        return x
    except Exception: raise HTTPException(400,"Invalid image data")
def enc(x):
    ok,b=cv2.imencode(".jpg",x,[int(cv2.IMWRITE_JPEG_QUALITY),95])
    if not ok: raise HTTPException(500,"Could not encode result")
    return base64.b64encode(b.tobytes()).decode()

def load_engine():
    global _analyzer,_swapper
    if _analyzer is not None:return
    if not Path(MODEL_PATH).exists():
        raise RuntimeError(f"Licensed swap model not found at {MODEL_PATH}")
    from insightface.app import FaceAnalysis
    from insightface.model_zoo import get_model
    providers=["CUDAExecutionProvider","CPUExecutionProvider"]
    _analyzer=FaceAnalysis(name=os.getenv("MDE_FACE_MODEL","buffalo_l"),providers=providers)
    _analyzer.prepare(ctx_id=0,det_size=(DET_SIZE,DET_SIZE))
    _swapper=get_model(MODEL_PATH,providers=providers)

@app.get("/health")
def health(): return {"ok":True,"service":APP_NAME,"model_configured":Path(MODEL_PATH).exists()}

@app.post("/generate")
def generate(body:RequestBody,authorization:Optional[str]=Header(default=None)):
    check_key(authorization); src=dec(body.source_image_base64); dst=dec(body.target_image_base64)
    try: load_engine()
    except Exception as e: raise HTTPException(503,str(e))
    sf=_analyzer.get(src); df=_analyzer.get(dst)
    if not sf: raise HTTPException(422,"No face detected in source image")
    if not df: raise HTTPException(422,"No face detected in target image")
    source=max(sf,key=lambda f:f.bbox[2]-f.bbox[0]); result=dst.copy()
    strength=max(0.0,min(1.0,body.strength))
    for face in df:
        swapped=_swapper.get(result,face,source,paste_back=True)
        result=swapped if strength>=.999 else cv2.addWeighted(dst,1-strength,swapped,strength,0)
    return JSONResponse({"ok":True,"image_base64":enc(result),"mime_type":"image/jpeg","engine":body.engine})
