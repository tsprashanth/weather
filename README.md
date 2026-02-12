# Weather Forecast Service

A simple HTTP service that returns today's weather forecast for a given latitude/longitude, using the [National Weather Service API](https://www.weather.gov/documentation/services-web-api) as the data source.

## Prerequisites

- **Java 17+**
- **Maven 3.8+** (or use the Maven wrapper if included)

## Build & Run

```bash
# Build (and run tests)
mvn clean package

# Run the server
java -jar target/weather-service-1.0.0.jar
```

The server starts on **port 8080**.

## Usage

```bash
# Example: Kansas City area
curl "http://localhost:8080/forecast?latitude=39.7456&longitude=-97.0892"
```

**Response:**
```json
{
  "latitude": 39.7456,
  "longitude": -97.0892,
  "shortForecast": "Mostly Sunny",
  "temperatureF": 72,
  "temperatureCharacterization": "moderate"
}
```

**Note:** The NWS API only covers **US locations**. Coordinates outside the US will return an error.

## Temperature Characterization

| Range         | Label    |
|---------------|----------|
| >= 85°F       | hot      |
| 51°F – 84°F  | moderate |
| <= 50°F       | cold     |

## Design Decisions & Shortcuts

- **Spring Boot + RestClient**: Lightweight setup with minimal boilerplate. RestClient is the modern synchronous HTTP client in Spring 6.1+.
- **No caching**: The `/points` lookup from NWS is effectively static for a given coordinate and should be cached. Forecasts update hourly and could also benefit from a short TTL cache (e.g. Caffeine). Skipped for simplicity.
- **No retries / circuit breaker**: The NWS API can be intermittent. A production service would add resilience4j or Spring Retry.
- **Raw Map parsing**: The NWS JSON is parsed into `Map<String, Object>` rather than full typed DTOs. This is concise but fragile — a production service should define proper model classes.
- **First period = "today"**: We return the first forecast period, which is the current or next upcoming period (e.g. "Today", "This Afternoon", "Tonight"). A more robust version might let the caller specify which period they want.
- **No auth/rate-limiting** on our own endpoint.
- **Temperature thresholds** are subjective — 85°F/50°F seemed reasonable as general-purpose breakpoints.

## Running Tests

```bash
mvn test
```
