# Manga Chapter Date Fixer

A tool to fetch and update manga/comic chapter release dates in Komga and Kavita libraries.

## Features

- Fetches release dates from MangaUpdates, MangaDex, AniList, and more
- Updates Komga and Kavita libraries via their APIs
- Configurable provider priorities & dry-run support
- **Web API server** for remote execution and monitoring
- **Preview and targeted update endpoints** for userscript/browser integration
- Native Ubuntu support with OpenJDK 17+ and Kotlin 1.9+
- Docker support for containerized deployment

## Requirements

- **Ubuntu (Native)**: OpenJDK 17+ and Kotlin 1.9+
- **Docker (Container)**: Docker Engine

## Configuration

The application loads its configuration from a YAML file. You can specify the configuration file location via:

1. **CLI argument**: `./gradlew run --args="/path/to/config.yaml"`
2. **Environment variable**: `CONFIG_PATH=/path/to/config.yaml`
3. **Default**: `chapterReleaseDateProviders.yaml` (in current directory)

Edit the configuration file with your provider/API information before running.

## Web API Server

The application runs a web server on **port 1996** that exposes the following endpoints:

### Endpoints

- **GET /health** - Health check endpoint
  ```bash
  curl http://localhost:1996/health
  ```
  Response: `{"status":"ok"}`

- **POST /run** - Trigger library-wide chapter date updates
  ```bash
  curl -X POST http://localhost:1996/run
  ```

- **GET /preview?title=...&chapter=...** - Preview best date and attempts for a single chapter
  ```bash
  curl "http://localhost:1996/preview?title=One%20Piece&chapter=1000"
  ```
  Response includes `best` structured finding and `attempts`.

- **POST /update-chapter?library=komga|kavita&title=...&chapter=...** - Update a specific chapter
  ```bash
  curl -X POST "http://localhost:1996/update-chapter?library=komga&title=One%20Piece&chapter=1000"
  ```

### Userscript (similar to @Snd-R/komf-userscript)

You can integrate with Komga/Kavita UIs using a Tampermonkey userscript that calls `/preview` and `/update-chapter`.

Example snippet:
```javascript
// ==UserScript==
// @name         Manga Chapter Date Fixer Helper
// @namespace    http://tampermonkey.net/
// @version      0.1
// @match        http://*/series/*
// @match        http://*/manga/*
// @grant        GM_xmlhttpRequest
// ==/UserScript==
(function() {
  const API = 'http://localhost:1996';
  function call(endpoint, method='GET') {
    return fetch(endpoint, { method }).then(r=>r.json());
  }
  // Example button that previews date for visible title/chapter
  function addButton() {
    const title = document.querySelector('h1')?.textContent?.trim();
    const chapter = document.querySelector('[data-chapter]')?.getAttribute('data-chapter');
    if (!title || !chapter) return;
    const btn = document.createElement('button');
    btn.textContent = 'Preview Release Date';
    btn.onclick = async () => {
      const res = await call(`${API}/preview?title=${encodeURIComponent(title)}&chapter=${encodeURIComponent(chapter)}`);
      alert(`Best: ${res.best?.normalizedDate || 'N/A'}\nSource: ${res.best?.source || '-'}\nAttempts: ${res.attempts?.join(' | ')}`);
    };
    document.body.appendChild(btn);
  }
  window.addEventListener('load', addButton);
})();
```

## Ubuntu CLI Usage

### Prerequisites

Ensure you have OpenJDK 17+ installed:
```bash
sudo apt update
sudo apt install openjdk-17-jdk
java -version  # Should show version 17 or higher
```

### Running the Application

1. **Clone the repository:**
   ```bash
   git clone https://github.com/mrunknownpbu/manga-chapter-date-fixer.git
   cd manga-chapter-date-fixer
   ```

2. **Edit configuration:**
   ```bash
   cp chapterReleaseDateProviders.yaml config.yaml
   # Edit config.yaml with your API keys and settings
   ```

