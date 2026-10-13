import json
import os
import time

import pika
import requests
from docling_processor import process_file_with_docling  # docling.py'den import ediyoruz

RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", 5672))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "guest")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "guest")
QUEUE_NAME = os.getenv("RABBITMQ_QUEUE_FILE", "file_processing_queue")
RECONNECT_DELAY_SECONDS = 5
RABBITMQ_HEARTBEAT_SECONDS = int(os.getenv("RABBITMQ_HEARTBEAT_SECONDS", 600))

BACKEND_CALLBACK_URL = os.getenv("BACKEND_CALLBACK_URL")

def send_callback(payload: dict):
    try:
        res = requests.post(BACKEND_CALLBACK_URL, json=payload, timeout=60)
        print(
            f"[CALLBACK] Status: {res.status_code}, "
            f"Response: {res.text[:500]}"
        )
        res.raise_for_status()
        return True
    except Exception as e:
        print(f"[CALLBACK ERROR] Backend'e ulaşılamadı: {e}")
        return False


def acknowledge_message(ch, delivery_tag):
    if ch.is_open:
        ch.basic_ack(delivery_tag=delivery_tag)


def reject_message(ch, delivery_tag):
    if ch.is_open:
        ch.basic_nack(delivery_tag=delivery_tag, requeue=True)

def callback(ch, method, properties, body):

    print(f"\n[*] RabbitMQ'dan mesaj alındı: {body.decode('utf-8')}")
    data = json.loads(body.decode('utf-8'))

    file_id = data.get("fileId")
    #file_path = data.get("filePath")

    file_path = data.get("filePath")

    conversion_type = data.get("conversionType")

    max_tokens = data.get("maxTokens") or 400

    print(f"\n[*] RabbitMQ'dan mesaj alındı -> File ID: {file_id}")

    try:
        result = process_file_with_docling(file_path, conversion_type, max_tokens)
        payload = {
            "fileId": file_id,
            "status": "COMPLETED",
            "convertedText": result.get("convertedText"),
            "chunks": result.get("chunks"),
            "errorMessage": None
        }
    except Exception as e:
        print(f"[ERROR] İşlem sırasında hata: {e}")
        callback_sent = send_callback({
            "fileId": file_id,
            "status": "FAILED",
            "convertedText": None,
            "chunks": [],
            "errorMessage": str(e)
        })
        if callback_sent:
            acknowledge_message(ch, method.delivery_tag)
        else:
            reject_message(ch, method.delivery_tag)
        return

    if send_callback(payload):
        acknowledge_message(ch, method.delivery_tag)
    else:
        reject_message(ch, method.delivery_tag)

def start_consumer():
    while True:
        conn = None
        try:
            creds = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
            params = pika.ConnectionParameters(
                host=RABBITMQ_HOST,
                port=RABBITMQ_PORT,
                credentials=creds,
                heartbeat=RABBITMQ_HEARTBEAT_SECONDS,
                blocked_connection_timeout=300,
            )
            conn = pika.BlockingConnection(params)
            channel = conn.channel()
            channel.queue_declare(queue=QUEUE_NAME, durable=True)
            channel.basic_qos(prefetch_count=1)
            channel.basic_consume(queue=QUEUE_NAME, on_message_callback=callback)

            print(f"[*] RabbitMQ Consumer hazır. '{QUEUE_NAME}' kuyruğu dinleniyor...")
            channel.start_consuming()
        except (pika.exceptions.AMQPConnectionError, pika.exceptions.ConnectionClosedByBroker) as e:
            print(
                f"[RABBITMQ] Bağlantı kapatıldı: {e}. "
                f"{RECONNECT_DELAY_SECONDS} saniye içinde yeniden bağlanılacak."
            )
            time.sleep(RECONNECT_DELAY_SECONDS)
        finally:
            if conn is not None and not conn.is_closed:
                conn.close()

if __name__ == "__main__":
    start_consumer()