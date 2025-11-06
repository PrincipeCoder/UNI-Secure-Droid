# /Orchestrator/Dockerfile

FROM principecoder/uni-secure-droid:latest

WORKDIR /app

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY . .

CMD ["celery", "-A", "orchestrator", "worker", "--loglevel=info"]
