import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

/**
 * 合并条件类名并处理 Tailwind CSS 工具类冲突。
 * @param inputs 类名、条件对象或嵌套类名数组。
 * @returns 去重且冲突已合并的类名字符串。
 */
export function cn(...inputs: ClassValue[]): string {
  return twMerge(clsx(inputs));
}
