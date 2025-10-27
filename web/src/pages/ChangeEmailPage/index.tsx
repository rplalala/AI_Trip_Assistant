import { App as AntdApp, Alert, Button, Card, Flex, Form, Input, Typography } from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { requestEmailChange, verifyChangeEmailToken } from '../../api/user';

type ChangeEmailFieldType = {
  email: string;
};

export default function ChangeEmailPage() {
  const { message } = AntdApp.useApp();
  const [params] = useSearchParams();
  const token = useMemo(() => params.get('token') || '', [params]);
  const [status, setStatus] = useState<'verifying' | 'invalid' | 'valid'>('verifying');
  const [submitting, setSubmitting] = useState(false);
  const [completed, setCompleted] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    let mounted = true;
    if (!token) {
      setStatus('invalid');
      return;
    }
    (async () => {
      try {
        await verifyChangeEmailToken(token);
        if (mounted) setStatus('valid');
      } catch {
        if (mounted) setStatus('invalid');
      }
    })();
    return () => {
      mounted = false;
    };
  }, [token]);

  const onFinish = async (values: ChangeEmailFieldType) => {
    if (!token) {
      message.error('Missing token');
      return;
    }
    setSubmitting(true);
    try {
      await requestEmailChange(token, values.email);
      message.success('Email sent');
      setCompleted(true);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to request email change';
      message.error(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
      <Card style={{ width: 460 }} variant="outlined">
        <Typography.Title level={3} style={{ marginBottom: 16 }}>
          Change Email
        </Typography.Title>

        {status === 'verifying' && (
          <Alert type="info" showIcon message="Verifying link..." />
        )}

        {status === 'invalid' && (
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
        )}

        {status === 'valid' && !completed && (
          <>
            <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
              Enter your new email address. We will send a confirmation link to complete the change.
            </Typography.Paragraph>
            <Form layout="vertical" onFinish={onFinish}>
              <Form.Item<ChangeEmailFieldType>
                name="email"
                label="New email"
                rules={[
                  { required: true, message: 'Please enter your new email' },
                  { type: 'email', message: 'Please enter a valid email' },
                ]}
              >
                <Input placeholder="name@example.com" />
              </Form.Item>
              <Button type="primary" htmlType="submit" block loading={submitting}>
                Send confirmation
              </Button>
            </Form>
          </>
        )}

        {status === 'valid' && completed && (
          <>
            <Alert
              type="success"
              showIcon
              message="Email sent. Please check your inbox to confirm the change."
              style={{ marginBottom: 16 }}
            />
            <Button block type="primary" onClick={() => navigate('/profile')}>
              Back to Profile
            </Button>
          </>
        )}
      </Card>
    </Flex>
  );
}
