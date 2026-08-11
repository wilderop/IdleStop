# IdleStop

Paper 26.2 plugin that automatically runs `/stop` once two conditions are both true:

1. The server has been online for at least a configurable minimum uptime (default **24 hours**).
2. No players have been online for a configurable period (default **60 seconds**).

When a player joins during the empty countdown, the timer is **silently reset**. No messages are sent to players.

## Configuration (`config.yml`)

```yaml
min-uptime-hours: 24
empty-seconds: 60
check-interval-ticks: 20   # 20 ticks = 1 second
```

## Building

```bash
mvn clean package
```

The jar will be at `target/IdleStop-1.0.0.jar`.

## Notes

- Uptime is measured from the moment the plugin enables (i.e. server start for practical purposes).
- The plugin logs every relevant state change to the console (countdown start, cancel, waiting for uptime, and final stop).
- Designed for servers that auto-restart after `/stop` (e.g. a simple `while true; do ...; done` wrapper).
