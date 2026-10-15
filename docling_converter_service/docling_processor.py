import ipaddress
import os
import socket
import tempfile
from pathlib import Path
from urllib.parse import urljoin, urlsplit

import requests
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

MAX_URL_BYTES = 10 * 1024 * 1024
MAX_URL_REDIRECTS = 5



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


def _validate_public_http_url(url: str) -> None:

    if not url or len(url) > 2048:
        raise ValueError("URL boş olamaz ve 2048 karakteri aşamaz.")
    parsed_url = urlsplit(url)
    scheme = parsed_url.scheme.lower()
    try:
        port = parsed_url.port
    except ValueError as error:
        raise ValueError("URL içindeki port numarası geçersiz.") from error

    if (
        scheme not in {"http", "https"}
        or not parsed_url.hostname
        or parsed_url.username is not None
        or parsed_url.password is not None
        or (port is not None and port != (443 if scheme == "https" else 80))
    ):
        raise ValueError(
            "URL yalnızca standart portları kullanan kullanıcı bilgisiz HTTP/HTTPS adresi olabilir."
        )

    try:
        addresses = socket.getaddrinfo(
            parsed_url.hostname,
            port or (443 if scheme == "https" else 80),
            type=socket.SOCK_STREAM,
        )
    except (OSError, ValueError) as error:
        raise ValueError(f"URL adresi çözümlenemedi: {parsed_url.hostname}") from error

    if not addresses or any(
        not ipaddress.ip_address(address[4][0]).is_global
        for address in addresses
    ):
        raise ValueError("Yerel veya özel ağ adreslerine erişime izin verilmiyor.")


def _download_url_html(url: str) -> bytes:
    current_url = url
    with requests.Session() as session:
        session.trust_env = False
        for redirect_count in range(MAX_URL_REDIRECTS + 1):
            _validate_public_http_url(current_url)

            with session.get(
                current_url,
                headers={"User-Agent": "UCTMA-Docling-Converter/1.0"},
                stream=True,
                timeout=(5, 30),
                allow_redirects=False,
            ) as response:
                if 300 <= response.status_code < 400:
                    location = response.headers.get("Location")
                    if (
                        response.status_code == 304
                        or not location
                        or redirect_count == MAX_URL_REDIRECTS
                    ):
                        raise ValueError("URL yönlendirmeleri geçersiz veya çok fazla.")
                    current_url = urljoin(current_url, location)
                    continue

                response.raise_for_status()
                content_type = response.headers.get("Content-Type", "").split(";", 1)[0].lower()
                if content_type not in {"text/html", "application/xhtml+xml"}:
                    raise ValueError(
                        f"URL bir HTML sayfası döndürmedi: {content_type or 'Content-Type belirtilmemiş'}"
                    )

                html_content = bytearray()
                for chunk in response.iter_content(chunk_size=64 * 1024):
                    html_content.extend(chunk)
                    if len(html_content) > MAX_URL_BYTES:
                        raise ValueError("URL sayfası izin verilen 10 MB boyut sınırını aşıyor.")

                return bytes(html_content)

    raise ValueError("URL yönlendirmeleri çözümlenemedi.")


def process_url_with_docling(
    url: str,
    conversion_type: str,
    max_tokens: int = 400,
) -> dict:

    if conversion_type not in {"CONVERT", "CONVERT_AND_CHUNK"}:
        raise ValueError(f"Desteklenmeyen işlem tipi: {conversion_type}")

    html_content = _download_url_html(url)
    temporary_path = None

    try:
        with tempfile.NamedTemporaryFile(
            mode="wb",
            suffix=".html",
            delete=False,
        ) as html_file:
            temporary_path = html_file.name
            html_file.write(html_content)

        return process_file_with_docling(
            file_path=temporary_path,
            conversion_type=conversion_type,
            max_tokens=max_tokens,
        )
    finally:
        if temporary_path and os.path.exists(temporary_path):
            os.remove(temporary_path)
