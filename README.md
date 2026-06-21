# HighCore MC Discord Bot

A comprehensive Discord bot for the HighCore MC Minecraft server, built with Java and Spring Boot. This bot manages server operations, user interactions, and community engagement.

## Features

### Server Management
- **Server Log Monitoring**: Real-time monitoring of RCON server logs.
- **Server Control**: Start, stop, and restart the Minecraft server.
- **Whitelist Management**: Manage the server whitelist.

### Ticketing System
- **Ticket Channels**: Private channels for user support.
- **Staff Alerts**: Automatic pings to staff members on ticket creation.
- **Ticket Operations**: Close, claim, and manage tickets.

### Community & Moderation
- **Role Management**: Automated role assignments (e.g., "Verified Player").
- **NSFW Filtering**: Built-in NSFW image detection using ONNX.
- **QR Code Scanner**: Detects malicious QR codes in images.

### Voice System (Projector)
- **Voice Channel Management**: Automatic creation of voice channels for voice chats.
- **Category Management**: Organization of voice channels within categories.
- **Temporary Channels**: Auto-cleanup of unused voice channels.

### Game Integration
- **BungeeBridge**: Real-time chat synchronization between Discord and Minecraft (BungeeCord).
- **Staff Commands**: Commands to view and manage online players.

## Technology Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.5.0
- **Database**: PostgreSQL
- **Discord API**: JDA (Java Discord API)
- **AI/ML**: ONNX Runtime (for image moderation)

## Project Structure

The project is organized into several modules:

- `src/main/java/com/integrafty/opexy`: Main application package.
- `com.integrafty.opexy.listener`: Event listeners for Discord events.
- `com.integrafty.opexy.command`: Slash command implementations.
- `com.integrafty.opexy.service`: Core business logic and service classes.
- `com.integrafty.opexy.config`: Configuration classes.
- `com.integrafty.opexy.entity`: JPA database entities.
- `com.integrafty.opexy.repository`: Database repositories.
- `com.integrafty.opexy.util`: Utility classes and helpers.