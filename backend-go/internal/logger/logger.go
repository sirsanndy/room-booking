package logger

import (
	"encoding/json"
	"fmt"
	"io"
	"os"
	"time"

	"meetingroom/internal/config"
)

// Logger provides structured logging
type Logger struct {
	output io.Writer
	level  LogLevel
	format string
}

type LogLevel int

const (
	DEBUG LogLevel = iota
	INFO
	WARN
	ERROR
	FATAL
)

var levelNames = map[LogLevel]string{
	DEBUG: "DEBUG",
	INFO:  "INFO",
	WARN:  "WARN",
	ERROR: "ERROR",
	FATAL: "FATAL",
}

type LogEntry struct {
	Timestamp string                 `json:"timestamp"`
	Level     string                 `json:"level"`
	Message   string                 `json:"message"`
	Fields    map[string]interface{} `json:"fields,omitempty"`
}

// NewLogger creates a new logger instance
func NewLogger(cfg config.LogConfig) (*Logger, error) {
	var output io.Writer
	if cfg.Output == "stdout" || cfg.Output == "" {
		output = os.Stdout
	} else {
		// TODO: Implement file rotation based on maxSize, maxBackups, maxAge
		file, err := os.OpenFile(cfg.Output, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
		if err != nil {
			return nil, fmt.Errorf("failed to open log file: %w", err)
		}
		output = file
	}

	level := parseLogLevel(cfg.Level)

	return &Logger{
		output: output,
		level:  level,
		format: cfg.Format,
	}, nil
}

func parseLogLevel(level string) LogLevel {
	switch level {
	case "debug":
		return DEBUG
	case "info":
		return INFO
	case "warn":
		return WARN
	case "error":
		return ERROR
	case "fatal":
		return FATAL
	default:
		return INFO
	}
}

func (l *Logger) log(level LogLevel, message string, fields map[string]interface{}) {
	if level < l.level {
		return
	}

	entry := LogEntry{
		Timestamp: time.Now().Format(time.RFC3339),
		Level:     levelNames[level],
		Message:   message,
		Fields:    fields,
	}

	var output string
	if l.format == "json" {
		data, _ := json.Marshal(entry)
		output = string(data) + "\n"
	} else {
		// Text format
		output = fmt.Sprintf("[%s] %s: %s", entry.Timestamp, entry.Level, entry.Message)
		if len(fields) > 0 {
			fieldsJSON, _ := json.Marshal(fields)
			output += fmt.Sprintf(" %s", string(fieldsJSON))
		}
		output += "\n"
	}

	l.output.Write([]byte(output))
}

func (l *Logger) Debug(message string, fields map[string]interface{}) {
	l.log(DEBUG, message, fields)
}

func (l *Logger) Info(message string, fields map[string]interface{}) {
	l.log(INFO, message, fields)
}

func (l *Logger) Warn(message string, fields map[string]interface{}) {
	l.log(WARN, message, fields)
}

func (l *Logger) Error(message string, fields map[string]interface{}) {
	l.log(ERROR, message, fields)
}

func (l *Logger) Fatal(message string, fields map[string]interface{}) {
	l.log(FATAL, message, fields)
	os.Exit(1)
}

func (l *Logger) Close() error {
	if closer, ok := l.output.(io.Closer); ok {
		return closer.Close()
	}
	return nil
}
