package pkg

import "github.com/gin-gonic/gin"

// Response 统一响应结构
type Response struct {
	Code    int         `json:"code"`
	Message string      `json:"message"`
	Data    interface{} `json:"data"`
}

// PageResult 分页结构
type PageResult struct {
	List  interface{} `json:"list"`
	Total int64       `json:"total"`
	Page  int         `json:"page"`
	Size  int         `json:"size"`
}

// OK 成功响应
func OK(c *gin.Context, data interface{}) {
	c.JSON(200, Response{
		Code:    CodeOK,
		Message: MsgMap[CodeOK],
		Data:    data,
	})
}

// OKPage 成功分页响应
func OKPage(c *gin.Context, list interface{}, total int64, page, size int) {
	OK(c, PageResult{
		List:  list,
		Total: total,
		Page:  page,
		Size:  size,
	})
}

// Fail 失败响应
func Fail(c *gin.Context, code int, msg string) {
	if msg == "" {
		msg = MsgMap[code]
	}
	c.JSON(200, Response{
		Code:    code,
		Message: msg,
		Data:    nil,
	})
}
