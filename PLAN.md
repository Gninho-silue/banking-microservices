# PLAN.md — Faire de ce projet *mon* projet (Banking Microservices)

> Brief pour Claude Code. Lis ce fichier en entier avant de commencer.
> Travaille **une étape à la fois**, dans l'ordre. À la fin de chaque étape : tests verts → commit(s) → push → coche la case ici → stop et résume ce qui a été fait.

## Contexte (à ne pas oublier)

- Ce dépôt est basé sur l'architecture du cours EazyBytes **"Master Microservices with Spring Boot, Docker & Kubernetes"** (packages `com.easybytes.*`, module `easy-bom`).
- Objectif : ajouter du **vrai travail personnel** (tests, fonctionnalités métier, CI) pour pouvoir le défendre en entretien. **On ne cache jamais l'origine du cours** : le README la mentionne explicitement.
- Base de données : **MySQL, on la garde**. Ne pas migrer vers PostgreSQL.
- Le projet **n'est pas déployé**. La CI = build + tests uniquement, **pas de CD**.
- Schéma DB géré par `src/main/resources/schema.sql` dans chaque service (pas de Flyway). Garder ce mécanisme, mettre à jour ces fichiers.
- Stack : Java 21, Spring Boot 3, Spring Cloud (Config, Eureka, Gateway, OpenFeign, Stream/Kafka, Resilience4j), Keycloak, Jib, Helm/K8s, Grafana LGTM.

## Règles générales

- **Commit & push obligatoires** à chaque étape, sur `main` (remote `origin` = github.com/Gninho-silue/banking-microservices).
- **Un commit = une feature terminée** (code + tests + doc de la feature), pas de commits intermédiaires « WIP ». On ne commite que quand la feature est complète et que tous les tests passent. Messages en anglais, format Conventional Commits : `feat(loans): ...`, `test(accounts): ...`, `chore: ...`, `ci: ...`, `docs: ...`, `refactor: ...`.

### TDD (obligatoire pour tout nouveau code, étapes 2 et 3)
Cycle **Red → Green → Refactor**, règle par règle :
1. **Red** : écrire d'abord un test qui décrit le comportement attendu ; le lancer et **constater qu'il échoue** (pour la bonne raison).
2. **Green** : écrire le minimum de code pour le faire passer.
3. **Refactor** : nettoyer (noms, duplication) avec les tests toujours verts.
Commencer par les règles métier pures (calculateur d'amortissement, validation de virement), puis la couche service, puis le contrôleur, puis l'intégration (Testcontainers).
Quand tu résumes une étape, liste les comportements dans l'ordre où tu les as testés.

> Étape 1 = code déjà existant : ce n'est pas du TDD mais des *tests de caractérisation* (on fige le comportement actuel). Si un test révèle un bug, le signaler avant de corriger.
- Avant chaque commit : `./mvnw -B verify` dans le(s) service(s) modifié(s) doit passer. **Ne jamais commiter du code rouge.**
- `git push` après chaque étape terminée (pas seulement à la fin).
- **Interdits** : `git push --force`, réécriture d'historique, commit de secrets / `.env` / données Docker (`.data/`), `@Disabled` pour faire passer un test.
- Montant d'argent = **`BigDecimal`** (jamais `double`/`float`), arrondi explicite (`RoundingMode.HALF_EVEN`, scale 2).
- Garder le style existant (Lombok, DTO + mapper, `GlobalExceptionHandler`, `ResponseDto`/`ErrorResponseDto`).
- Si une décision est ambiguë ou destructive → demander avant d'agir.

---

## Étape 0 — Hygiène du dépôt
- [x] Ajouter au `.gitignore` : `docker-compose/**/.data/`, `**/target/`, `.env`, `*.log`.
- [x] `git rm -r --cached docker-compose/default/.data docker-compose/prod/.data` (les fichiers restent sur disque).
- [x] Vérifier qu'aucun secret réel n'est versionné (`git grep -nIiE "password|secret|client-secret"`) ; s'il y en a, me le signaler (ne pas réécrire l'historique).
- [x] Commit : `chore: stop tracking docker volume data and tighten .gitignore` → push.

