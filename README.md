# DocMind — AI Document Intelligence Platform

> Upload company documents. Ask questions in plain English. Get cited AI answers instantly.

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3-green?logo=springboot" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/React-18-blue?logo=react" alt="React" />
  <img src="https://img.shields.io/badge/TypeScript-5-blue?logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/PostgreSQL-pgvector-blue?logo=postgresql" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/AI-Llama_3.1-purple" alt="Llama 3.1" />
</p>

---

## ✨ Key Features

- **RAG-Powered Answers** — Retrieval-Augmented Generation using Groq's Llama 3.1 70B
- **Source Citations** — Every answer shows exactly which page the information came from
- **Role-Based Access Control** — Admins see all, HR sees HR-tagged docs, Employees see only their own
- **Policy Freshness Alerts** — Visual warnings when documents haven't been verified in 6+ months
- **PDF & DOCX Support** — Upload and parse both formats with automatic chunking
- **Real-Time Processing** — Async document parsing with live status polling
- **Chat Interface** — Ask follow-up questions in a natural conversational UI

---

## 📊 How DocMind Compares

| Feature | DocMind | Guru | Glean | Notion AI |
|---------|---------|------|-------|-----------|
| Self-hosted | ✅ | ❌ | ❌ | ❌ |
| Source citations with page numbers | ✅ | ❌ | Partial | ❌ |
| RBAC (Admin/HR/Employee) | ✅ | ✅ | ✅ | ❌ |
| Policy freshness warnings | ✅ | ❌ | ❌ | ❌ |
| Free to run | ✅ | ❌ | ❌ | ❌ |
| Custom AI model | ✅ | ❌ | ❌ | ❌ |
| PDF + DOCX parsing | ✅ | ✅ | ✅ | ✅ |

---

## 🛠 Tech Stack

### Backend
- **Java 21** + **Spring Boot 3.3.0**
- **Spring Security 6** with JWT authentication (JJWT 0.12.3)
- **Spring Data JPA** with PostgreSQL
- **Apache PDFBox 3.0.1** for PDF parsing
- **Apache POI 5.2.5** for DOCX parsing
- **SpringDoc OpenAPI 2.3.0** for Swagger UI

### Frontend
- **React 18** + **TypeScript 5**
- **Vite 5** for blazing-fast dev server
- **Tailwind CSS 3** with custom glassmorphism design system
- **React Router DOM 6** for SPA routing
- **Axios** with JWT interceptors
- **React Dropzone** for drag-and-drop file uploads
- **Lucide React** for icons

### Database & AI
- **PostgreSQL** with pgvector on **Neon** (serverless)
- **Groq API** running **Llama 3.1 70B Versatile**

---

## 🚀 Getting Started

### Prerequisites
- Java 21 (JDK)
- Node.js 18+
- PostgreSQL database (or use [Neon](https://neon.tech) free tier)
- [Groq API key](https://console.groq.com)

### 1. Clone & Configure

```bash
git clone https://github.com/your-username/ai-doc-platform.git
cd ai-doc-platform
cp .env.example .env
# Edit .env with your actual values
```

### 2. Start Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend starts at `http://localhost:8080` with Swagger UI at `/swagger-ui.html`.

### 3. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend starts at `http://localhost:5173`.

### 4. Login

Default admin credentials:
- **Email:** `admin@aidoc.com`
- **Password:** `Admin@123`

---

## 📡 API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Log in and get JWT token |

### Documents
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/documents` | List all accessible documents |
| POST | `/api/documents/upload` | Upload PDF/DOCX file |
| DELETE | `/api/documents/{id}` | Delete a document |
| GET | `/api/documents/{id}/status` | Check processing status |
| PATCH | `/api/documents/{id}/verify` | Mark document as verified (Admin/HR) |

### Query
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/query/ask` | Ask a question about a document |
| GET | `/api/query/history` | Get query history |

---

## 🏗 Architecture

```
┌─────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   React UI  │────▶│  Spring Boot API │────▶│  PostgreSQL     │
│   (Vite)    │     │  (Java 21)       │     │  (Neon + pgvec) │
└─────────────┘     └────────┬─────────┘     └─────────────────┘
                             │
                             ▼
                    ┌──────────────────┐
                    │   Groq AI API    │
                    │  (Llama 3.1 70B) │
                    └──────────────────┘
```

---

## 🐳 Docker

```bash
docker-compose up -d
```

---

## 📸 Screenshots

> Screenshots coming soon — upload a PDF and try asking "What is the PTO policy?"

---

## 📄 License

MIT License — free to use, modify, and distribute.
