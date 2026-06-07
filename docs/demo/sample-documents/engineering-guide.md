# Engineering Guide Demo

This document is a fictional engineering guide for the Phase 6 evaluation demo. It does not describe any real production system.

## Branch Naming

Feature branches should use the format `feature/<short-topic>`. Bug fix branches should use `fix/<short-topic>`. Release preparation branches should use `release/<version>`. Branch names should be lowercase and use hyphens instead of spaces.

## Commit Message Rules

Commit messages should start with a type followed by a short description, such as `feat: add document upload` or `fix: handle empty retrieval results`. The first line should stay under 72 characters. A commit should describe one logical change.

## Code Review Rules

Every pull request requires at least one reviewer approval before merge. Reviewers should check correctness, readability, tests, and migration impact. Large pull requests should include a short implementation note so reviewers can understand the main change path quickly.

## Release Flow

Release candidates are created from the release branch. Before release, the team runs automated tests, checks database migrations, and verifies the main user flow in a staging environment. Production release notes should include user-visible changes and rollback notes.
