package middleware

import (
	"strings"

	"github.com/gin-gonic/gin"

	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/pkg"
)

// UserRepo 用户查询接口（由 repository 层实现）
type UserRepo interface {
	// IsUserActive 根据 userID 查用户状态：是否存在、是否启用、是否未删除
	IsUserActive(userID int64) (bool, error)
}

var userRepo UserRepo

// SetUserRepo 注入用户查询实现（S3 模块接入时调用）
func SetUserRepo(r UserRepo) {
	userRepo = r
}

const CtxUserID = "ctx_user_id"
const CtxUsername = "ctx_username"
const CtxRole = "ctx_role"

// Auth JWT 认证中间件：解析 token + 实时查库校验用户状态
func Auth() gin.HandlerFunc {
	return func(c *gin.Context) {
		authHeader := c.GetHeader("Authorization")
		if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
			pkg.Fail(c, pkg.CodeUnauthorized, "")
			c.Abort()
			return
		}
		tokenStr := strings.TrimPrefix(authHeader, "Bearer ")
		claims, err := pkg.ParseToken(tokenStr)
		if err != nil {
			pkg.Fail(c, pkg.CodeUnauthorized, "token 无效或已过期")
			c.Abort()
			return
		}

		// 实时查库校验用户状态（保留 Java JwtInterceptor L75 行为）
		if userRepo != nil {
			active, err := userRepo.IsUserActive(claims.UserID)
			if err != nil || !active {
				pkg.Fail(c, pkg.CodeUnauthorized, "账号已被禁用或删除")
				c.Abort()
				return
			}
		}

		c.Set(CtxUserID, claims.UserID)
		c.Set(CtxUsername, claims.Username)
		c.Set(CtxRole, claims.Role)
		c.Next()
	}
}

