# DevOps Project – Problem Statement

## Objective

The project requires teams to design, implement, and operate a complete software system that reflects a realistic DevOps workflow. The goal is to demonstrate how a system is structured, integrated, deployed, and maintained in a reproducible and observable way. Development, deployment, and operation must therefore be treated as a single engineering problem rather than as separate phases.

At the technical level, the project must result in a web application that includes a client side, a server side, persistent storage, and a separate Generative AI component. The system must be containerised, runnable locally, automatically tested and deployed through GitHub Actions, deployable to Kubernetes, and observable through Prometheus and Grafana. The application domain is flexible, but the technical and process requirements are fixed and must all be satisfied.

The project is intended to simulate a realistic cloud-native software delivery scenario. The final result must be structured in a way that supports modular development, reproducible setup, automated deployment, and operational visibility.

> **Deadline:** To be announced (EOD – 23:59 Munich time)

| Aspect | Requirement |
|---|---|
| Project type | Complete DevOps-oriented software system |
| Main focus | Development, deployment, operation, and observability as one integrated workflow |
| Required system elements | Client, server, database, GenAI, CI/CD, Kubernetes, monitoring |
| Application domain | Flexible, but all technical requirements must be fulfilled |

---

## Team Organisation

Teams consist of three students. Each student must take responsibility for one primary subsystem, typically client, server, or GenAI. However, subsystem ownership does not imply isolated work. Teams are expected to collaborate across subsystem boundaries, especially for integration, deployment, and debugging. A project where each student only works on their part without participating in system integration does not reflect the intended DevOps model.

Registration information is required in order to make contribution tracking possible and to connect repository activity to individual team members. Therefore, each student must provide their GitHub username, TUMonline login, and matriculation number.

Work must be transparent and traceable throughout the semester. This means that contributions must be visible through GitHub commits, pull request authorship, code review participation, and involvement in infrastructure tasks such as CI/CD configuration and deployment.

Communication must take place through the official course channels. Tutor feedback, planning, questions, and issue reporting must be visible in the dedicated Artemis team channels. No other communication channel will be taken into account for evaluation. Teams are expected to work in an organised and responsive way and to treat communication as part of engineering practice.

| Aspect | Requirement |
|---|---|
| Team size | 3 students |
| Registration | GitHub username, TUMonline login, matriculation number |
| Ownership | Each student owns a primary subsystem (client, server, GenAI) |
| Collaboration | Collaborative development across subsystem boundaries is expected |
| Contribution tracking | Visible via commits, PRs, code reviews, and infrastructure work |
| Communication | Only official course channels in Artemis |

---

## Development Workflow

The project must be developed in a GitHub mono-repository. Therefore, the system must be treated as one integrated deliverable. A mono-repo makes it possible to version client, server, GenAI service, deployment files, CI/CD workflows, and documentation together, and to validate changes across the whole system.

All work must be structured through pull requests. Each feature or bugfix must be developed in a dedicated feature branch. Direct commits to the main branch are not acceptable as a normal workflow. A pull request must be opened, reviewed, and approved before the change is merged into main. Team members must peer-review each other's work. Review is part of the workflow and should be treated as a normal step before merging changes.

The CI pipeline must run automatically on every pull request. At a minimum, it must build the relevant services and execute the automated tests. On merge to main, the CD pipeline must automatically deploy the system to a Kubernetes environment. The intended workflow is therefore: develop in a feature branch, validate through CI, review through PR, merge into main, and deploy automatically.

| Aspect | Requirement |
|---|---|
| Repository | GitHub mono-repo |
| Branching | Each feature or bugfix developed in a feature branch |
| Pull Requests | Mandatory before merge into main |
| Code review | Peer review and approval by team members required |
| CI checks | Automated tests and validation on every PR |
| CD behaviour | Automatic deployment to Kubernetes on merge to main |

---

## System Architecture

