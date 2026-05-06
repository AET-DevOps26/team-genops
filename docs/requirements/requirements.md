# Project Requirements

## Objective

The project requires teams to design, implement, and operate a complete software system that reflects a realistic DevOps workflow. The goal is to demonstrate how a system is structured, integrated, deployed, and maintained in a reproducible and observable way. Development, deployment, and operation must therefore be treated as a single engineering problem rather than as separate phases.

At the technical level, the project must result in a web application that includes a client side, a server side, persistent storage, and a separate Generative AI component. The system must be containerised, runnable locally, automatically tested and deployed through GitHub Actions, deployable to Kubernetes, and observable through Prometheus and Grafana. The application domain is flexible, but the technical and process requirements are fixed and must all be satisfied.

The project is intended to simulate a realistic cloud-native software delivery scenario. The final result must be structured in a way that supports modular development, reproducible setup, automated deployment, and operational visibility.

> **Deadline:** To be announced (EOD – 23:59 Munich time)

| Aspect | Requirement |
|--------|-------------|
| Project type | Complete DevOps-oriented software system |
| Main focus | Development, deployment, operation, and observability as one integrated workflow |
| Required system elements | Client, server, database, GenAI, CI/CD, Kubernetes, monitoring |
| Application domain | Flexible, but all technical requirements must be fulfilled |

---

## Team Organisation

Teams consist of three students. Each student must take responsibility for one primary subsystem, typically client, server, or GenAI. However, subsystem ownership does not imply isolated work. Teams are expected to collaborate across subsystem boundaries, especially for integration, deployment, and debugging. A project where each student only works on their part without participating in system integration does not reflect the intended DevOps model.

Registration information is required in order to make contribution tracking possible and to connect repository activity to individual team members. Therefore, each student must provide their GitHub username, TUMonline login, and matriculation number.

Work must be transparent and traceable throughout the semester. This means that contributions must be visible through GitHub commits, pull request authorship, code review participation, and involvement in infrastructure tasks such as CI/CD configuration and deployment.

Communication must take place through the official course channels. Tutor feedback, planning, questions, and issue reporting must be visible in the dedicated Artemis team channels. No other communication channel will be taken into account for evaluation.

| Aspect | Requirement |
|--------|-------------|
| Team size | 3 students |
| Registration | GitHub username, TUMonline login, matriculation number |
| Ownership | Each student owns a primary subsystem (client, server, GenAI) |
| Collaboration | Collaborative development across subsystem boundaries is expected |
| Contribution tracking | Visible via commits, PRs, code reviews, and infrastructure work |
| Communication | Only official course channels in Artemis |

---

## Development Workflow

The project must be developed in a GitHub mono-repository. The system must be treated as one integrated deliverable. A mono-repo makes it possible to version client, server, GenAI service, deployment files, CI/CD workflows, and documentation together, and to validate changes across the whole system.

All work must be structured through pull requests. Each feature or bugfix must be developed in a dedicated feature branch. Direct commits to the main branch are not acceptable as a normal workflow. A pull request must be opened, reviewed, and approved before the change is merged into main.

The CI pipeline must run automatically on every pull request. At a minimum, it must build the relevant services and execute the automated tests. On merge to main, the CD pipeline must automatically deploy the system to a Kubernetes environment.

> **Intended workflow:** develop in a feature branch → validate through CI → review through PR → merge into main → deploy automatically

| Aspect | Requirement |
|--------|-------------|
| Repository | GitHub mono-repo |
| Branching | Each feature or bugfix developed in a feature branch |
| Pull Requests | Mandatory before merge into main |
| Code review | Peer review and approval by team members required |
| CI checks | Automated tests and validation on every PR |
| CD behaviour | Automatic deployment to Kubernetes on merge to main |

---

## System Architecture

The system must be structured as a set of interacting but separated components. At minimum, this includes a client side, a server side, a database, and a separate GenAI component.

- **Client** must provide a usable interface and communicate with the backend over REST.
- **Server** must expose REST APIs, coordinate business logic, and interact with persistent storage.
- **Database** must support persistent data storage and must have a documented schema.
- **GenAI component** must run as an independent service and communicate with the backend over a defined interface.

The server side must be implemented in **Spring Boot** and must consist of **at least three microservices**. These services do not need to be large, but they must have distinct responsibilities and communicate in a controlled and documented way.

