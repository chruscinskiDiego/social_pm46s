# Physical activity social network

Academic Android project for the Advanced Topics in Mobile Programming course (PM46S-2025-1), part of the Systems Analysis and Development degree at Universidade Tecnológica Federal do Paraná (UTFPR).

## Project team

**Scrum Master**

- Marcos Felipe Altenhofen

**Developers**

- Diego Chruscinski de Souza
- João Gabriel Jarutais
- Gustavo Moretto Dalla Costa
- Geovane de Campos Soares

## Project overview

The application is designed to measure users' physical activity and provide a social experience around exercise. Its coursework specification uses the device accelerometer to detect movement, send activity records to a server, and present individual and group rankings.

The goal is to let users monitor movement, share results, compare participation and form groups. The target audience is Android users interested in recording activity, joining challenges and interacting with others.

## Coursework requirements

- Access through a social login provider such as Google or Facebook.
- Start and stop accelerometer-based activity monitoring after login.
- Send activity records with user identification and timestamps to a remote server.
- Display individual activity rankings.
- Create groups for rankings and interaction.

These describe the coursework requirements; consult the current implementation when evaluating the status of each feature.

## Functional design

### Authentication

Social login through Firebase Authentication obtains a user name or identifier for profiles and rankings.

### Main screen

- Start monitoring: enables accelerometer readings and indicates active monitoring.
- Stop monitoring: stops readings.
- Navigation to rankings and group management.

### Activity monitoring

The planned foreground service continuously reads the accelerometer, processes movement levels and periodically sends the user identifier, detected activity and measurement time to Cloud Firestore. Battery use is a design consideration.

### Rankings

Individual rankings display the user's name or nickname, accumulated activity or score and overall position. The design includes group views and updates from Cloud Firestore.

### Groups

The design includes group creation, joining existing groups, viewing members and ranking groups by combined or average activity.

## Technologies

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| NoSQL storage | Cloud Firestore |
| Authentication | Firebase Authentication and social providers |
| Motion sensor | Android SensorManager, TYPE_ACCELEROMETER |
| Backend communication | Firebase SDK for Android |
| Dependencies | Gradle |
| IDE | Android Studio |

## Documented Git workflow

The team documented the following branch roles:

- `prd`: stable production branch; no direct commits.
- `qa`: integration testing and final adjustments before a release.
- `dev`: ongoing development.

The documented promotion cycle is `dev` → `qa` → `prd`. This records the intended team workflow; branch names in the current repository may differ.

### Supporting branches

- `[id]-feature/nome-da-feature`: new functionality.
- `[id]-fix/nome-do-fix`: bug fixes and improvements.
- `[id]-hotfix/nome-do-hotfix`: urgent production fixes.
- `[id]-docs/nome-da-documentacao`: documentation changes.

Names use kebab-case and a numeric task identifier. Original examples:

```text
102-feature/adicionar-feed
115-fix/erro-login
120-release/v1-0-0
```

### Pull requests

- Submit changes through pull requests to the appropriate branch.
- Integrate features and fixes into dev.
- After validation, merge dev into qa.
- After final testing, merge qa into prd and synchronize dev as needed.
- Request review from the Scrum Master or a designated teammate.

### Commit conventions

- `feat:`: new feature.
- `fix:`: bug fix.
- `docs:`: documentation.
- `refactor:`: restructuring without changing behavior.
- `test:`: tests.
- `chore:`: maintenance and configuration.

Example: `feat: implement user registration`.
