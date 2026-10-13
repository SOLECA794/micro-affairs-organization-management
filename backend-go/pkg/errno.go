package pkg

// 错误码常量，与 04 文档错误码表对齐
const (
	CodeOK          = 0
	CodeParamError  = 40001 // 参数校验错误（40001-40099）
	CodeUnauthorized = 40100 // 未登录或 Token 失效
	CodeForbidden   = 40300 // 无权限（角色越权）
	CodeNotFound    = 40400 // 资源不存在
	CodeConflict    = 40901 // 重复操作
	CodeFull        = 40902 // 名额已满
	CodeStateError  = 40903 // 状态不允许
	CodeServerError = 50000 // 系统异常
)

// 常用错误消息
var MsgMap = map[int]string{
	CodeOK:          "ok",
	CodeParamError:  "参数错误",
	CodeUnauthorized: "未登录或登录已失效",
	CodeForbidden:   "无权限访问",
	CodeNotFound:    "资源不存在",
	CodeConflict:    "操作重复",
	CodeFull:        "名额已满",
	CodeStateError:   "当前状态不允许该操作",
	CodeServerError: "系统异常",
}
