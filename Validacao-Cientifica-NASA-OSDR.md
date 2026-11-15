# Validação Científica e Fórmulas Físico-Biológicas (NASA OSDR & HRP)

Este documento registra a rastreabilidade científica, os **3 Tiers de Prioridade** extraídos de [PDFs/Pesquisa.pdf](file:///C:/Users/Marcos/Documents/projetos/java/hackathon-nasa-2026/PDFs/Pesquisa.pdf), os arquivos `*_TRANSFORMED.csv` oficiais do NASA OSDR e as equações matemáticas implementadas no motor Java do **OrbitalVitals** ([TelemetrySimulator.java](file:///C:/Users/Marcos/Documents/projetos/java/hackathon-nasa-2026/src/engine/TelemetrySimulator.java) e [AnomalyEngine.java](file:///C:/Users/Marcos/Documents/projetos/java/hackathon-nasa-2026/src/engine/AnomalyEngine.java)).

---

## 0. Classificação em Tiers de Relevância Operacional (do PDF da Pesquisa)

* **Tier 1 — Prioridade Máxima (Core do Sistema: Telemetria Contínua e Humanos em Órbita):**
  * **RadLab API (`GET /radlab/api`):** `absorbed_dose_rate`, `dose_equivalent_rate`, `GCR`, `SAA`, `SPE`.
  * **EDA (Environmental Data Application):** Temperatura, Umidade Relativa e `CO2` de cabine (ISS RR-1 a RR-28).
  * **`OSD-569` (`GLDS-561` / `LSDS-7`) — SpaceX Inspiration4 (*Homo sapiens*):** Hemograma completo (`LSDS-7_Complete_Blood_Count_CBC_TRANSFORMED.csv`), **Razão Neutrófilos-Linfócitos (`NLR`)** para disfunção imune e resposta inflamatória aguda.
  * **`OSD-575` (`LSDS-8`) — SpaceX Inspiration4 (*Homo sapiens*):** Painel Metabólico (`CMP`), painel cardiovascular `HDCVD9`, citocinas `HD71`/`NULISA-seq` e **Proteína C-Reativa (`CRP`)** (`LSDS-8_*_TRANSFORMED.csv`).
* **Tier 2 — Prioridade Alta para Modelagem de Longa Duração (Análogos Humanos, SANS e Modelos Crônicos):**
  * **`OSD-435` (`LSDS-22`):** Ecocardiografia da aorta (`LSDS-22_Echocardiography_aorta_TRANSFORMED.csv`) — frequência cardíaca e **velocidade sanguínea de pico na aorta (`Peak Blood Velocity cm/s`)**.
  * **`OSD-815`, `OSD-816`, `OSD-817`, `OSD-818` (`LSDS-136`, `LSDS-135`, `LSDS-132`):** Modelo ex vivo humano para **SANS** — gradiente translaminar `ICP:IOP` e **inclinação do nervo ótico (`Optic Nerve Tilt`)**.
  * **`OSD-942`, `OSD-195`, `OSD-198`, `OSD-783`:** Análogos humanos de **21 a 64 dias de repouso no leito** e **7 dias de imersão seca**.
  * **`OSD-108`, `OSD-310` (`LSDS-30`), `OSD-417`, `OSD-471`, `OSD-661–664`, `OSD-977`:** Densidade mineral óssea (`BMD via DEXA/DXA`), histomorfometria e **força de flexão vertebral (`OSD-471`)**.
* **Tier 3 — Contexto Molecular / Lacuna de Espécie:**
  * **`OSD-1005` (PROMETEO 2)** e **`OSD-952`** (monitoramento comportamental por vídeo).

---

## 1. Perda de Densidade Mineral Óssea (DEXA) e Força Vertebral (`OSD-108`, `OSD-471`, `OSD-942`)

### Fontes Oficiais NASA / OSDR
* **NASA Human Research Program (HRP) & DEXA Baselines:** Astronautas em microgravidade (`0.0G`) perdem em média **1,0% a 1,5% de Densidade Mineral Óssea (BMD) por mês** nos ossos de sustentação de carga (fêmur proximal e coluna lombar), acumulando **6% a 9% em 6 meses (180 dias)**.
* **Resistência Mecânica Vertebral (`OSD-471` / `OSD-310 LSDS-30`):** A perda de microarquitetura trabecular reduz a resistência à flexão vertebral a uma taxa ~1,65x proporcional à perda de BMD areal.

### Fórmula Implementada no Código Java
* Fator de descarga gravitacional ($U_G$):
  $$U_G = \max(0, \; 1.0 - G)$$
* Taxa mensal efetiva (`0.0G` = $1,35\%/\text{mês}$):
  $$\text{Rate}_{\text{month}} = 0.08 + (1.27 \times U_G) \quad [\%/\text{mês}]$$
* **Perda Óssea Total Acumulada após $D$ dias no espaço:**
  $$\text{CumulativeBoneLoss}(D, G) = \text{Rate}_{\text{month}} \times \left(\frac{D}{30.0}\right) \quad [\%]$$
* **Resistência Vertebral Estimada (`OSD-471`):**
  $$\text{VertebralStrength}(D, G) = \max\left(65.0, \; 100.0 - 1.65 \times \text{CumulativeBoneLoss}(D, G)\right) \quad [\%]$$

---

## 2. Síndrome Neuro-Ocular (SANS — `OSD-815..818` / `LSDS-136`)

* Pressão Intraocular (`IOP`) e **Inclinação do Nervo Ótico (`Optic Nerve Tilt`)** em função da falta de gravidade ($U_G$) e do tempo de exposição ($D$):
  $$\text{IOP}(D, G) = 14.2 + (4.2 \times U_G) + \left(3.8 \times U_G \times \min\left(1.0, \frac{D}{45.0}\right)\right) \quad [\text{mmHg}]$$
  $$\text{OpticTilt}(D, G) = 1.2^\circ + \left(6.8^\circ \times U_G \times \min\left(1.0, \frac{D}{60.0}\right)\right)$$

---

## 3. Dosimetria de Radiação (`RadLab`) e Biomarcadores Imunes (`OSD-569 NLR` & `OSD-575 CRP`)

* **Atenuação por blindagem:** Estação bloqueia **78%** da dose externa; Traje EVA bloqueia **32%**.
* **Biomarcadores Sanguíneos (`LSDS-7` e `LSDS-8`):**
  * **Razão Neutrófilos-Linfócitos (`NLR`):** basal $\approx 1,75$, elevando-se proporcionalmente à taxa de dose interna absorvida e duração da missão.
  * **Proteína C-Reativa (`CRP`):** basal $\approx 0,8\text{ mg/L}$, subindo sob exposição radiológica e eventos de partículas solares (`SPE`).
