FROM minio/minio:latest

# Рабочая директория для данных
WORKDIR /data

# Открываем порты:
# 9000 — API
# 9001 — Web UI
EXPOSE 9000 9001

# Переменные окружения (можешь поменять)
ENV MINIO_ROOT_USER=minioadmin
ENV MINIO_ROOT_PASSWORD=minioadmin

# Запуск MinIO
CMD ["server", "/data", "--console-address", ":9001"]
