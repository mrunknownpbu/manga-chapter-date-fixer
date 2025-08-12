# Manga Chapter Date Fixer

A tool to fetch and update manga/comic chapter release dates in Komga and Kavita libraries.

## Features

- Fetches release dates from MangaUpdates, MangaDex, AniList, and more
- Updates Komga and Kavita libraries via their APIs
- Configurable provider priorities & dry-run support

## Usage

1. Edit `chapterReleaseDateProviders.yaml` with your provider/API info.
2. Build and run:
   ```
   ./gradlew run --args='chapterReleaseDateProviders.yaml'
   ```
3. Check logs for updates.

## Roadmap

- [ ] Implement real API integrations for each provider
- [ ] Add error handling and logging
- [ ] Add tests and CI
- [ ] Refine matching and update logic

MIT License