The system must be structured as a set of interacting but separated components. At minimum, this includes a client side, a server side, a database, and a separate GenAI component. The client side must provide a usable interface and communicate with the server over REST. The server side must expose REST APIs, coordinate business logic, and interact with persistent storage. The database must support persistent data storage and must have a documented schema. The GenAI component must run as an independent service and communicate with the remaining services over a defined interface.

The server side must be implemented in **Spring Boot** and must consist of **at least three microservices**. These services do not need to be large, but they must have distinct responsibilities and communicate in a controlled and documented way.

The client side may be implemented in React, Angular, or Vue.js. It must provide a usable and responsive interface and interact with the server over REST APIs. The database may be MySQL, PostgreSQL, or a similar relational or persistent database system, but it must run via Docker in local development and support documented persistent storage in the deployed setup.

| Component | Technology | Notes |
|---|---|---|
| Client Side | React, Angular, Vue.js | Usable, responsive UI that interacts with server over REST |
| Server Side | Spring Boot (Java) | Must expose REST APIs and consist of at least 3 microservices; modular architecture required |
| Database | MySQL or PostgreSQL or similar | Must support persistent storage; schema must be documented; run via Docker |

---

## GenAI Component

The GenAI component must be implemented as a separate service in Python. It must be deployed as a modular microservice, containerised independently, networked with the server, and integrated through a defined interface.

Functionally, the GenAI component must fulfil a real user-facing use case. Acceptable examples include summarisation, generation, question answering, or a similarly meaningful feature that is accessible through the application workflow. It is not sufficient to include a GenAI service that exists technically but is not connected to an actual user-facing capability.

The system must support both cloud-based and local large language models. Cloud support may be implemented through providers such as the OpenAI API. Local model support may be implemented using technologies such as GPT4All or LLaMA. Teams do not need to demonstrate sophisticated model research, but they do need to demonstrate that the service architecture can work with both remote and local inference options where feasible.

> **Optional Advanced Bonus:** Teams may implement a full retrieval-augmented generation (RAG) setup using a vector database such as Weaviate.

| Aspect | Requirement |
|---|---|
| Language | Python |
| Deployment | Modular microservice, containerised and networked with the server |
| Functionality | Real user-facing use case, e.g. summarisation, generation, Q&A |
| Model support | Cloud-based models (e.g. OpenAI API) and local models (e.g. GPT4All, LLaMA) |
| Optional bonus | Full RAG architecture using a vector database such as Weaviate |

---

## Environment and Deployment

All components must be fully containerised and runnable locally using a compose-based setup. This includes the client, the server-side services, the GenAI service, and the database. Each component must therefore have its own Dockerfile. The local setup must support end-to-end system execution through a `docker-compose.yml` file.

The local setup must be simple. The system must be runnable in **three or fewer commands**, for example by building and starting through `docker compose up`. The setup must provide sane defaults, which means that students should not rely on long manual configuration instructions or complex environment preparation steps. A new user must be able to start the system without reverse-engineering the project.

The same system must also be deployable to Kubernetes. Deployment may be implemented either through Helm charts or raw Kubernetes manifests. The project must support deployment on the course infrastructure via Rancher and also on one cloud environment (Azure). Configuration must be externalised using environment variables, Secrets, and similar mechanisms. Hardcoded credentials, hardcoded environment-dependent values, or manual configuration in the code are not acceptable.

| Aspect | Requirement |
|---|---|
| Containerisation | All components (server, client, GenAI, DB) must have their own Dockerfile |
| Local orchestration | `docker-compose.yml` must run the system end-to-end locally |
| Setup | Runnable in three or fewer commands; no complex manual ENV setup |
| Kubernetes | Deployable using Helm or raw manifests |
| Environments | Local infrastructure (Rancher) and a cloud option (Azure) |

---

## CI/CD

The system must include a working CI/CD pipeline implemented with **GitHub Actions**. The pipeline must reflect the actual lifecycle of the system and must be reliable enough that it can be treated as part of the system.

