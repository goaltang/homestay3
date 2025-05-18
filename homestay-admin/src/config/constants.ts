// API基础URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "";

// 文件上传路径
export const UPLOAD_URL = `${API_BASE_URL}/api/files/upload`;

// 默认页大小
export const DEFAULT_PAGE_SIZE = 10;

// 默认分页选项
export const PAGE_SIZE_OPTIONS = [5, 10, 20, 50, 100];

// 图片占位符
export const IMAGE_PLACEHOLDER = "https://via.placeholder.com/150";

// 日期格式
export const DATE_FORMAT = "YYYY-MM-DD";
export const DATE_TIME_FORMAT = "YYYY-MM-DD HH:mm:ss";