3. **Run the web server:**
   ```bash
   ./gradlew run
   ```
   The application will start a web server on port 1996.

4. **Use the API endpoints:**
   ```bash
   # Health check
   curl http://localhost:1996/health
   
   # Trigger update
   curl -X POST http://localhost:1996/run
   ```

5. **Run with custom config (via environment variable):**
   ```bash
   CONFIG_PATH=config.yaml ./gradlew run
   ```

### Building a Distribution

To create a standalone distribution:
```bash
./gradlew build
./gradlew distZip
# Find the distribution in build/distributions/
```

## Docker Usage

### Building the Docker Image

```bash
# Build with default tag
docker build -t manga-chapter-date-fixer .

# Build with custom tag
docker build -t myuser/manga-chapter-date-fixer:v1.0 .
```

### Running the Docker Container

1. **Run with default config (mounted):**
   ```bash
   docker run -p 1996:1996 -v $(pwd)/chapterReleaseDateProviders.yaml:/app/chapterReleaseDateProviders.yaml manga-chapter-date-fixer
   ```

2. **Run with custom config file:**
   ```bash
   docker run -p 1996:1996 -v $(pwd)/my-config.yaml:/app/config.yaml -e CONFIG_PATH=/app/config.yaml manga-chapter-date-fixer
   ```

3. **Use the API endpoints:**
   ```bash
   # Health check
   curl http://localhost:1996/health
   
   # Trigger update
   curl -X POST http://localhost:1996/run
   ```

### Automated Build and Push Script

Use the provided script to build and push to Docker Hub:

```bash
# Make script executable (if not already)
chmod +x build_and_push.sh

# Build and push to Docker Hub
./build_and_push.sh myuser/manga-chapter-date-fixer

# Build and push with specific tag
./build_and_push.sh myuser/manga-chapter-date-fixer v1.0
```

**Note:** Make sure you're logged in to Docker Hub before pushing:
```bash
docker login
```

## Docker Hub Deployment

### Manual Deployment

1. **Tag your image:**
   ```bash
   docker tag manga-chapter-date-fixer myuser/manga-chapter-date-fixer:latest
   ```

2. **Push to Docker Hub:**
   ```bash
   docker push myuser/manga-chapter-date-fixer:latest
   ```

### Automated Deployment

Use the provided script:
```bash
./build_and_push.sh myuser/manga-chapter-date-fixer
```

### Running from Docker Hub

Once pushed, anyone can run your image:
```bash
# Pull and run the latest version with API exposed
docker run -p 1996:1996 -v $(pwd)/config.yaml:/app/config.yaml myuser/manga-chapter-date-fixer:latest

# Run specific version
docker run -p 1996:1996 -v $(pwd)/config.yaml:/app/config.yaml myuser/manga-chapter-date-fixer:v1.0
```

## Development

### Project Structure
```
.
├── src/                          # Kotlin source files
│   ├── main.kt                   # Application entry point
│   ├── config/                   # Configuration classes
│   ├── fetcher/                  # Release date providers
│   ├── komga/                    # Komga API integration
│   └── kavita/                   # Kavita API integration
├── chapterReleaseDateProviders.yaml  # Default configuration
├── build.gradle.kts              # Build configuration
├── Dockerfile                    # Docker build instructions
├── build_and_push.sh            # Docker automation script
└── README.md                     # This file
```

### Building from Source

```bash
# Clean build
./gradlew clean build

# Run tests (if any)
./gradlew test

# Generate application distribution
./gradlew distZip
```

## Roadmap

- [ ] Implement real API integrations for each provider
- [ ] Add error handling and logging
- [ ] Add tests and CI
- [ ] Refine matching and update logic
- [ ] Userscript: auto-detect series/chapter IDs in Komga/Kavita UI and call `/update-chapter`

## License

MIT License - see [LICENSE](LICENSE) file for details.