**Continuous Integration** must build and test all services. It must also perform static analysis or linting where appropriate. This means that the CI pipeline should validate the codebase before integration into the main branch and should fail when the system is not in a correct or stable state.

**Continuous Deployment** must automatically deploy to Kubernetes after merge to main. This deployment process must be reproducible and maintainable. The workflow must make correct use of secrets and environment-specific variables. Hardcoded tokens should be avoided.

| Aspect | Requirement |
|---|---|
| Tooling | GitHub Actions |
| CI tasks | Build and test all services; perform static analysis/linting |
| CD tasks | Automatically deploy to Kubernetes on merge to main |
| Configuration | Must use secrets and support environment-specific variables |

---

## Observability

The system must expose basic but meaningful operational visibility. This means that monitoring should not stop at "Prometheus is installed" or "Grafana is running." Instead, the monitored data must allow someone to understand whether the system is behaving correctly or incorrectly.

**Prometheus** must be used for metrics collection. At minimum, the project must track request count, latency, and error rate. These metrics should cover the core runtime behaviour of the system, especially on the server side and, where relevant, the GenAI component. **Grafana** must be used for visualisation, and dashboards must reflect key system metrics. These dashboards must be submitted as exported `.json` files. At least one meaningful alert rule must be configured, for example for service downtime or slow response time.

| Tool | Requirements |
|---|---|
| Prometheus | Metrics collection for at least request count, latency, and error rate |
| Grafana | Dashboards must reflect key system metrics (server, GenAI); must be submitted as `.json` |
| Alerts | At least one meaningful alert rule, e.g. service down or slow response time |

---

## Testing

Testing must validate the behaviour of the system. Tests must cover critical server-side logic, relevant parts of the GenAI component, and important client-side workflows and interactions.

Unit tests are mandatory for critical server and GenAI logic. Client-side tests should cover core workflows and interactions. All tests must run automatically in the CI pipeline. The pipeline should therefore act as the main enforcement point for system stability and should prevent broken changes from being merged.

| Aspect | Requirement |
|---|---|
| Unit Tests | Must cover critical server and GenAI logic |
| Client Tests | Should cover core workflows and interactions |
| CI Testing | All tests must run automatically in the CI pipeline |

---

## Engineering Artefacts

Teams must provide engineering artefacts that explain how the system is structured and how it works. These artefacts must reflect the actual implementation and must support understanding, reproducibility, and evaluation.

A high-level architecture description is required, together with decomposition into subsystems and their interfaces. UML-style diagrams are mandatory. Specifically, the project must include a **Subsystem Decomposition diagram**, a **Use Case Diagram**, and an **Analysis Object Model**. In addition, the system must provide API documentation through **OpenAPI/Swagger** and expose Swagger UI or an equivalent interface.

| Aspect | Requirement |
|---|---|
| Architecture | High-level system description |
| Decomposition | Subsystems and interfaces |
| Architecture Diagrams | UML-style diagrams: Subsystem Decomposition, Use Case Diagram, Analysis Object Model are mandatory |
| API documentation | Must provide OpenAPI/Swagger documentation and expose Swagger UI or equivalent |

---

## Deliverables

The final submission must include the complete codebase for the client, server, and GenAI services, together with all configuration required to run the system locally and to deploy it to Kubernetes. This includes Dockerfiles, the compose setup, deployment manifests or Helm charts, CI/CD workflows, and monitoring configuration.

The monitoring setup must include Prometheus and Grafana configuration, exported dashboards, and alert rule files. The testing suite must be included together with instructions for how to run it. Documentation in `README.md` must include setup instructions, architecture information, API documentation references, CI/CD and monitoring instructions, and an explanation of student responsibilities.

The project concludes with a **final presentation** and **individual oral examination**. You need to be prepared to defend the whole project, as well as your individual contribution within it. The final team presentation must include a live demo. Each student must present and explain their subsystem and be ready to answer technical questions.

