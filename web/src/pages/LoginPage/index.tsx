import { Button, Form, Input, message, Tabs } from 'antd';
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
    const { setStatus } = useAuth();
    const from = (location.state as any)?.from?.pathname || '/trips';

    const [loadingLogin, setLoadingLogin] = useState(false);
    const [loadingReg, setLoadingReg] = useState(false);

    function onLogin(values: FieldType) {
        setLoadingLogin(true);
        const loginPayload: LoginPayload = {
            email: values.email,
            password: values.password,
        }
        login(loginPayload)
            .then((token) => {
                setStatus('authenticated');
                localStorage.setItem('token', token);
                navigate(from, { replace: true });
            })
            .catch((err: Error) => {
                console.log(err)
                alert(err.message) // temp
                message.error(err.message || 'Login failed.'); //todo
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
                    .then((token) => {
                        setStatus('authenticated');
                        localStorage.setItem('token', token);
                        navigate(from, { replace: true });
                    })
            })
            .catch((err: any) => {
                alert(err.message) // temp
                message.error(err.message || 'Register failed.');//todo
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
                                label="Name"
                                name="username"
                                rules={[{ required: true, message: 'Please enter your name' }]}
                            >
                                <Input />
                            </Form.Item>
                            <Form.Item<RegisterFormFieldType>
                                label="Email"
                                name="email"
                                rules={[{ type: 'email', required: true, message: 'Please input your email!' }]}
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