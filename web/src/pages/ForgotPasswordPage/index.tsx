import { App as AntdApp, Button, Card, Flex, Form, Input, Typography, Alert } from 'antd';
import { useState } from 'react';
import { forgotPassword } from '../../api/user';
import { useNavigate } from 'react-router-dom';

export default function ForgotPasswordPage() {
    const [form] = Form.useForm();
    const { message } = AntdApp.useApp();
    const [loading, setLoading] = useState(false);
    const [sentTo, setSentTo] = useState<string | null>(null);
    const navigate = useNavigate();

    const onFinish = async (values: { email: string }) => {
        setLoading(true);
        try {
            await forgotPassword(values.email);
            setSentTo(values.email);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Failed to send reset link.';
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
            <Card style={{ width: 480 }} variant="outlined">
                {!sentTo ? (
                    <>
                        <Typography.Title level={3} style={{ textAlign: 'left', marginBottom: 8 }}>
                            Forgot Password
                        </Typography.Title>
                        <Typography.Paragraph type="secondary" style={{ marginBottom: 24 }}>
                            Enter your email and we'll send you a link to reset your password.
                        </Typography.Paragraph>
                        <Form form={form} layout="vertical" onFinish={onFinish}>
                            <Form.Item name="email" label="Email" rules={[{ required: true, type: 'email' }]}>
                                <Input placeholder="your@email.com" />
                            </Form.Item>
                            <Form.Item>
                                <Button type="primary" htmlType="submit" block loading={loading}>
                                    Send Reset Link
                                </Button>
                            </Form.Item>
                        </Form>
                    </>
                ) : (
                    <>
                        <Typography.Title level={3} style={{ textAlign: 'left', marginBottom: 16 }}>
                            Check Your Email
                        </Typography.Title>
                        <Alert
                            type="success"
                            showIcon
                            message={`We've sent a password reset link to ${sentTo}. Please check your email.`}
                            style={{ marginBottom: 24 }}
                        />
                        <Button type="primary" block onClick={() => navigate('/login')}>
                            Back to Login
                        </Button>
                    </>
                )}
            </Card>
        </Flex>
    );
}
