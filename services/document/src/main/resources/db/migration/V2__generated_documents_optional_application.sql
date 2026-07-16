-- Generated documents no longer require a target application.
--
-- A document is owned by its user; being filed under an application is a property it may or
-- may not have. Requiring application_id made a whole legitimate case unrepresentable — "just
-- tighten up my general resume" — and forced users to invent a fake application to save one.
--
-- Widening only: existing rows keep their application, and nothing that reads them changes.

ALTER TABLE document.cover_letters ALTER COLUMN application_id DROP NOT NULL;
ALTER TABLE document.resumes ALTER COLUMN application_id DROP NOT NULL;
