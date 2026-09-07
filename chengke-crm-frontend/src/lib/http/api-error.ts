/**
 * 表示经过 HTTP 层标准化后的接口错误。
 * 调用方可通过稳定业务码和 HTTP 状态决定交互，不依赖中文消息。
 */
export class ApiError extends Error {
  /**
   * 创建标准接口错误。
   * @param code 后端返回的稳定业务错误码。
   * @param message 可展示给用户的错误消息。
   * @param status HTTP 响应状态码；断网等场景可能不存在。
   * @param requestId 用于前后端日志关联的请求标识。
   */
  constructor(
    public readonly code: string,
    message: string,
    public readonly status?: number,
    public readonly requestId?: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}
