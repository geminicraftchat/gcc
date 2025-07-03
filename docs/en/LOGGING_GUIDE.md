# GeminiCraftChat Logging System Guide

## Overview

GeminiCraftChat plugin includes a comprehensive logging system that can record all API calls, chat records, error information, performance data, and player usage statistics.

## Logging Configuration

### Basic Configuration

Logging configuration in `config.yml`:

```yaml
logging:
  enabled: true                    # Enable/disable logging system
  directory: "logs"                # Log file directory
  format: "yyyy-MM-dd_HH-mm-ss"   # Log filename timestamp format
  separate_files: true             # Whether to save different log types separately
  retention_days: 30               # Log retention days (automatic cleanup)
```

### Log Type Configuration

```yaml
logging:
  include:
    chat: true                    # Chat records
    commands: true                # Command execution records
    errors: true                  # Error records
    model_changes: true           # Model switching records
    temperature_changes: true     # Temperature adjustment records
    api_calls: true              # API call details
    performance: true            # Performance metrics
    statistics: true             # Usage statistics
```

### Advanced Configuration

```yaml
logging:
  # File size limits
  max_file_size: "10MB"          # Maximum single file size
  max_files: 100                 # Maximum number of files per type
  
  # Performance settings
  async_logging: true            # Enable asynchronous logging
  buffer_size: 1000             # Log buffer size
  flush_interval: 5             # Buffer flush interval (seconds)
  
  # Privacy settings
  anonymize_players: false       # Anonymize player names in logs
  include_ip_addresses: false    # Include IP addresses in logs
  
  # Compression
  compress_old_logs: true        # Compress logs older than 7 days
  compression_format: "gzip"     # Compression format (gzip/zip)
```

## Log File Structure

### Directory Structure
```
plugins/GeminiCraftChat/logs/
├── chat/
│   ├── 2024-01-15_chat.log
│   ├── 2024-01-16_chat.log
│   └── ...
├── api/
│   ├── 2024-01-15_api.log
│   ├── 2024-01-16_api.log
│   └── ...
├── errors/
│   ├── 2024-01-15_errors.log
│   └── ...
├── statistics/
│   ├── daily_stats_2024-01-15.json
│   ├── player_stats.json
│   └── ...
└── archived/
    ├── 2024-01-01_chat.log.gz
    └── ...
```

### Log File Formats

#### Chat Logs
```
[2024-01-15 14:30:25] [CHAT] Player: Steve | Model: api1 | Persona: default
[2024-01-15 14:30:25] [USER] Steve: Hello AI!
[2024-01-15 14:30:27] [AI] Hello! How can I help you today?
[2024-01-15 14:30:27] [RESPONSE_TIME] 2.3s
```

#### API Call Logs
```
[2024-01-15 14:30:25] [API_REQUEST] Model: api1 | Player: Steve
URL: https://api.openai.com/v1/chat/completions
Headers: {"Content-Type": "application/json", "Authorization": "Bearer sk-***"}
Body: {"model": "gpt-3.5-turbo", "messages": [...], "temperature": 0.7}

[2024-01-15 14:30:27] [API_RESPONSE] Status: 200 | Time: 2.3s
Response: {"choices": [{"message": {"content": "Hello! How can I help you today?"}}]}
```

#### Error Logs
```
[2024-01-15 14:35:12] [ERROR] Player: Steve | Model: api1
Error: API request failed: 429 Too Many Requests
Stack trace:
  at GeminiService.sendGenericRequest(GeminiService.java:205)
  at GeminiService.sendMessage(GeminiService.java:108)
  ...
```

#### Performance Logs
```
[2024-01-15 14:30:00] [PERFORMANCE] Memory: 512MB/2GB | CPU: 15% | Active Players: 23
[2024-01-15 14:30:00] [METRICS] API Calls/min: 45 | Avg Response Time: 2.1s | Success Rate: 98.5%
```

## Log Management Commands

### View Statistics
```bash
/gcc logs stats
```
Shows current usage statistics:
- Total API calls
- Success/failure rates
- Average response times
- Most active players
- Model usage distribution

### Export Statistics
```bash
/gcc logs export
```
Exports detailed statistics to CSV files:
- `player_stats.csv` - Per-player usage statistics
- `model_stats.csv` - Per-model performance statistics
- `daily_stats.csv` - Daily usage trends

### Reset Statistics
```bash
/gcc logs reset
```
Resets all accumulated statistics counters.

### View Recent Errors
```bash
/gcc logs errors
```
Shows the last 10 error messages with timestamps.

### Archive Old Logs
```bash
/gcc logs archive
```
Manually archive logs older than configured retention period.

### Clean Up Logs
```bash
/gcc logs cleanup
```
Remove logs older than retention period and compress archives.

## Statistics and Analytics

### Player Statistics
```json
{
  "player_name": "Steve",
  "total_messages": 156,
  "total_api_calls": 156,
  "successful_calls": 152,
  "failed_calls": 4,
  "average_response_time": 2.1,
  "favorite_model": "api1",
  "favorite_persona": "teacher",
  "first_use": "2024-01-01T10:00:00Z",
  "last_use": "2024-01-15T14:30:25Z",
  "total_tokens_used": 45678
}
```

