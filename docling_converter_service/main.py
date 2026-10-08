import os 
import shutil
import tempfile
import json
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse, StreamingResponse
from docling.document_converter import DocumentConverter
from docling.chunking import HybridChunker
from docling.datamodel.document import ConversionResult


app=FastAPI(
    title = "Docling Converter Service",
    description= "Docling kullanarak belgeleri markdown a dönüştüren, isteğe göre chuncklara da bölen bir servis.",
    version="1.0.0"
)

try:
    converter = DocumentConverter()
    chunker = HybridChunker()
except Exception as e:
    print(f"Doclingden alınan modeller alınırken bir sıkıntı yaşandı: {str(e)}")


# çıktıların sunucuda geçici olarak saklanacağı ve indirileceği klasör
OUTPUT_DIR = "/tmp/parser_outputs"
os.makedirs(OUTPUT_DIR, exist_ok=True)

SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".pptx", ".html", ".csv", ".xlsx", ".txt"}

def validate_file_extension(filename:str) -> str:
    suffix= os.path.splitext(filename).lower #splitext ile uzantısını alıyoruz
    if suffix not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail = f"Unsupported file type. {suffix} files are not supported"
        )
    return suffix




@app.get("/health",summary="This endpoint is just to check whether service is up or not")
async def heath():
    return {"status": "healthy", "service": "Docling Converter Service"}



#bu endpoint desteklenen formattaki dosyaları markdown formatına dönüştürerek kaydeder ve response da dosya yolunu döndürür
@app.post("/convert",summary ="This endpoint is to convert supported files to markdown format")
async def convert(file:UploadFile=File(...)):

    parsed_doc = to_markdown(file)
    
    markdown_output = parsed_doc.document.export_to_markdown()
    
    output_filename = f"{os.path.splitext(file.filename)[0]}.md"
    output_filepath = os.path.join(OUTPUT_DIR, output_filename)

    with open(output_filepath, "w", encoding="utf-8") as f:
        f.write(markdown_output)

    return FileResponse(
        path=output_filepath, 
        filename=output_filename, 
        media_type="text/markdown"
    )

@app.post("/convert-and-chunk", summary="This endpoint is to convert markdown and separate to chunks ")
async def convert_and_chunk(file: UploadFile = File(...)):

    parsed_doc = to_markdown(file)
    
    chunks = chunker.chunk(parsed_doc.document)

    def chunk_generator():
        for i, chunk in enumerate(chunks):
            chunk_text = chunker.serialize(chunk)
            page_numbers = []
            
            if chunk.meta and chunk.meta.doc_items:
                page_numbers = list(set([
                    page.page_no for page in chunk.meta.doc_items.prov 
                    if hasattr(page, 'page_no')
                ]))
            
            chunk_data = {
                "chunk_index": i,
                "text": chunk_text,
                "metadata": {
                    "source_file": file.filename, 
                    "page_numbers": sorted(page_numbers)
                }
            }
            yield json.dumps(chunk_data, ensure_ascii=False) + "\n"

                                #ndjson ile birlikte her json objesini ayrı satır olarak dönüyorum
    return StreamingResponse(chunk_generator(), media_type="application/x-ndjson") 


def to_markdown(file: UploadFile) -> ConversionResult:

    suffix = os.path.splitext(file.filename).lower() # splitext ile dosyanın formatını alıyoruz
    if suffix not in SUPPORTED_EXTENSIONS:
        raise HTTPException(
            status_code=400, 
            detail=f"Unsupported file type: {suffix}. Supported types: {', '.join(SUPPORTED_EXTENSIONS)}"
        )

    with tempfile.NamedTemporaryFile(delete=False, suffix=suffix) as tmp:
        shutil.copyfileobj(file.file, tmp) #dosyayı işleme almak iç.in geçici olarak kaydediyoruz
        tmp_path = tmp.name

    try:
  
        conversion_result = converter.convert(tmp_path)
        return conversion_result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Belge analiz edilirken hata oluştu: {str(e)}")
    finally:
        if os.path.exists(tmp_path): #işlem bittikten sonra geçici dosyayı siliyoruz
            os.remove(tmp_path)
