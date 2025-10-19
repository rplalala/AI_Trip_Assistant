import { Button, Form, Input, Tabs, App as AntdApp, Flex, Card, Typography } from 'antd'
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { login, register, type LoginPayload, type RegisterPayload } from '../../api/user';
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
    const from = (location.state)?.from?.pathname || '/';
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
                message.error(err.message || 'Login failed.');
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
                login(registerPayload)
                    .then(afterLogin)
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
                <Input />
            </Form.Item>

            <Form.Item<LoginFieldType>
                label="Password"
                name="password"
                rules={[{ required: true, message: 'Please input your password!' }]}
            >
                <Input.Password />
            </Form.Item>

            <Form.Item label={null}>
                <Button type="primary" htmlType="submit" loading={loadingLogin}>
                    Sign in
                </Button>
            </Form.Item>
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
                <Input />
            </Form.Item>

            <Form.Item<RegisterFormFieldType>
                label="Name"
                name="username"
                rules={[{ required: true, message: 'Please enter your name' }]}
            >
                <Input />
            </Form.Item>

            <Form.Item<RegisterFormFieldType>
                label="Password"
                name="password"
                rules={[{ required: true, message: 'Please input your password!' }]}
            >
                <Input.Password />
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
                <Input.Password />
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