| Component | Technology | Notes |
|-----------|------------|-------|
| Client Side | React, Angular, or Vue.js | Usable, responsive UI that interacts with server over REST |
| Server Side | Spring Boot (Java) | Must expose REST APIs and consist of at least 3 microservices; modular architecture required |
| Database | MySQL, PostgreSQL, or similar | Must support persistent storage; schema must be documented; run via Docker |

---

## GenAI Component

The GenAI component must be implemented as a separate service in **Python**. It must be deployed as a modular microservice, containerised independently, networked with the server, and integrated through a defined interface.

Functionally, the GenAI component must fulfil a real user-facing use case. Acceptable examples include summarisation, generation, question answering, or a similarly meaningful feature that is accessible through the application workflow. It is not sufficient to include a GenAI service that exists technically but is not connected to an actual user-facing capability.

The system must support both **cloud-based** and **local** large language models. Cloud support may be implemented through providers such as the OpenAI API. Local model support may be implemented using technologies such as GPT4All or LLaMA.

> **Optional bonus:** Full RAG architecture using a vector database such as Weaviate.

| Aspect | Requirement |
|--------|-------------|
| Language | Python |
| Deployment | Modular microservice, containerised and networked with the server |
| Functionality | Real user-facing use case, e.g. summarisation, generation, Q&A |
| Model support | Cloud-based models (e.g. OpenAI API) and local models (e.g. GPT4All, LLaMA) |
| Optional bonus | Full RAG architecture using a vector database such as Weaviate |

---

## Environment and Deployment

All components must be fully containerised and runnable locally using a compose-based setup. Each component must have its own Dockerfile. The local setup must support end-to-end system execution through a `docker-compose.yml` file.

The local setup must be simple — runnable in **three or fewer commands** (e.g. `docker compose up`). A new user must be able to start the system without reverse-engineering the project.

The same system must also be deployable to **Kubernetes**, either through Helm charts or raw Kubernetes manifests. Configuration must be externalised using environment variables, Secrets, and similar mechanisms. Hardcoded credentials or environment-dependent values are not acceptable.

| Aspect | Requirement |
|--------|-------------|
| Containerisation | All components (server, client, GenAI, DB) must have their own Dockerfile |
| Local orchestration | `docker-compose.yml` must run the system end-to-end locally |
| Setup | Runnable in three or fewer commands; no complex manual ENV setup |
| Kubernetes | Deployable using Helm or raw manifests |
| Environments | Local infrastructure (Rancher) and a cloud option (Azure) |

---

## CI/CD

The system must include a working CI/CD pipeline implemented with **GitHub Actions**.

**Continuous Integration** must build and test all services and perform static analysis or linting where appropriate. The CI pipeline should fail when the system is not in a correct or stable state.

**Continuous Deployment** must automatically deploy to Kubernetes after merge to main. The workflow must make correct use of secrets and environment-specific variables.

| Aspect | Requirement |
|--------|-------------|
| Tooling | GitHub Actions |
| CI tasks | Build and test all services; perform static analysis/linting |
| CD tasks | Automatically deploy to Kubernetes on merge to main |
| Configuration | Must use secrets and support environment-specific variables |

---

## Observability

The system must expose basic but meaningful operational visibility. Monitoring should not stop at "Prometheus is installed" — monitored data must allow someone to understand whether the system is behaving correctly or incorrectly.

- **Prometheus** must track at minimum: request count, latency, and error rate.
- **Grafana** dashboards must reflect key system metrics and be submitted as exported `.json` files.
- **At least one meaningful alert rule** must be configured (e.g. service downtime or slow response time).

| Tool | Requirements |
|------|--------------|
| Prometheus | Metrics collection for at least request count, latency, and error rate |
| Grafana | Dashboards must reflect key system metrics (server, GenAI); must be submitted as `.json` |
| Alerts | At least one meaningful alert rule, e.g. service down or slow response time |

---

## Testing

Testing must validate the behaviour of the system and cover critical server-side logic, relevant parts of the GenAI component, and important client-side workflows.

- **Unit tests** are mandatory for critical server and GenAI logic.
- **Client-side tests** should cover core workflows and interactions.
- All tests must run **automatically in the CI pipeline**.

| Aspect | Requirement |
|--------|-------------|
| Unit Tests | Must cover critical server and GenAI logic |
| Client Tests | Should cover core workflows and interactions |
| CI Testing | All tests must run automatically in the CI pipeline |

---

## Engineering Artefacts