**Terminé quand** : `git status` est propre après un `docker compose up/down`.

## Étape 1 — Vraie suite de tests sur le code existant
Pour **accounts, loans, cards** :
- [x] Profil de test `src/test/resources/application-test.yml` : `spring.cloud.config.enabled=false`, `eureka.client.enabled=false`, Kafka/stream désactivé ou mocké, pour que les tests tournent sans infra.
- [x] Remplacer le `*ApplicationTests` vide par un vrai test de contexte (ou le supprimer s'il ne peut pas démarrer sans infra, en le justifiant dans le commit).
- [x] **Tests unitaires (JUnit 5 + Mockito)** des services : `AccountsServiceImpl`, `CustomerServiceImp`, `LoansServiceImpl`, `CardsServiceImpl` — cas nominaux + erreurs (`ResourceNotFoundException`, `*AlreadyExistsException`).
- [x] **Tests de contrôleurs (`@WebMvcTest`)** : codes HTTP, validation (`@Valid`), format des erreurs.
- [x] **Tests d'intégration repository avec Testcontainers MySQL** (`@DataJpaTest` + `@Testcontainers`, `MySQLContainer`), qui chargent `schema.sql`.
- [x] `CustomerServiceImp` (agrégation via Feign) : test avec Feign clients mockés + test du fallback (`CardsFallback`, `LoansFallback`).
- [x] Ajouter JaCoCo (plugin dans `easy-bom` ou par service) ; viser **≥ 70 % de couverture lignes sur `service/`**.
- [x] Un commit par service une fois sa suite complète et verte (`test(accounts): ...`, `test(loans): ...`, `test(cards): ...`) → push.

**Terminé quand** : `./mvnw -B verify` vert dans les 3 services, Docker Desktop requis uniquement pour Testcontainers.

## Étape 2 — Virements entre comptes (feature principale)
Dans **accounts** :
- [x] Ajouter `balance DECIMAL(19,2) NOT NULL DEFAULT 0` à `accounts` (`schema.sql` + entité `Accounts`, `BigDecimal`).
- [x] Nouvelle table/entité `transfers` : id, `from_account`, `to_account`, `amount`, `status`, `idempotency_key` (UNIQUE), `created_at`.
- [x] Endpoint `POST /api/transfers` (header obligatoire `Idempotency-Key`) + `GET /api/transfers/{id}` + `GET /api/accounts/{accountNumber}/transfers`.
- [x] Règles : montant > 0, comptes différents et existants, solde suffisant (`InsufficientFundsException` → 422), même `Idempotency-Key` rejouée ⇒ **même réponse, aucun double débit**.
- [x] Concurrence : `@Transactional` + verrou pessimiste (`@Lock(PESSIMISTIC_WRITE)`), **verrouiller les deux comptes dans l'ordre croissant des numéros** pour éviter les deadlocks.
- [x] **Transactional Outbox** : table `outbox_events` (id, aggregate_id, type, payload JSON, created_at, published_at). L'événement `TransferCompleted` est écrit **dans la même transaction** que le virement ; un `@Scheduled` publisher l'envoie à Kafka via `StreamBridge` puis marque `published_at`.
- [x] **message** : nouveau consumer `transferNotification` qui log une notification (comme `email`/`sms` existants). Mettre à jour la config des bindings.
- [x] Tests : unitaires (règles métier), idempotence, **test de concurrence** (ex. 20 virements parallèles sur le même compte → solde final correct, jamais négatif), Testcontainers MySQL + **Testcontainers Kafka** pour vérifier que l'événement outbox est publié.
- [x] Documenter les endpoints (annotations OpenAPI existantes).
- [x] Commits (en TDD, un par feature terminée, tests inclus) : `feat(accounts): idempotent transfers between accounts` (solde + virements + idempotence + concurrence), `feat(accounts): transactional outbox for transfer events`, `feat(message): transfer notification consumer` → push.

**Terminé quand** : tous les tests verts, et un scénario manuel via docker-compose fonctionne (virement → log dans `message`).

## Étape 3 — Échéancier de prêt (amortissement)
Dans **loans** :
- [x] Migrer `totalLoan`, `amountPaid`, `outstandingAmount` de `Integer` vers `BigDecimal` (`DECIMAL(19,2)`) — entité, DTO, mapper, `schema.sql`, tests de l'étape 1 adaptés.
- [x] Ajouter `annualInterestRate DECIMAL(5,4)`, `termMonths INT`, `startDate DATE` (valeurs par défaut cohérentes à la création).
- [x] Classe pure `AmortizationCalculator` (sans Spring) : méthode à mensualités constantes, `M = P·r / (1 − (1+r)^−n)`, `r = taux annuel / 12`. Gérer `r = 0`. Calculs en `BigDecimal` avec `MathContext.DECIMAL64`, arrondi à 2 décimales ; **la dernière échéance absorbe l'écart d'arrondi** pour que le capital restant finisse exactement à 0.
- [x] Endpoint `GET /api/loans/schedule?mobileNumber=...` (cohérent avec l'API existante) → liste `{n, dueDate, payment, interest, principal, remaining}` + totaux.
- [x] Tests : valeur de référence **100 000 à 5 % sur 12 mois ⇒ mensualité 8 560,75** ; somme des principaux = capital ; dernier `remaining` = 0.00 ; taux 0 ; validation des entrées (taux négatif, durée 0 → 400).
- [x] Commits (en TDD, tests inclus) : `refactor(loans): use BigDecimal for monetary amounts`, `feat(loans): amortization schedule` → push.

## Étape 4 — CI GitHub Actions (build + tests, pas de déploiement)
- [x] `.github/workflows/ci.yml` : déclenché sur `push` et `pull_request` vers `main`.
- [x] Job matrix sur `[accounts, loans, cards, message, gatewayserver, configserver, eurekaserver]` : `actions/setup-java@v4` (Temurin 21, cache Maven) → installer `easy-bom` d'abord (`./mvnw -B install` dans le BOM ou `-pl`), puis `./mvnw -B verify`.
- [x] Testcontainers fonctionne nativement sur `ubuntu-latest` (Docker présent).
- [x] Upload des rapports JaCoCo en artefact.
- [x] Badge CI en haut du README.
- [x] Commit `ci: build and test all services on push` → push, **vérifier que le run GitHub est vert** (`gh run watch` ou onglet Actions) ; corriger sinon.

## Étape 5 — README
- [ ] Section **"Based on"** en tête : « Built on the architecture of the EazyBytes *Master Microservices with Spring Boot, Docker & Kubernetes* course. »
- [ ] Section **"Beyond the course — what I added"** : virements idempotents + outbox Kafka, échéancier de prêt, suite de tests (unit / WebMvc / Testcontainers, chiffres de couverture réels), CI, hygiène du dépôt.
- [ ] Diagramme d'architecture en **Mermaid** (gateway, services, Kafka, Keycloak, MySQL, observabilité).
- [ ] Exemples `curl` pour transfert et échéancier ; comment lancer les tests.
- [ ] Corriger le README : DB = MySQL (pas de mention PostgreSQL), retirer tout ce qui n'existe pas dans le code.
- [ ] Commit `docs: ...` → push.

## Étape 6 — Renommage (en dernier, seulement si tout est vert)
- [ ] Packages `com.easybytes.*` → `dev.silue.banking.*` ; module `easy-bom` → `banking-bom` ; `groupId` en conséquence.
- [ ] `git grep -n -i "easybytes\|easy-bom"` dans Java, POM, YAML, Helm, k8s, Dockerfile, noms d'images Jib — tout mettre à jour de façon cohérente.
- [ ] La mention du cours reste dans le README (section "Based on").
- [ ] Toute la CI verte après renommage.
- [ ] Commit `refactor: rename packages and BOM to project namespace` → push.

---

## À la fin (pour Gninho)
Résume : ce qui a été ajouté, nombre de tests, couverture réelle, lien du run CI vert. Ces chiffres serviront à remettre à jour LinkedIn et le portfolio (`silue-dev`) — **uniquement avec ce qui est réellement dans le code**.
