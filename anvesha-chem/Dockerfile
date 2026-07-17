FROM continuumio/miniconda3:24.11.1-0 AS builder

RUN conda install -c conda-forge rdkit -y && \
    conda clean -afy

FROM python:3.11-slim

WORKDIR /app

COPY --from=builder /opt/conda /opt/conda

ENV PATH="/opt/conda/bin:$PATH"
ENV LD_LIBRARY_PATH="/opt/conda/lib:$LD_LIBRARY_PATH"

RUN apt-get update && \
    apt-get install -y --no-install-recommends default-jre-headless && \
    rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8001

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
CMD python -c "import httpx; exit(0 if httpx.get('http://localhost:8001/health').status_code==200 else 1)"

CMD ["uvicorn","app.main:app","--host","0.0.0.0","--port","8001"]