package middleware

import (
	"time"

	"github.com/gin-gonic/gin"
	"go.uber.org/zap"

	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/pkg"
)

// RequestLog 请求日志中间件
func RequestLog() gin.HandlerFunc {
	return func(c *gin.Context) {
		start := time.Now()
		path := c.Request.URL.Path
		method := c.Request.Method
		ip := c.ClientIP()

		c.Next()

		latency := time.Since(start)
		status := c.Writer.Status()
		pkg.Logger.Info("request",
			zap.String("method", method),
			zap.String("path", path),
			zap.Int("status", status),
			zap.String("ip", ip),
			zap.Duration("latency", latency),
		)
	}
}

