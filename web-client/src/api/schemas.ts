// The single sanctioned bridge to the generated OpenAPI types.
// This is the ONLY file allowed to import from `~/generated/openapi`.
// Everything else imports the aliases below — never `components['schemas']` directly,
// and never a hand-written `interface` that duplicates a spec type.
//
// These are aliases (pointers), not copies: delete a schema from openapi.yaml and the
// corresponding line here becomes a compile error — the single-source-of-truth working
// for you, not against you.
import type { components } from "~/generated/openapi";

/** Generic accessor for any schema: `Schemas['Application']`. */
export type Schemas = components["schemas"];

// Named aliases for the high-traffic types used across services/components.
export type UserResponse = Schemas["UserResponse"];
export type LoginRequest = Schemas["LoginRequest"];
export type RegisterRequest = Schemas["RegisterRequest"];

/** Unified backend error body: `{ code, message, details }`. */
export type ApiError = Schemas["Error"];

// Applications (application service)
export type JobApplication = Schemas["JobApplication"];
export type ApplicationList = Schemas["ApplicationList"];
export type ApplicationStage = Schemas["ApplicationStage"];
export type CreateApplicationRequest = Schemas["CreateApplicationRequest"];
export type UpdateApplicationRequest = Schemas["UpdateApplicationRequest"];

// Job-posting extraction (genai service)
export type JobPostingExtractRequest = Schemas["JobPostingExtractRequest"];
export type JobPostingExtraction = Schemas["JobPostingExtraction"];

// Profile & generated documents (document service)
export type Profile = Schemas["Profile"];
export type ProfileRequest = Schemas["ProfileRequest"];
export type ProfileAggregateResponse = Schemas["ProfileAggregateResponse"];
export type WorkExperience = Schemas["WorkExperience"];
export type WorkExperienceRequest = Schemas["WorkExperienceRequest"];
export type Education = Schemas["Education"];
export type EducationRequest = Schemas["EducationRequest"];
export type Skill = Schemas["Skill"];
export type SkillRequest = Schemas["SkillRequest"];
export type SkillLevel = Schemas["SkillLevel"];
export type Language = Schemas["Language"];
export type LanguageRequest = Schemas["LanguageRequest"];
export type LanguageProficiency = Schemas["LanguageProficiency"];
export type GeneratedDocument = Schemas["GeneratedDocument"];
export type GeneratedDocumentList = Schemas["GeneratedDocumentList"];
export type GeneratedDocumentType = Schemas["GeneratedDocumentType"];
export type CreateGeneratedDocumentRequest =
  Schemas["CreateGeneratedDocumentRequest"];
