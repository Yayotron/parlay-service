# Parlay Recommendation Service

This is a Kotlin-based Spring Boot service that provides parlay recommendations for Premier League matches using the API-Football.

## Features
- **Match Analysis**: Fetches Premier League matches for a given date.
- **Prediction Engine**: Analyzes team form, H2H, and API predictions to suggest the best betting options.
- **Risk Scoring**: Categorizes recommendations into Low Risk and High Risk parlays.
- **GraalVM Support**: Configured for native image compilation.

## Prerequisites
- Java 17
- Maven
- API-Football Key (from [api-football.com](https://www.api-football.com/))

## Configuration
Set your API key in `src/main/resources/application.yml` or via environment variable:
```bash
export API_FOOTBALL_KEY=your_actual_api_key
```

## Running the Service
```bash
mvn spring-boot:run
```

## API Usage
### Get Parlay Recommendations
**Endpoint**: `GET /parlays`

**Parameters**:
- `date` (required): The date of the matches in `YYYY-MM-DD` format.

**Example Request**:
```bash
curl "http://localhost:8080/parlays?date=2026-01-10"
```

**Example Response**:
```json
{
  "lowRiskParley": {
    "betting": {
      "Arsenal vs Chelsea": "Arsenal Wins",
      "Liverpool vs Everton": "Liverpool Wins",
      "Man City vs Spurs": "Both Score"
    },
    "successProbability": "75%",
    "expectedReturn": "150%"
  },
  "highRiskParley": {
    "betting": {
      "Leicester vs Wolves": "Leicester Wins",
      "Brighton vs Fulham": "Goals Over 2.5",
      "West Ham vs Aston Villa": "Aston Villa Wins"
    },
    "successProbability": "45%",
    "expectedReturn": "400%"
  }
}
```
