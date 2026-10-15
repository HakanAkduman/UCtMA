import os
from pathlib import Path

from docling.datamodel import asr_model_specs
from docling.datamodel.base_models import InputFormat
from docling.datamodel.pipeline_options import (
    AsrPipelineOptions,
    VideoPipelineOptions,
)
from docling.document_converter import (
    AudioFormatOption,
    DocumentConverter,
    VideoFormatOption,
)
from docling.pipeline.asr_pipeline import AsrPipeline

from docling.chunking import HybridChunker
from docling_core.transforms.chunker.hierarchical_chunker import DocChunk


# DESTEKLENEN DOSYA FORMATLARI

PLAIN_TEXT_EXTENSIONS = {".csv", ".txt", ".md"}

AUDIO_EXTENSIONS = {
    ".wav",
    ".mp3",
    ".m4a",
    ".aac",
    ".ogg",
    ".flac",
}

VIDEO_EXTENSIONS = {
    ".mp4",
    ".avi",
    ".mov",
    ".mkv",
    ".webm",
}

DOCUMENT_EXTENSIONS = {
    ".pdf",
    ".docx",
    ".pptx",
    ".html",
    ".xlsx",
}

IMAGE_EXTENSIONS = {
    ".png",
    ".jpg",
    ".jpeg",
    ".tif",
    ".tiff",
    ".bmp",
    ".webp",
}

SUPPORTED_EXTENSIONS = (
    PLAIN_TEXT_EXTENSIONS
    | AUDIO_EXTENSIONS
    | VIDEO_EXTENSIONS
    | DOCUMENT_EXTENSIONS
    | IMAGE_EXTENSIONS
)



# DOCLING CONVERTER YAPILANDIRMASI

# Ses transkripsiyonu için Whisper modelini seçiyoruz.
# WHISPER_BASE, ilk aşamada kaynak tüketimi açısından
# daha makul bir başlangıç tercihidir.
asr_options = AsrPipelineOptions()
asr_options.asr_options = asr_model_specs.WHISPER_BASE

# Video için varsayılan kare örnekleme ayarlarını kullanıyoruz.
video_options = VideoPipelineOptions()

# Tek bir converter üzerinden standart belge, ses ve
# video dosyalarını işleyebiliyoruz.
converter = DocumentConverter(
    format_options={
        InputFormat.AUDIO: AudioFormatOption(
            pipeline_cls=AsrPipeline,
            pipeline_options=asr_options,
        ),
        InputFormat.VIDEO: VideoFormatOption(
            pipeline_options=video_options,
        ),
    }
)


# METİN PARÇALAMA

def _chunk_markdown(
    markdown_text: str,
    max_tokens: int = 400,
) -> list[dict]:
    """
    Düz metin dosyalarını basitçe parçalara ayırır.

    Not:
    Buradaki max_tokens parametresi gerçek tokenizer token sayısını
    değil, yaklaşık olarak token sayısını belirler.
    """

    words = markdown_text.split()
    chunk_size = max_tokens or 400

    chunks = []

    for index, start in enumerate(range(0, len(words), chunk_size)):
        chunk_text = " ".join(words[start:start + chunk_size])

        chunks.append({
            "chunkIndex": index,
            "text": chunk_text,
            "pageNumbers": [],
        })

    return chunks


# DOCLING CHUNK OLUŞTURMA

def _chunk_document(
    document,
    max_tokens: int = 400,
) -> list[dict]:
    """
    Docling tarafından oluşturulan belgeyi chunk'lara ayırır.

    PDF gibi sayfalı belgelerde sayfa numaralarını korumaya çalışır.
    Ses ve video belgelerinde sayfa numarası bulunmayabileceğinden
    pageNumbers boş liste olabilir.
    """

    chunker = HybridChunker(
        max_tokens=max_tokens or 400,
    )

    chunks = chunker.chunk(document)
    extracted_chunks = []

    for index, chunk in enumerate(chunks):
        chunk_text = chunker.serialize(chunk)

        # Chunk metadata bilgisinden sayfa numaralarını alıyoruz.
        doc_chunk = DocChunk.model_validate(chunk)

        page_numbers = set()

        if doc_chunk.meta and doc_chunk.meta.doc_items:
            for item in doc_chunk.meta.doc_items:
                if not hasattr(item, "prov") or not item.prov:
                    continue

                for provenance in item.prov:
                    if hasattr(provenance, "page_no"):
                        page_numbers.add(provenance.page_no)

        extracted_chunks.append({
            "chunkIndex": index,
            "text": chunk_text,
            "pageNumbers": sorted(page_numbers),
        })

    return extracted_chunks


# ANA DOSYA İŞLEME FONKSİYONU

def process_file_with_docling(
    file_path: str,
    conversion_type: str,
    max_tokens: int = 400,
) -> dict:
    """
    Dosyayı Docling ile işler.

    Desteklenen işlemler:
        CONVERT
            Dosyayı Markdown metnine dönüştürür.

        CONVERT_AND_CHUNK
            Dosyayı Markdown'a dönüştürür ve chunk'lara ayırır.

    Desteklenen dosyalar:
        Belgeler: PDF, DOCX, PPTX, HTML, XLSX
        Metin: CSV, TXT, MD
        Ses: WAV, MP3, M4A, AAC, OGG, FLAC
        Video: MP4, AVI, MOV, MKV, WEBM

    Dönen sonuç:
        {
            "convertedText": "...",
            "chunks": [...]
        }
    """

    # Dosya mevcut mu?
    if not os.path.isfile(file_path):
        raise FileNotFoundError(
            f"Dosya bulunamadı: {file_path}"
        )

    # İşlem tipi geçerli mi?
    if conversion_type not in {
        "CONVERT",
        "CONVERT_AND_CHUNK",
    }:
        raise ValueError(
            f"Desteklenmeyen işlem tipi: {conversion_type}"
        )

    # Dosya uzantısını belirle.
    extension = Path(file_path).suffix.lower()

    # Format destekleniyor mu?
    if extension not in SUPPORTED_EXTENSIONS:
        raise ValueError(
            f"Desteklenmeyen dosya formatı: {extension}"
        )

    # Düz metin dosyalarını doğrudan oku.
    # CSV, TXT ve Markdown için Docling kullanmak zorunda değiliz.
    if extension in PLAIN_TEXT_EXTENSIONS:
        with open(
            file_path,
            "r",
            encoding="utf-8-sig",
        ) as text_file:
            converted_text = text_file.read()

        if conversion_type == "CONVERT":
            chunks = []
        else:
            chunks = _chunk_markdown(
                converted_text,
                max_tokens,
            )

        return {
            "convertedText": converted_text,
            "chunks": chunks,
        }

    # PDF, Office belgeleri, ses ve video dosyalarını işle.
    #
    # Uzantıya göre ilgili pipeline seçimini Docling yapar.
    # Ses dosyaları ASR pipeline'ına, video dosyaları video
    # pipeline'ına yönlendirilir.
    conversion_result = converter.convert(
        Path(file_path)
    )

    # Dönüştürülmüş belgeyi Markdown'a çevir.
    converted_text = (
        conversion_result.document.export_to_markdown()
    )

    # Sadece dönüştürme istendiyse chunk oluşturma.
    if conversion_type == "CONVERT":
        return {
            "convertedText": converted_text,
            "chunks": [],
        }

    # CONVERT_AND_CHUNK için chunk'ları oluştur.
    extracted_chunks = _chunk_document(
        conversion_result.document,
        max_tokens,
    )

    return {
        "convertedText": converted_text,
        "chunks": extracted_chunks,
    }
