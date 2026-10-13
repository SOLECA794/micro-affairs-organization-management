package config

import (
	"os"
	"strconv"
)

// Config 全局配置
type Config struct {
	Port        string
	DBHost      string
	DBPort      string
	DBUser      string
	DBPassword  string
	DBName      string
	RedisAddr   string
	RedisPwd    string
	JWTSecret   string
	JWTExpireH  int
	LogLevel    string
}

// Load 从环境变量加载配置
func Load() *Config {
	expireH := 24
	if v := os.Getenv("JWT_EXPIRE_HOURS"); v != "" {
		if n, err := strconv.Atoi(v); err == nil {
			expireH = n
		}
	}
	return &Config{
		Port:       getEnv("PORT", "8080"),
		DBHost:     getEnv("DB_HOST", "127.0.0.1"),
		DBPort:     getEnv("DB_PORT", "3306"),
		DBUser:     getEnv("DB_USER", "root"),
		DBPassword: getEnv("DB_PASSWORD", "root"),
		DBName:     getEnv("DB_NAME", "assoc"),
		RedisAddr:  getEnv("REDIS_ADDR", "127.0.0.1:6379"),
		RedisPwd:   getEnv("REDIS_PASSWORD", ""),
		JWTSecret:  getEnv("JWT_SECRET", "dev-only-secret-change-in-prod"),
		JWTExpireH: expireH,
		LogLevel:   getEnv("LOG_LEVEL", "info"),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
