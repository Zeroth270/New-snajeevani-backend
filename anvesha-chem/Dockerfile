# ─────────────────────────────────────────────────────────────────────────────
# anvesha-chem Dockerfile
# Multi-stage build: installs RDKit from conda-forge and OPSIN jar at build time
# ─────────────────────────────────────────────────────────────────────────────

# ── Stage 1: Build environment with RDKit (via conda-forge miniforge) ─────────
FROM continuumio/miniconda3:24.11.1-0 AS builder

# Install RDKit from conda-forge (most reliable way to get a prebuilt wheel)
RUN conda install -c conda-forge rdkit -y && \
    conda clean -afy

# ── Stage 2: Final runtime image ──────────────────────────────────────────────
FROM python:3.11-slim AS runtime

WORKDIR /app

# Copy RDKit shared libs + Python bindings from conda env
COPY --from=builder /opt/conda/lib/python3.11/site-packages/rdkit /usr/local/lib/python3.11/site-packages/rdkit
COPY --from=builder /opt/conda/lib/libRDKit*.so* /usr/local/lib/
COPY --from=builder /opt/conda/lib/libboost_*.so* /usr/local/lib/
RUN ldconfig

# Install Java runtime (required by OPSIN via py2opsin)
RUN apt-get update && apt-get install -y --no-install-recommends \
    default-jre-headless \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY pyproject.toml .
RUN pip install --no-cache-dir -e ".[test]" || pip install --no-cache-dir \
    fastapi>=0.115.0 \
    "uvicorn[standard]>=0.32.0" \
    pydantic>=2.9.0 \
    pydantic-settings>=2.6.0 \
    httpx>=0.27.0 \
    tenacity>=9.0.0 \
    py2opsin>=2.0.0 \
    python-dotenv>=1.0.0 \
    python-multipart>=0.0.12

# Copy application source
COPY app/ ./app/
COPY .env.example .env

# Expose service port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "import httpx; httpx.get('http://localhost:8001/health')" || exit 1

# Run with uvicorn
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8001", "--workers", "2"]
