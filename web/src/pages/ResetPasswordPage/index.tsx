import { App as AntdApp, Button, Card, Flex, Form, Input, Typography } from 'antd';
import { useMemo, useState } from 'react';
import { resetPassword } from '../../api/user';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function ResetPasswordPage() {
    const { message } = AntdApp.useApp();
    const [loading, setLoading] = useState(false);
    const [params] = useSearchParams();
    const navigate = useNavigate();

    const token = useMemo(() => params.get('token') || '', [params]);

    const onFinish = async (values: { password: string; confirm: string }) => {
        if (!token) {
            message.error('Missing token.');
            return;
        }
        setLoading(true);
        try {
            await resetPassword(token, values.password);
            message.success('Password reset successfully! Please log in.');
            setTimeout(() => navigate('/login'), 1200);
        } catch (err: unknown) {
            const msg = err instanceof Error ? err.message : 'Failed to reset password.';
            message.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
            <Card style={{ width: 480 }} variant="outlined">
                <Typography.Title level={3} style={{ textAlign: 'left', marginBottom: 16 }}>
                    Reset Password
                </Typography.Title>
                <Form layout="vertical" onFinish={onFinish}>
                    <Form.Item name="password" label="New Password" rules={[{ required: true, min: 6 }]}
                    >
                        <Input.Password placeholder="Enter new password" />
                    </Form.Item>
                    <Form.Item
                        name="confirm"
                        label="Confirm Password"
                        dependencies={["password"]}
                        rules={[
                            { required: true },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) return Promise.resolve();
                                    return Promise.reject(new Error('The new password that you entered do not match!'));
                                },
                            }),
                        ]}
                    >
                        <Input.Password placeholder="Re-enter new password" />
                    </Form.Item>
                    <Form.Item>
                        <Button type="primary" htmlType="submit" block loading={loading}>
                            Reset Password
                        </Button>
                    </Form.Item>
                </Form>
            </Card>
        </Flex>
    );
}
