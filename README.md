# Tend

A habit tracker. Android app (Kotlin + XML) and a REST API (ASP.NET Core + SQL Server).

## Run it
**1. API** (`api/`): open `api/TendApi.sln` in Visual Studio 2022, choose the **http** launch profile and run. Swagger test page: http://localhost:5080/swagger. The `TendDb` database is created on first run (SQL Server LocalDB; change the connection string in `appsettings.json` to use another server).

**2. App** (`android/Tend/`): open the `android/Tend` folder in Android Studio, let Gradle sync, then run on an emulator. The emulator reaches the API at `10.0.2.2:5080` (set in `Config.kt`). For a real phone, put your PC's IP in `Config.kt`.

## Features
- Register and log in, passwords hashed with BCrypt on the server, JWT sessions
- Settings: display name, profile photo, week start, accent colour, daily reminder, change password
- Habits with a Mon-Sun check-in strip and streaks
