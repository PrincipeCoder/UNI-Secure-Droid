# /DynamicAnalyzer/Dockerfile
# RNF-4: Imagen aislada para sandbox

FROM principecoder/uni-secure-droid:latest

# Instalar Android SDK y herramientas
RUN apt-get update && apt-get install -y \
    wget \
    unzip \
    libgl1-mesa-glx \
    libpulse0 \
    && rm -rf /var/lib/apt/lists/*

# Descargar Android SDK command-line tools
RUN mkdir -p /opt/android-sdk/cmdline-tools && \
    cd /opt/android-sdk/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip && \
    unzip commandlinetools-linux-9477386_latest.zip && \
    rm commandlinetools-linux-9477386_latest.zip && \
    mv cmdline-tools latest

ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator

# Aceptar licencias y descargar componentes necesarios
RUN yes | sdkmanager --licenses && \
    sdkmanager "platform-tools" "platforms;android-30" "system-images;android-30;google_apis;x86_64" "emulator"

# Crear AVD para sandbox
RUN echo "no" | avdmanager create avd -n malware_sandbox -k "system-images;android-30;google_apis;x86_64" -d "pixel"

# Copiar código del analizador dinámico
WORKDIR /app
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt

COPY . .

# RNF-4: Usuario no privilegiado
RUN useradd -m -u 1000 sandbox && \
    chown -R sandbox:sandbox /app /opt/android-sdk

USER sandbox

CMD ["celery", "-A", "analyzer", "worker", "--loglevel=info", "-Q", "dynamic_analysis"]
