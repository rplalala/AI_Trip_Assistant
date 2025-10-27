import { Alert, App as AntdApp, Button, Card, Flex, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { confirmEmailChange } from '../../api/user';

export default function ConfirmChangeEmailPage() {
  const { message } = AntdApp.useApp();
  const [params] = useSearchParams();
  const token = useMemo(() => params.get('token') || '', [params]);
  const [status, setStatus] = useState<'verifying' | 'success' | 'fail'>('verifying');
  const navigate = useNavigate();

  useEffect(() => {
    let mounted = true;
    if (!token) {
      setStatus('fail');
      return;
    }
    (async () => {
      try {
        await confirmEmailChange(token);
        if (!mounted) return;
        setStatus('success');
        message.success('Email changed successfully!');
        setTimeout(() => window.location.assign('/profile'), 1200);
      } catch (err: unknown) {
        if (!mounted) return;
        const msg = err instanceof Error ? err.message : 'Failed to confirm email change';
        message.error(msg);
        setStatus('fail');
      }
    })();
    return () => {
      mounted = false;
    };
  }, [token, message, navigate]);

  let content: React.ReactNode;
  if (status === 'verifying') {
    content = <Alert type="info" showIcon message="Verifying link..." />;
  } else if (status === 'success') {
    content = <Alert type="success" showIcon message="Email changed successfully!" />;
  } else {
    content = (
      <>
        <Alert
          type="error"
          showIcon
          message="Change failed. The link may be invalid or expired."
          style={{ marginBottom: 16 }}
        />
        <Button block type="primary" onClick={() => navigate('/profile')}>
          Back to Profile
        </Button>
      </>
    );
  }

  return (
    <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
      <Card style={{ width: 460 }} variant="outlined">
        <Typography.Title level={3} style={{ marginBottom: 16 }}>
          Confirm Email Change
        </Typography.Title>
        {content}
      </Card>
    </Flex>
  );
}
