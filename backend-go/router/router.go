package router

import (
	"github.com/gin-gonic/gin"

	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/middleware"
	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/pkg"
)

// Setup 注册全部路由
func Setup() *gin.Engine {
	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(middleware.RequestLog())
	r.Use(middleware.CORS())

	// 健康检查
	r.GET("/api/health", func(c *gin.Context) {
		pkg.OK(c, gin.H{"status": "up"})
	})

	// 公开路由（无需登录）：登录接口、活动列表、活动详情、签到码校验等由后续模块接入

	// 需要登录的路由组
	auth := r.Group("/api")
	auth.Use(middleware.Auth())
	{
		// GET /api/auth/me 由 S3 接入
		// 其余业务路由由各模块接入
	}

	return r
}