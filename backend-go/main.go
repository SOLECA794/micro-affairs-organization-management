package main

import (
	"context"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"go.uber.org/zap"

	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/config"
	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/pkg"
	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/router"
)

func main() {
	cfg := config.Load()

	// 初始化日志
	pkg.InitLogger(cfg.LogLevel)
	defer pkg.Logger.Sync()

	// 初始化 JWT
	pkg.InitJWT(cfg.JWTSecret)

	// 路由
	r := router.Setup()

	srv := &http.Server{
		Addr:    ":" + cfg.Port,
		Handler: r,
	}

	// 优雅启动
	go func() {
		pkg.Logger.Info("server starting", zap.String("port", cfg.Port))
		if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			pkg.Logger.Fatal("listen failed", zap.Error(err))
		}
	}()

	// 优雅关闭
	quit := make(chan os.Signal, 1)
	signal.Notify(quit, syscall.SIGINT, syscall.SIGTERM)
	<-quit
	pkg.Logger.Info("server shutting down...")

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := srv.Shutdown(ctx); err != nil {
		pkg.Logger.Fatal("server forced to shutdown", zap.Error(err))
	}
	pkg.Logger.Info("server exited")
}