### Model Performance Statistics
```json
{
  "model_name": "api1",
  "display_name": "OpenAI GPT-3.5",
  "total_calls": 1234,
  "successful_calls": 1198,
  "failed_calls": 36,
  "success_rate": 97.1,
  "average_response_time": 2.3,
  "min_response_time": 0.8,
  "max_response_time": 15.2,
  "total_tokens": 567890,
  "average_tokens_per_call": 474
}
```

### Daily Usage Trends
```json
{
  "date": "2024-01-15",
  "total_calls": 234,
  "unique_players": 45,
  "peak_hour": 14,
  "peak_calls_per_hour": 67,
  "models_used": {
    "api1": 123,
    "api2": 89,
    "api3": 22
  },
  "average_response_time": 2.1
}
```

## Monitoring and Alerts

### Performance Monitoring
The logging system automatically tracks:
- **Response Times**: Average, min, max response times per model
- **Success Rates**: API call success/failure rates
- **Resource Usage**: Memory and CPU usage
- **Player Activity**: Active players and usage patterns

### Error Tracking
Automatic error categorization:
- **API Errors**: Connection timeouts, rate limits, authentication failures
- **Configuration Errors**: Invalid settings, missing API keys
- **Plugin Errors**: Internal errors, memory issues
- **Player Errors**: Invalid commands, permission issues

### Alert Conditions
Configure alerts for:
- High error rates (>5% failures)
- Slow response times (>10s average)
- High memory usage (>80% heap)
- API rate limit warnings

## Log Analysis Tools

### Built-in Analysis Commands
```bash
# View top players by usage
/gcc logs top players

# View model performance comparison
/gcc logs compare models

# View hourly usage patterns
/gcc logs usage hourly

# View error trends
/gcc logs errors trend
```

### External Analysis
Log files are designed to be compatible with:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Splunk**
- **Grafana** with log data sources
- **Custom scripts** for CSV/JSON analysis

### Sample Analysis Queries

#### Find High-Usage Players
```bash
grep "CHAT" logs/chat/*.log | cut -d'|' -f2 | sort | uniq -c | sort -nr | head -10
```

#### Calculate Average Response Times
```bash
grep "RESPONSE_TIME" logs/chat/*.log | awk '{print $3}' | sed 's/s//' | awk '{sum+=$1; count++} END {print sum/count}'
```

#### Find Error Patterns
```bash
grep "ERROR" logs/errors/*.log | cut -d':' -f3- | sort | uniq -c | sort -nr
```

## Privacy and Compliance

### Data Protection
- **Player Anonymization**: Option to hash player names
- **Message Content**: Configurable inclusion of chat content
- **IP Address Logging**: Optional IP address recording
- **Data Retention**: Automatic cleanup after configured period

### GDPR Compliance
- **Right to be Forgotten**: Commands to remove specific player data
- **Data Export**: Export player's data in machine-readable format
- **Consent Tracking**: Log player consent for data processing

### Security
- **Log File Permissions**: Restricted access to log files
- **API Key Protection**: API keys are masked in logs
- **Encryption**: Optional log file encryption
- **Audit Trail**: Complete audit trail of all actions

## Troubleshooting

### Common Issues

**Q: Logs not being created?**
A: Check:
1. Logging is enabled in config.yml
2. Plugin has write permissions to logs directory
3. Disk space is available
4. No file system errors in server logs

**Q: Log files too large?**
A: Configure:
1. Reduce retention_days
2. Set max_file_size limit
3. Enable log compression
4. Exclude verbose log types

**Q: Performance impact from logging?**
A: Optimize:
1. Enable async_logging
2. Increase buffer_size
3. Reduce flush_interval
4. Disable unnecessary log types

**Q: Cannot find specific events?**
A: Use:
1. Grep commands for text search
2. Check correct log file type
3. Verify timestamp format
4. Enable debug mode for more details

### Debug Logging
Enable debug mode for detailed logging:
```yaml
debug:
  enabled: true
  log_level: "DEBUG"
  include_stack_traces: true
```

This will log:
- Detailed API request/response data
- Configuration loading steps
- Internal plugin operations
- Performance metrics

## Best Practices

### Configuration
1. **Enable Essential Logs**: Always enable chat, errors, and api_calls
2. **Set Reasonable Retention**: 30-90 days depending on usage
3. **Use Compression**: Enable compression for old logs
4. **Monitor Disk Space**: Set up disk space monitoring

### Performance
1. **Async Logging**: Always enable for high-traffic servers
2. **Buffer Settings**: Tune buffer size based on usage
3. **Separate Files**: Use separate files for different log types
4. **Regular Cleanup**: Set up automated log cleanup

### Security
1. **Restrict Access**: Limit log file access to administrators
2. **Anonymize Data**: Consider anonymizing player data
3. **Secure Storage**: Store logs on secure, backed-up storage
4. **Regular Audits**: Review logs regularly for security issues

### Analysis
1. **Regular Reviews**: Review logs weekly for issues
2. **Trend Analysis**: Track usage trends over time
3. **Performance Monitoring**: Monitor response times and error rates
4. **Capacity Planning**: Use logs for server capacity planning

---

**Comprehensive logging for better insights! 📊📋**