| Deliverable | Description |
|---|---|
| Source Code | Complete codebase for server, client, and GenAI services |
| Docker Setup | Dockerfiles and `docker-compose.yml` for local setup |
| Kubernetes Deployment | Helm charts or raw Kubernetes YAMLs with setup instructions |
| Monitoring Configuration | Prometheus and Grafana config with exported dashboards and alert rules |
| Testing Suite | Unit/integration tests with instructions to run them |
| Documentation | `README.md` with setup guide, architecture, API docs, CI/CD and monitoring instructions, student responsibilities |

---

## Common Pitfalls and How to Avoid Them

### Effective Project

#### Reliability > Feature Count

One of the very common reasons for a team to fail is feature orientation. It might seem reasonable that the best strategy is to build as many features as possible in the shortest time. But quality can never beat quantity this way. If you cannot reliably deploy or run every new feature you create, you will eventually fail with the whole flow of project execution. One of the important DevOps principles is that **fast, reliable flow is more important than feature count**. We recommend the following strategy: keep the scope small, make the system deployable early, and iterate on it.

#### The System is a Single Pipeline

Every part of the system is a different component, but they are all interconnected and dependent on each other. A big mistake is to think about coding, deployment, and monitoring as three separate components. The best practice you can implement is to link every component of the system into one chain:

```
code → test → build → deploy → observe → improve
```

#### Reproducibility

The joke "it works on my machine" is as old as time. Try to ask yourself after each feature is implemented: *"Can someone else run your system without you?"* Typical failures include many manual steps and undocumented environments. The best advice is to make setup trivial: try to eliminate manual configuration and make sure to test the setup yourself from scratch at least a couple of times.

#### Visible System Behaviour

Monitoring might seem like the easiest and most fun part of the responsibilities. In reality, it requires significant effort. Many teams "install monitoring", but dashboards show nothing useful. Remember that all dashboards must be linked to system behaviour. You need to be able to monitor what can break. Visualisation of latency, failures, and load will help you and your team understand the system state.

---

### Patterns of Failure

#### Project as a Checklist

The easiest way to think about the project is to treat it as a collection of checkpoints you need to mark. Requirements should serve only as a starting point, on top of which you add your own ideas, understanding, and experience. Try to give every requirement meaning and connect it to real system behaviour.

#### Late Integration

What fails most projects one week before submission? Late integration. Too many teams build components separately and integrate them at the very end. Every time the result is the same: CI/CD breaks, deployment becomes unstable, and eventually the system is incomplete. **Start integration as early as possible.**

#### Fake CI/CD

It's easy to have CI/CD, but it's extremely hard to have good CI/CD. Very often the pipeline exists, but tests are meaningless or missing. Follow good CI/CD practices from the beginning: https://about.gitlab.com/blog/how-to-keep-up-with-ci-cd-best-practices/

#### GenAI as Decoration

If you are not interested in working with GenAI technology, you might catch yourself integrating it just as a checkbox. Use this opportunity to learn a new technology and think about real challenges modern teams are solving.

#### "I Will Document It Later"

Always start a new class or method with a short comment describing its purpose. Try to document as you go and make it a habit. It helps not only you, but also anyone who looks at your code later, and improves maintainability.

Further reading: https://www.aleksandrhovhannisyan.com/blog/writing-better-documentation/

---

### Team Culture

#### Other People Cannot Read Your Thoughts

If something is bothering you, bring it up for discussion as soon as possible. The earlier you do it, the easier it is to resolve. If you encounter a problem you cannot resolve within your team, contact your tutor or instructor.

#### Clear Roles and Responsibilities

Right after forming a team, one of the first things to discuss is individual responsibilities. Try to define roles similar to those in real projects, and throughout the project execution define responsibilities and accountability for various tasks. We recommend using a RACI matrix: https://www.atlassian.com/work-management/project-management/raci-chart

#### Individual Strength

Everyone in a team has different strengths and weaknesses. Good teams are technically strong, but great teams consist of people who complement each other. Be eager to teach what you know, and learn what you do not.
