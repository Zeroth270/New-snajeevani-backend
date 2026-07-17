-- =================================================================
-- Anvesha — PostgreSQL Schema
-- Applied via Flyway V1__init.sql
-- =================================================================

-- ================= institutions & users =================
CREATE TABLE institution (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    type            VARCHAR(50)  NOT NULL,   -- IIT, NIT, CSIR_LAB, PRIVATE_UNIV, OTHER
    tto_contact_email VARCHAR(255),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id              BIGSERIAL PRIMARY KEY,
    institution_id  BIGINT REFERENCES institution(id),
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL,   -- RESEARCHER, TTO_OFFICER, INSTITUTION_ADMIN, SUPER_ADMIN
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================= papers & extracted molecules =================
CREATE TABLE paper (
    id                  BIGSERIAL PRIMARY KEY,
    institution_id      BIGINT REFERENCES institution(id),
    uploaded_by         BIGINT REFERENCES app_user(id),
    title               VARCHAR(500) NOT NULL,
    authors             TEXT,
    source_type         VARCHAR(30) NOT NULL,  -- PREPRINT, JOURNAL, THESIS, CONFERENCE
    publication_date    DATE,
    raw_text            TEXT,                  -- extracted full text
    file_path           VARCHAR(500),           -- original PDF, if stored
    status              VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, PROCESSED, FAILED
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE molecule (
    id                      BIGSERIAL PRIMARY KEY,
    paper_id                BIGINT NOT NULL REFERENCES paper(id) ON DELETE CASCADE,
    extracted_name_raw      VARCHAR(500) NOT NULL,   -- name as it appeared in text
    iupac_name              VARCHAR(500),
    smiles                  TEXT,
    extraction_confidence   NUMERIC(4,3),            -- 0.000 - 1.000
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================= novelty scans =================
CREATE TABLE novelty_scan (
    id                      BIGSERIAL PRIMARY KEY,
    molecule_id             BIGINT NOT NULL REFERENCES molecule(id) ON DELETE CASCADE,
    novelty_score           NUMERIC(4,3) NOT NULL,   -- 0 = identical to known compound, 1 = fully novel
    is_novel                BOOLEAN NOT NULL,
    closest_match_source    VARCHAR(30),             -- PUBCHEM, CHEMBL, SURECHEMBL
    closest_match_id        VARCHAR(100),
    tanimoto_similarity     NUMERIC(4,3),
    scanned_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================= disclosure grace-period tracking =================
CREATE TABLE disclosure_window (
    id                          BIGSERIAL PRIMARY KEY,
    paper_id                    BIGINT NOT NULL REFERENCES paper(id) ON DELETE CASCADE,
    disclosure_date             DATE NOT NULL,             -- date paper/thesis was disclosed
    grace_period_days           INT NOT NULL DEFAULT 365,  -- Section 31 grace period
    deadline_date               DATE NOT NULL,             -- disclosure_date + grace_period_days
    qualifies_for_section_31    BOOLEAN NOT NULL DEFAULT false,
    status                      VARCHAR(30) NOT NULL DEFAULT 'OPEN', -- OPEN, CLOSING_SOON, EXPIRED, FILED
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================= alerts =================
CREATE TABLE alert (
    id                      BIGSERIAL PRIMARY KEY,
    recipient_user_id       BIGINT NOT NULL REFERENCES app_user(id),
    disclosure_window_id    BIGINT REFERENCES disclosure_window(id),
    alert_type              VARCHAR(30) NOT NULL,  -- DEADLINE_30D, DEADLINE_7D, EXPIRED, NOVEL_MOLECULE_FOUND
    message                 TEXT NOT NULL,
    is_read                 BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ================= patent filings (outcome tracking) =================
CREATE TABLE patent_filing (
    id                  BIGSERIAL PRIMARY KEY,
    molecule_id         BIGINT NOT NULL REFERENCES molecule(id),
    filed_by            BIGINT REFERENCES app_user(id),
    filing_date         DATE,
    patent_office       VARCHAR(50),   -- e.g. INDIA_IPO, USPTO
    application_number  VARCHAR(100),
    status              VARCHAR(30) NOT NULL DEFAULT 'FILED'  -- FILED, GRANTED, REJECTED, WITHDRAWN
);

-- ================= indexes =================
CREATE INDEX idx_paper_institution   ON paper(institution_id);
CREATE INDEX idx_molecule_paper      ON molecule(paper_id);
CREATE INDEX idx_scan_molecule       ON novelty_scan(molecule_id);
CREATE INDEX idx_window_paper        ON disclosure_window(paper_id);
CREATE INDEX idx_window_status       ON disclosure_window(status);
CREATE INDEX idx_alert_user          ON alert(recipient_user_id, is_read);
