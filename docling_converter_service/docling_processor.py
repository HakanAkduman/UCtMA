import os
from docling.document_converter import DocumentConverter
from docling.chunking import HybridChunker
from docling_core.transforms.chunker.hierarchical_chunker import DocChunk

try:
    converter = DocumentConverter()
except Exception as e:
    print(f"Docling modeli yüklenirken hata oluştu: {str(e)}")


SUPPORTED_EXTENSIONS = {".pdf", ".docx", ".pptx", ".html", ".csv", ".xlsx", ".txt", ".md"}
PLAIN_TEXT_EXTENSIONS = {".csv", ".txt", ".md"}


def _chunk_markdown(markdown_text: str, max_tokens: int) -> list[dict]:
    """Split plain Markdown into simple word-based chunks.

    Markdown files do not need Docling conversion. The word limit is an
    approximation of the tokenizer limit used by HybridChunker and keeps the
    existing callback shape for CONVERT_AND_CHUNK.
    """
    words = markdown_text.split()
    chunk_size = max_tokens or 400

    return [
        {
            "chunkIndex": index,
            "text": " ".join(words[start:start + chunk_size]),
            "pageNumbers": [],
        }
        for index, start in enumerate(range(0, len(words), chunk_size))
    ]

def process_file_with_docling(file_path: str, conversion_type: str, max_tokens: int = 400):
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"Dosya bulunamadı: {file_path}")

    extension = os.path.splitext(file_path)[1].lower()
    if extension in PLAIN_TEXT_EXTENSIONS:
        with open(file_path, "r", encoding="utf-8") as markdown_file:
            plain_text = markdown_file.read()

        return {
            "convertedText": plain_text,
            "chunks": _chunk_markdown(plain_text, max_tokens)
            if conversion_type == "CONVERT_AND_CHUNK"
            else [],
        }

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