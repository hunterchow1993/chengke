import { useCallback, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { useAppDispatch } from '@/app/store/hooks';
import { ApiError } from '@/lib/http/api-error';

import { endLocalSession } from './end-local-session';

/**
 * 管理退出登录二次确认与提交。仅当退出接口成功后才清缓存并进入登录页。
 * @returns 弹窗开关、提交态、错误文案，以及打开、取消、确认方法。
 */
export function useConfirmLogout() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const submittingRef = useRef(false);
  const [open, setOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const openDialog = useCallback(() => {
    setErrorMessage(null);
    setOpen(true);
  }, []);

  const closeDialog = useCallback(() => {
    if (submittingRef.current) {
      return;
    }

    setOpen(false);
    setErrorMessage(null);
  }, []);

  const confirmLogout = useCallback(async () => {
    if (submittingRef.current) {
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      await endLocalSession(dispatch);
      setOpen(false);
      void navigate('/login', { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof ApiError ? error.message : '退出失败，请稍后重试');
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }, [dispatch, navigate]);

  return {
    closeDialog,
    confirmLogout,
    errorMessage,
    isSubmitting,
    open,
    openDialog,
  };
}
