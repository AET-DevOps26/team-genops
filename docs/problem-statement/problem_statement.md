# Problem Statement — JobReady

## Overview

Finding and securing a job is one of the most stressful and disorganised experiences a professional faces. Job seekers typically manage dozens of applications across multiple platforms, manually track deadlines and follow-ups in spreadsheets, write repetitive application documents from scratch, and prepare for interviews with little structured guidance. The process is fragmented, time-consuming, and heavily dependent on the individual's ability to self-organise and self-assess.

There is a need for a unified, intelligent platform that consolidates the entire job search journey into a single workflow: from building a structured professional profile, to tracking and managing applications, to generating tailored application documents, to rehearsing for interviews.

---

## Main Functionality

**JobReady** is an end-to-end job application and preparation hub that covers four core areas:

### 1. User Profile (Single Source of Truth)
When joining the platform, users build a structured professional profile by entering their information directly: work experience, education, skills, certifications, languages, and preferences. The platform uses a guided profile setup to collect clean, structured data. The profile can be updated and extended at any time as the user's background evolves.

This profile becomes the foundation for everything else in the platform. It is the single source of truth from which all personalised outputs — resumes, cover letters, fit analyses, and interview questions — are generated.

### 2. Application Tracking (via Email Integration)
Users connect their email account, and the system automatically detects, organises, and tracks job applications across all stages:

| Stage | Description |
|---|---|
| Applied | Application submitted, awaiting response |
| Follow-up | No response received; follow-up recommended |
| Interview | Interview scheduled or in progress |
| Offer | Offer received |
| Closed | Position filled or application withdrawn |

For each application, the platform provides contextual guidance on the next best action — for example, drafting a follow-up email when no response has been received after a defined period.

### 3. Application Preparation (Resume + Cover Letter + Fit Analysis)
When a user adds a new job application, they provide the job description. The platform then generates a full set of tailored application documents and insights based on their profile and the specific role:

- A **tailored resume** that highlights the most relevant aspects of the user's background for the position
- A **tailored cover letter** that connects the user's profile to the role's requirements in a compelling and personalised way
- A **candidate-role fit analysis** that identifies where the user's background aligns with the job description and where gaps exist, giving them an honest view of their competitiveness before applying

All three outputs are regenerated per application, since the same candidate should present themselves differently depending on the role and company context.

### 4. Interview Preparation (Mock Interviews + Feedback)
- A **chat-based mock interview** simulates realistic interview scenarios based on the target role and company context, delivering structured feedback on the quality, clarity, and completeness of the candidate's responses
- Bonus feature: A **voice-based mock interview** allows users to practise speaking naturally under realistic conditions and receive feedback on delivery in addition to content

---

## Intended Users

JobReady is designed for **job seekers** — regardless of whether they are fresh graduates entering the workforce for the first time, experienced professionals exploring new opportunities, or anyone in between.

---

## GenAI Integration

The GenAI component is central to the platform's value proposition and is integrated across multiple user-facing workflows rather than existing as a single isolated feature:

| GenAI Touchpoint | Description |
|---|---|
| Resume generation | Profile + job description → role-tailored resume |
| Cover letter generation | Profile + job description → tailored cover letter |
| Role fit analysis | Skills gap identification and strengths mapping per application |
| Interview question generation | Role- and company-specific question sets |
| Response evaluation | Structured feedback on clarity, structure, and completeness |
| Follow-up email drafting | Contextual suggestions based on application status and timeline |

The system supports both cloud-based LLMs (e.g. OpenAI API) and local models (e.g. GPT4All, LLaMA), ensuring the GenAI service can operate in different deployment environments.

---

## Example Scenarios

### Scenario 1 — Setting up and applying for the first time
A master's student joins the platform and fills in their profile: two internships, a thesis project in machine learning, and a set of technical and language skills. They add a software engineering role they found online, paste in the job description, and the platform generates a tailored resume, a cover letter, and a fit analysis that flags distributed systems as an experience gap — giving them time to address it proactively before the interview.

### Scenario 2 — Tracking multiple applications
A professional managing eight active applications connects their email account. The platform automatically detects and organises each application by stage. Two weeks after submitting to one company with no reply, the platform surfaces a follow-up suggestion with a pre-drafted email they can review and send directly from within the app.

### Scenario 3 — Preparing for an interview
A mid-career professional returning after a career break uses the mock interview feature to rehearse responses to behavioural questions for a product management role. After each answer, the platform provides specific feedback on structure and content, helping them build confidence before the real interview.

### Scenario 4 — Voice-based practice (bonus)
A candidate preparing for a senior engineering interview uses the voice mock interview feature to practise articulating complex technical answers under time pressure. They receive feedback not only on the content of their answers but also on pacing, filler words, and overall clarity of delivery.
