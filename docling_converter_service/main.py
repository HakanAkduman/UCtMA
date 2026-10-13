import os
import tempfile
import shutil
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import JSONResponse
from docling_processor import process_file_with_docling, SUPPORTED_EXTENSIONS 

app = FastAPI(
    title="Docling Converter Service",
    description="Docling kullanarak belgeleri markdown'a dönüştüren ve chunk'lara bölen servis.",
    version="1.0.0"
)

@app.get("/health", summary="Servis sağlık kontrolü")
async def health():
    return {"status": "healthy", "service": "Docling Converter Service"}

@app.post("/convert", summary="Desteklenen dosyaları Markdown formatına dönüştürür")
async def convert(file: UploadFile = File(...)):
    suffix = os.path.splitext(file.filename)[1].lower() #dosyaının uzantısını alıyourz
    if suffix not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Desteklenmeyen dosya formatı: {suffix}. Desteklenenler: {', '.join(SUPPORTED_EXTENSIONS)}"
        )

    # Dosyayı geçici dizine kaydediyoruz
    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    try:
        # kendi docling servisimizi çağırıyoruz
        result = process_file_with_docling(tmp_path, conversion_type="CONVERT")
        return JSONResponse(content={
            "filename": file.filename,
            "convertedText": result["convertedText"]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Dönüştürme sırasında hata oluştu: {str(e)}")
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)

@app.post("/convert-and-chunk", summary="Belgeyi dönüştürür ve chunklara böler")
async def convert_and_chunk(file: UploadFile = File(...), max_tokens: int = 400):


    suffix = os.path.splitext(file.filename)[1].lower()
    if suffix not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Desteklenmeyen dosya formatı: {suffix}. Desteklenenler: {', '.join(SUPPORTED_EXTENSIONS)}"
        )

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp)
        tmp_path = tmp.name

    try:
        
        result = process_file_with_docling(tmp_path, conversion_type="CONVERT_AND_CHUNK", max_tokens=max_tokens)
        return JSONResponse(content={
            "filename": file.filename,
            "convertedText": result["convertedText"],
            "chunks": result["chunks"]
        })
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Chunking sırasında hata oluştu: {str(e)}")
    finally:
        if os.path.exists(tmp_path):
            os.remove(tmp_path)