# VerseGuide

**VerseGuide** is a Telegram bot that helps people find Bible verses and brief reflections tailored to how they feel or what they’re going through. Users simply type a word or short phrase (e.g., “anxiety”, “hope”, “forgiveness”), and the bot returns a verse + short encouraging reflection.

For developers: it’s built with Spring Boot, uses the Telegram Bot API, integrates an AI model (via Spring AI) for verse/reflection generation, and supports rate-limiting + premium subscription functionality.

---

## Table of Contents

- [Features](#features)
- [Why VersionGuide](#why-versionguide)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Installation](#installation)
    - [Configuration](#configuration)
    - [Running Locally](#running-locally)
- [Usage](#usage)
- [Subscription & Premium Access](#subscription--premium-access)
- [Deployment](#deployment)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

---

## Features

- 🕊️ Search for a single verse and reflection based on user input
- 🎚️ Daily usage limit for free users (configurable)
- 💎 Premium subscription for unlimited access
- 🤖 AI-driven verse + reflection generation via Spring AI
- 📱 Telegram bot interface – chat-based & intuitive
- 👤 Basic commands: `/start`, `/help`, `/status`, `/subscribe`, `/reset`
- ☁️ Easily deployable via Docker, CI/CD, Railway/AWS

---

## Why VerseGuide

Many people turn to spiritual texts for comfort, guidance or inspiration — but finding the *right* verse *right then* can feel hard. VerseGuide bridges that gap: you type how you feel, and it suggests a carefully-chosen verse + reflection that speaks into your situation.

From a developer perspective, it’s also a clean, modern integration of Telegram bots + Spring Boot + AI + payments + deployment — a whole stack in one lightweight app.

---

## Tech Stack

- **Language**: Java 23
- **Framework**: Spring Boot (3.x)
- **Telegram API**: [telegrambots](https://github.com/rubenlagus/TelegramBots) library
- **AI Integration**: Spring AI (via `ChatClient`)
- **Usage Tracking**: In-memory (`ConcurrentHashMap`) + optional DB for premium users
- **Payment Gateways**: Stripe (£1/month) + Paystack (₦ plan)
- **Deployment Options**: Docker, Railway.app, AWS Elastic Beanstalk, GitHub Actions CI/CD

---

## Getting Started

### Prerequisites

- Java 23 or above
- Maven 3.x
- Telegram Bot Token (via [@BotFather] on Telegram)
- (For premium features) Stripe account or Paystack account

### Installation

```bash
# Clone the repo
git clone https://github.com/your-username/verseguide.git
cd verseguide

# Build the project
mvn clean package -DskipTests

### Configuration

In src/main/resources/application.properties, set:
telegram.bot.username=VerseGuideBot
telegram.bot.token=YOUR_TELEGRAM_BOT_TOKEN
telegram.bot.daily-limit=5
verse.api.url=http://localhost:8080/api/verse

You can also override via environment variables or in your deployment environment.

Running Locally
mvn spring-boot:run

Later, open Telegram and start a chat with your bot (@VerseGuideBot) and type /start.
Usage

/start – shows welcome & how to use

/help – shows help message

/status – shows your remaining free requests today

/subscribe – shows payment options for Premium

/reset – resets your daily count (for test/dev)

Any normal message (e.g. “peace in anxious times”) – triggers a verse/reflection response

Free users: limited number of queries per day (configurable via telegram.bot.daily-limit).
Premium users: unlimited access once payment is active.

Project Structure
src/
 ├── main/
 │    ├── java/com/chikere/verseguide/
 │    │    ├── bot/
 │    │    │     VerseGuideBot.java
 │    │    ├── controller/
 │    │    │     VerseController.java
 │    │    ├── config/
 │    │    │     AiModelConfig.java
 │    │    ├── payment/
 │    │    │     StripeWebhookController.java
 │    │    │     PaystackWebhookController.java
 │    ├── resources/
 │           application.properties
 ├── test/
 └── ...

License

This project is licensed under the MIT License – see the LICENSE file for details.