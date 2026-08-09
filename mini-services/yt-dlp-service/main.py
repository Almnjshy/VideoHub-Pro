"""
VideoHub Pro — yt-dlp Web Service (DEPRECATED)

This was the original web service approach for running yt-dlp.
It has been replaced by the embedded Chaquopy approach (resolver.py
running inside the Android app via JNI).

This file is kept for reference only.
"""

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import Optional
import yt_dlp

app = FastAPI(title="VideoHub Pro yt-dlp Service", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


class ResolveRequest(BaseModel):
    url: str
    cookies: Optional[dict] = None


@app.get("/health")
async def health():
    return {"status": "ok", "version": yt_dlp.version.__version__}


@app.post("/resolve")
async def resolve(req: ResolveRequest):
    try:
        opts = {'quiet': True, 'no_warnings': True, 'nocheckcertificate': True}
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(req.url, download=False)
        return {
            "ok": True,
            "title": info.get('title'),
            "thumbnail": info.get('thumbnail'),
            "duration": info.get('duration'),
            "formats": [
                {"id": f.get('format_id'), "ext": f.get('ext'),
                 "height": f.get('height'), "url": f.get('url')}
                for f in info.get('formats', [])
            ],
        }
    except Exception as e:
        raise HTTPException(status_code=400, detail=str(e))


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
