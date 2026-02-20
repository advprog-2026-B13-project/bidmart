# Bidmart Frontend

Web interface built with Next.js and React.

## Overview

Frontend application for Bidmart.

**Technology Stack:**
- Framework: Next.js 16.1.6
- Language: TypeScript
- UI Framework: React 19.2.3
- Styling: Tailwind CSS 4
- Package Manager: pnpm
- Linting: ESLint 9

## Prerequisites

- Node.js 20+ (or compatible version)
- pnpm 8+

## Installation

1. Ensure Node.js and pnpm are installed:
```bash
node --version
pnpm --version
```

2. Navigate to the frontend directory:
```bash
cd apps/bidmart-fe
```

3. Install dependencies:
```bash
pnpm install
```

## Running

### Development server
```bash
pnpm dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

The app auto-refreshes as you edit files.

### Production build
```bash
pnpm build
```

### Start production server
```bash
pnpm start
```

Runs on [http://localhost:3000](http://localhost:3000)

## Development

### Lint code
```bash
pnpm lint
```

## Project Structure

```
app/
├── layout.tsx        # Root layout
├── page.tsx          # Home page
└── globals.css       # Global styles
public/               # Static assets
next.config.ts        # Next.js configuration
eslint.config.mjs     # ESLint configuration
tsconfig.json         # TypeScript configuration
```

## Troubleshooting

**Port 3000 already in use:**
```bash
pnpm dev -- -p 3001
```

**Clear cache:**
```bash
rm -rf .next node_modules
pnpm install
```
