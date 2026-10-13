package middleware

import (
	"github.com/gin-gonic/gin"

	"github.com/SOLECA794/micro-affairs-organization-management/backend-go/pkg"
)

// RequireRole 角色控制中间件：当前用户角色必须在允许列表内
func RequireRole(roles ...string) gin.HandlerFunc {
	allowed := make(map[string]bool, len(roles))
	for _, r := range roles {
		allowed[r] = true
	}
	return func(c *gin.Context) {
		role, _ := c.Get(CtxRole)
		roleStr, _ := role.(string)
		if !allowed[roleStr] {
			pkg.Fail(c, pkg.CodeForbidden, "")
			c.Abort()
			return
		}
		c.Next()
	}
}

// CurrentUserID 从上下文取当前用户 ID
func CurrentUserID(c *gin.Context) int64 {
	v, _ := c.Get(CtxUserID)
	id, _ := v.(int64)
	return id
}

// CurrentRole 从上下文取当前用户角色
func CurrentRole(c *gin.Context) string {
	v, _ := c.Get(CtxRole)
	role, _ := v.(string)
	return role
}

