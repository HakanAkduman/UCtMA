import os
from docling.document_converter import DocumentConverter
from docling.chunking import HybridChunker
from docling_core.transforms.chunker.hierarchical_chunker import DocChunk

try:
    converter = DocumentConverter()
except Exception as e:
    print(f"Docling modeli yüklenirken hata oluştu: {str(e)}")


SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".pptx", ".html", ".csv", ".xlsx", ".txt"}

def process_file_with_docling(file_path: str, conversion_type: str, max_tokens: int = 400):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Dosya bulunamadı: {file_path}")

    conversion_result = converter.convert(file_path)
    
    # CONVERT
    if conversion_type == "CONVERT":
        return {
            "convertedText": conversion_result.document.export_to_markdown(),
            "chunks": []
        }

    # CONVERT_AND_CHUNK
    chunker = HybridChunker(max_tokens=max_tokens if max_tokens else 400)
    chunks = chunker.chunk(conversion_result.document)

    extracted_chunks = []
    for i, chunk in enumerate(chunks):
        chunk_text = chunker.serialize(chunk)
        doc_chunk = DocChunk.model_validate(chunk)

        page_numbers = set()

        if doc_chunk.meta and doc_chunk.meta.doc_items:

            for item in doc_chunk.meta.doc_items:

                if hasattr(item, "prov") and item.prov:

                    for prov_item in item.prov:

                        if hasattr(prov_item, "page_no"):
                            page_numbers.add(prov_item.page_no)

        extracted_chunks.append({
            "chunkIndex": i,
            "text": chunk_text,
            "pageNumbers": sorted(list(page_numbers))
        })

    return {
        "convertedText": conversion_result.document.export_to_markdown(),
        "chunks": extracted_chunks
    }