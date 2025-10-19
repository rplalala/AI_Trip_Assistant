import { Button, Form, Input, Tabs, App as AntdApp } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { login, register, type LoginPayload, type RegisterPayload } from '../../api/user';
import { useState } from 'react'

type FieldType = {
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

    function onLogin(values: FieldType) {
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

    return (
        <Tabs
            defaultActiveKey="login"
            items={[
                {
                    key: 'login',
                    label: 'Login',
                    children: (
                        <Form
                            name="login"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 16 }}
                            style={{ maxWidth: 600 }}
                            onFinish={onLogin}
                            autoComplete="off"
                        >
                            <Form.Item<FieldType>
                                label="Email"
                                name="email"
                                rules={[{ type: 'email', required: true, message: 'Please input your email!' }]}
                            >
                                <Input />
                            </Form.Item>

                            <Form.Item<FieldType>
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
                    )
                },
                {
                    key: 'register',
                    label: 'Register',
                    children: (
                        <Form
                            name="register"
                            labelCol={{ span: 8 }}
                            wrapperCol={{ span: 16 }}
                            style={{ maxWidth: 600 }}
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
                    )
                }
            ]}
        />
    )
}