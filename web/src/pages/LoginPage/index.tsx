import { Button, Form, Input, Tabs, App as AntdApp, Flex, Card, Typography, Modal } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { login, register, resendVerifyEmail, type LoginPayload, type RegisterPayload } from '../../api/user';
import { useState } from 'react'
import './index.css'

type LoginFieldType = {
    email: string;
    password: string;
};

type RegisterFormFieldType = {
    username: string;
    email: string;
    password: string;
};

export default function LoginPage() {
    const navigate = useNavigate();
    const location = useLocation();
    const { setStatus, refreshProfile } = useAuth();
    const from = (location.state)?.from?.pathname || '/trips';
    const { message } = AntdApp.useApp();

    const [loadingLogin, setLoadingLogin] = useState(false);
    const [loadingReg, setLoadingReg] = useState(false);

    function afterLogin(token: string) {
        setStatus('authenticated');
        localStorage.setItem('token', token);
        refreshProfile()
            .then(() => {
                navigate(from, { replace: true });
            })
    }

    function onLogin(values: LoginFieldType) {
        setLoadingLogin(true);
        const loginPayload: LoginPayload = {
            email: values.email,
            password: values.password,
        }
        login(loginPayload)
            .then(afterLogin)
            .catch((err: Error) => {
                console.log(err)
                if (err.message.includes('Email not verified')) {
                    Modal.confirm({
                        title: 'Email not verified',
                        content: 'Your email has not been verified. Please click "Resend" to resend the verification email.',
                        okText: 'Resend',
                        cancelText: 'Cancel',
                        centered: true,
                        maskClosable: false,
                        keyboard: false,
                        onOk: async () => {
                            try{
                                await resendVerifyEmail(values.email);
                                message.success('Verification email resent successfully! Please check your email.');
                                navigate('/verify-email-pending?email=' + encodeURIComponent(values.email));
                            } catch (e: any) {
                                message.error(e.message || 'Failed to resend verification email.');
                            }
                        },
                    });
                } else {
                    message.error(err.message || 'Login failed.');
                }
            })
            .finally(() => {
                setLoadingLogin(false);
            })
    }


    function onRegister(values: RegisterFormFieldType) {
        setLoadingReg(true);
        const registerPayload: RegisterPayload = {
            username: values.username,
            email: values.email,
            password: values.password,
        }

        register(registerPayload)
            .then(() => {
                navigate('/verify-email-pending?email=' + encodeURIComponent(values.email));
            })
            .catch((err: any) => {
                message.error(err.message || 'Register failed.');
            })
            .finally(() => {
                setLoadingReg(false);
            })
    }

    const loginForm = (
        <Form
            name="login"
            labelCol={{ span: 10 }}
            wrapperCol={{ span: 14 }}
            style={{ maxWidth: 600 }}
            onFinish={onLogin}
            autoComplete="off"
        >
            <Form.Item<LoginFieldType>
                label="Email"
                name="email"
                rules={[{ type: 'email', required: true, message: 'Please input your email!' }]}
            >
                <Input autoComplete="email"/>
            </Form.Item>

            <Form.Item<LoginFieldType>
                label="Password"
                name="password"
                rules={[{ required: true, message: 'Please input your password!' }]}
            >
                <Input.Password autoComplete="current-password" />
            </Form.Item>

            <Form.Item label={null}>
                <Button type="primary" htmlType="submit" loading={loadingLogin}>
                    Log in
                </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
                <Button type="link" onClick={() => navigate('/forgot-password')}>
                    Forgot password?
                </Button>
            </div>
        </Form>
    );

    const registerForm = (
        <Form
            name="register"
            labelCol={{ span: 10 }}
            wrapperCol={{ span: 14 }}
            style={{ maxWidth: 800 }}
            onFinish={onRegister}
            autoComplete="off"
        >
            <Form.Item<RegisterFormFieldType>
                label="Email"
                name="email"
                rules={[{ type: 'email', required: true, message: 'Please input your email!' }]}
            >
                <Input autoComplete="email" />
            </Form.Item>

            <Form.Item<RegisterFormFieldType>
                label="Name"
                name="username"
                rules={[{ required: true, message: 'Please enter your name' }]}
            >
                <Input autoComplete="off" />
            </Form.Item>

            <Form.Item<RegisterFormFieldType>
                label="Password"
                name="password"
                rules={[{ required: true, message: 'Please input your password!' }]}
            >
                <Input.Password autoComplete="new-password" />
            </Form.Item>

            <Form.Item
                name="confirm"
                label="Confirm Password"
                dependencies={['password']}
                hasFeedback
                rules={[
                    {
                        required: true,
                        message: 'Please confirm your password!',
                    },
                    ({ getFieldValue }) => ({
                        validator(_, value) {
                            if (!value || getFieldValue('password') === value) {
                                return Promise.resolve();
                            }
                            return Promise.reject(new Error('The new password that you entered do not match!'));
                        },
                    }),
                ]}
            >
                <Input.Password autoComplete="new-password" />
            </Form.Item>

            <Form.Item label={null}>
                <Button type="primary" htmlType="submit" loading={loadingReg}>
                    Create account
                </Button>
            </Form.Item>
        </Form>
    );

    return (
        <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
            <Card style={{ width: 420 }} variant="outlined">
                <Typography.Title level={3} style={{ textAlign: 'center', marginBottom: 8 }}>
                    Welcome to Wanderlust
                </Typography.Title>
                <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 24 }}>
                    Sign in or create an account to continue
                </Typography.Paragraph>

                <Tabs
                    defaultActiveKey="login"
                    className="login-tabs"
                    tabBarStyle={{
                        display: 'flex',
                        justifyContent: 'center',
                    }}
                    items={[
                        {
                            key: 'login',
                            label: 'Login',
                            children: loginForm
                        },
                        {
                            key: 'register',
                            label: 'Register',
                            children: registerForm
                        }
                    ]}
                />
            </Card>
        </Flex>
    )
}