Teams must provide engineering artefacts that explain how the system is structured and how it works.

**Required artefacts:**
- High-level architecture description with subsystem decomposition and interfaces
- UML-style diagrams: **Subsystem Decomposition**, **Use Case Diagram**, **Analysis Object Model**
- API documentation via **OpenAPI/Swagger** with Swagger UI or equivalent exposed

| Aspect | Requirement |
|--------|-------------|
| Architecture | High-level system description |
| Decomposition | Subsystems and interfaces |
| Architecture Diagrams | Subsystem Decomposition, Use Case Diagram, Analysis Object Model (all mandatory) |
| API documentation | OpenAPI/Swagger documentation with Swagger UI or equivalent |

---

## Deliverables

| Deliverable | Description |
|-------------|-------------|
| Source Code | Complete codebase for server, client, and GenAI services |
| Docker Setup | Dockerfiles and `docker-compose.yml` for local setup |
| Kubernetes Deployment | Helm charts or raw Kubernetes YAMLs with setup instructions |
| Monitoring Configuration | Prometheus and Grafana config with exported dashboards and alert rules |
| Testing Suite | Unit/integration tests with instructions to run them |
| Documentation | `README.md` with setup guide, architecture, API docs, CI/CD and monitoring instructions, student responsibilities |

The project concludes with a **final presentation** and **individual oral examination**. Each student must present their subsystem and be ready to answer technical questions. The final team presentation must include a **live demo**.

---

## Common Pitfalls and How to Avoid Them

> *"A smart man learns from his mistakes, a wise man learns from the mistakes of others."*

### Patterns of Effective Projects

#### Reliability > Feature Count

Feature orientation is one of the most common reasons teams fail. Quality cannot be beaten by quantity. If you cannot reliably deploy or run every new feature, the whole project flow eventually breaks. **Keep the scope small, make the system deployable early, and iterate.**

#### The System Is a Single Pipeline

Every component is interconnected. Treating coding, deployment, and monitoring as separate concerns results in just a codebase. Link every component into one chain:

```
code → test → build → deploy → observe → improve
```

#### Reproducibility

Ask yourself after each feature: *"Can someone else run this system without me?"* Typical failures include many manual steps and undocumented environments. Make setup trivial, eliminate manual configuration, and test from scratch at least a couple of times.

#### Visible System Behaviour

Many teams "install monitoring" but dashboards show nothing useful. All dashboards must be linked to real system behaviour. Visualise latency, failures, and load so your team can understand system state rather than collect data without purpose.

---

### Patterns of Failure

#### Project as a Checklist

Treating requirements as checkboxes drastically reduces quality. Requirements should be a starting point — add your own ideas, understanding, and experience on top. Connect every requirement to real system behaviour.

#### Late Integration

The most common answer to *"What would you do differently?"* is: **"Start integration much earlier."** Teams that build components separately and integrate at the end consistently end up with broken CI/CD and incomplete systems.

#### Fake CI/CD

It's easy to have CI/CD, but hard to have *good* CI/CD. Tests that are meaningless, pipelines that fail randomly, or that require manual approval for small changes are all red flags. Follow good practices from the beginning: [CI/CD Best Practices](https://about.gitlab.com/blog/how-to-keep-up-with-ci-cd-best-practices/)

#### GenAI as Decoration

Integrating GenAI as a checkbox wastes the opportunity. Use it to learn a new technology and think about real challenges modern teams are solving. Flexibility and willingness to learn are important qualities in a DevOps engineer.

#### "I Will Document It Later"

Always start a new class or method with a short comment describing its purpose. Document as you go. It helps not only you, but anyone who looks at your code later, and improves maintainability. Reference: [Writing Better Documentation](https://www.aleksandrhovhannisyan.com/blog/writing-better-documentation/)

---

## Team Culture

A good project is not built by perfect individuals working alone. The best teams communicate effectively, take responsibility, and support each other.

### Other People Cannot Read Your Thoughts

If something is bothering you, bring it up as soon as possible. Issues accumulate over time. If you cannot resolve a problem within your team, contact your tutor or instructor.

### Clear Roles and Responsibilities

Define individual responsibilities right after forming your team. Use a **RACI matrix** to clarify roles and accountability: [RACI Chart Guide](https://www.atlassian.com/work-management/project-management/raci-chart)

### Individual Strength

Everyone has different strengths. Good teams are technically strong, but great teams complement each other. Be eager to teach what you know and learn what you do not.
