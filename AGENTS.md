# AGENTS.md

This document provides guidance for automated agents and contributors working on this repository.

## Project Overview

This project consists of:

- Backend: Java Spring Boot application using Maven as the build tool.
- Frontend: React application built with Vite.

Repository structure:

```
/
├── (Spring Boot backend source code)
├── frontend/   # Vite + React frontend
└── pom.xml
```

The backend code lives in the root directory, while the frontend code lives in the `frontend` directory.

## Backend (Spring Boot)

All backend-related commands must be executed from the root folder.

### Common Commands

- Start the backend server:

  ```bash
  ./mvnw spring-boot:run
  ```

- Run backend tests:

  ```bash
  ./mvnw test
  ```

## Frontend (Vite + React)

All frontend-related commands must be executed from the `frontend` folder.

### Common Commands

- Install the frontend dependencies:

  ```bash
  npm run install
  ```

- Start the frontend development server:

  ```bash
  npm run dev
  ```

- Run the frontend tests:

  ```bash
  npm run test
  ```

- Run ESlint codestyle checks:

  ```bash
  npm run lint
  ```

- Build the frontend for production:

  ```bash
  npm run build
  ```

## Development Workflow

### Branching Strategy

- Each new feature must be implemented in a separate branch.
- Branch names should:
  - Be lowercase
  - Use words separated by hyphens (`-`)
  - Describe the feature clearly

Example branch name: `user-profile-picture`

### Pull Requests

- Once a feature is complete, open a ull request.
- The pull request must include:
  - A clear title.
  - A description explaining what feature was implemented and any relevant details.

### Continuous Integration expectations

All pull requests are expected to pass the continuous integration checks defined in the `./github/workflows/ci.yml` GitHub Actions workflow file before being merged.

## Notes for Agents

- Ensure backend and frontend commands are run in their correct directories.
- Do not commit directly to the main branch.
- Do not add unnecessary comments to the code.
- Keep changes focused on the feature described by the branch name.
- Follow existing project conventions and structure when adding or modifying code.
