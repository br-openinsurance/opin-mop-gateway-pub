# Achados Alpine — imagem base (2026)

Varredura da imagem `ghcr.io/br-openinsurance/opin-mop-gateway-pub/open-insurance-mop-gateway:develop` (Alpine **3.23.5**). Achados **Critical** e **High** estão no [escopo de remediação](README.md#escopo-de-remediação-severidade).

| CVE | Pacote | Versão afetada | CVSS | Severidade | Correção |
|-----|--------|----------------|------|------------|----------|
| CVE-2026-11822 | `sqlite-libs` | 3.51.2-r0 | 9.8 | **Critical** | `>= 3.53.2` |
| CVE-2026-11824 | `sqlite-libs` | 3.51.2-r0 | 9.8 | **Critical** | `>= 3.53.2` |
| CVE-2026-41989 | `libgcrypt` | 1.11.2-r0 | 7.5 | **High** | `>= 1.11.3` |

---

## CVE-2026-11822 e CVE-2026-11824 — `sqlite-libs` 3.51.2-r0

**CVSS:** 9.8 (**Critical**)

**Pacote:** SQLite — biblioteca de banco de dados embarcado, presente na imagem base Alpine 3.23.5.

### Vetor de ataque (CVSS 9.8 = Network / Low / None)

- Acessível pela rede (não requer acesso local)
- Baixa complexidade de exploração
- Sem autenticação necessária
- Impacto total em confidencialidade, integridade e disponibilidade

### Riscos concretos para o MOP Gateway

- Score 9.8 geralmente indica **Remote Code Execution (RCE)** ou **SQL Injection** no próprio engine do SQLite.
- Se a aplicação processa dados não confiáveis que passam pelo SQLite (mesmo indiretamente via bibliotecas Java que usam SQLite nativo), um atacante pode executar código arbitrário no container.
- No contexto **Open Insurance**: dados de seguradoras chegam via API pública — se algum componente usa SQLite para cache/staging, payloads maliciosos podem explorar a vulnerabilidade.
- **Risco de compliance:** score 9.8 viola diretamente a policy **CISO-CS-ST009** (maturity 4).

### Mitigação

Atualizar para `sqlite-libs >= 3.53.2` na imagem base Alpine (`apk upgrade` no `Dockerfile` ou imagem base mais recente).

---

## CVE-2026-41989 — `libgcrypt` 1.11.2-r0

**CVSS:** 7.5 (**High**)

**Pacote:** `libgcrypt` — biblioteca de criptografia GNU, usada para operações criptográficas (hashing, cifração, assinaturas).

### Vetor de ataque (CVSS 7.5)

- Tipicamente acessível pela rede
- Pode ser explorado sem autenticação
- Impacto parcial (geralmente em confidencialidade **ou** disponibilidade)

### Riscos concretos para o MOP Gateway

- `libgcrypt` é fundamental para operações TLS/SSL e pode afetar assinaturas digitais.
- **Risco crítico para este serviço:** o MOP Gateway usa **JWS** (JSON Web Signature) para assinar payloads — se `libgcrypt` tiver vulnerabilidade criptográfica, pode comprometer a integridade das assinaturas.
- Possíveis cenários: side-channel attacks que vazem chaves privadas, DoS via payloads criptográficos malformados ou bypass de verificação de assinatura.
- **Impacto regulatório:** comprometimento das assinaturas JWS pode invalidar transmissões ao regulador de Open Insurance.

### Mitigação

Atualizar para `libgcrypt >= 1.11.3` na imagem base Alpine (`apk upgrade` no `Dockerfile` ou imagem base mais recente).
