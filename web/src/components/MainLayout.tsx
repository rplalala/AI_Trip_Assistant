import { Layout, Menu, theme, Avatar, Dropdown, Space } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { UserOutlined, LogoutOutlined, GithubOutlined, CloudServerOutlined, DeploymentUnitOutlined, RobotOutlined } from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext'
import React from 'react';

const { Header, Content, Footer } = Layout;

const items = [
    { key: '/', label: 'Home' },
    { key: '/trips', label: 'My Trips' },
];

const MainLayout = ({ children }: { children: React.ReactNode }) => {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, setStatus, setUser } = useAuth();

    const {
        token: { colorBgContainer, borderRadiusLG },
    } = theme.useToken();

    const handleLogout = () => {
        localStorage.removeItem('token');
        setUser(null);
        setStatus('unauthenticated');
        navigate('/');
    };

    const userMenu = {
        items: [
            { key: 'profile', label: 'Profile' },
            { key: 'logout', label: 'Logout', icon: <LogoutOutlined />, danger: true },
        ],
        onClick: ({ key }: { key: string }) => {
            if (key === 'logout') handleLogout();
            if (key === 'profile') navigate('/profile');
        },
    };

    const guestMenu = {
        items: [
            { key: 'login', label: 'LogIn' },
        ],
        onClick: ({ key }: { key: string }) => {
            if (key === 'login') navigate('/login');
        },
    };

    // Small segmented badge component used in the footer
    const SegBadge = ({
        leftIcon,
        leftText,
        rightText,
        rightBg,
        href,
        ariaLabel,
    }: {
        leftIcon: React.ReactNode;
        leftText: string;
        rightText: string;
        rightBg: string;
        href?: string;
        ariaLabel: string;
    }) => (
        <a
            href={href || '#'}
            target={href ? '_blank' : undefined}
            rel={href ? 'noreferrer' : undefined}
            aria-label={ariaLabel}
            title={ariaLabel}
            style={{ textDecoration: 'none' }}
        >
            <span
                style={{
                    display: 'inline-flex',
                    borderRadius: 8,
                    overflow: 'hidden',
                    boxShadow: '0 0 0 1px rgba(0,0,0,0.06) inset',
                }}
            >
                <span
                    style={{
                        background: 'rgba(0,0,0,0.06)',
                        color: 'rgba(0,0,0,0.75)',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 6,
                        padding: '4px 10px',
                        fontSize: 12,
                        lineHeight: 1,
                        fontWeight: 500,
                    }}
                >
                    {leftIcon}
                    <span>{leftText}</span>
                </span>
                <span
                    style={{
                        background: rightBg,
                        color: '#ffffff',
                        padding: '4px 10px',
                        fontWeight: 700,
                        fontSize: 12,
                        lineHeight: 1,
                    }}
                >
                    {rightText}
                </span>
            </span>
        </a>
    );

    return (
        <Layout>
            <Header style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0 24px',
            }}>
                <Menu
                    theme="dark"
                    mode="horizontal"
                    selectedKeys={[location.pathname]}
                    items={items}
                    style={{ flex: 1, minWidth: 0 }}
                    onClick={(e) => navigate(e.key)}
                />

                <Dropdown menu={user ? userMenu : guestMenu} placement="bottomRight" arrow>
                    <Space style={{ cursor: 'pointer', color: 'white' }}>
                        <Avatar
                            src={user?.avatar}
                            icon={!user?.avatar && <UserOutlined />}
                            size="large"
                        />
                        <span>{user?.username ?? 'Guest'}</span>
                    </Space>
                </Dropdown>
            </Header>

            <Content style={{ padding: '0 48px' }}>
                <div
                    style={{
                        background: colorBgContainer,
                        minHeight: 280,
                        padding: 24,
                        borderRadius: borderRadiusLG,
                    }}
                >
                    {children}
                </div>
            </Content>

            <Footer style={{ textAlign: 'center' }}>
                <div style={{ display: 'flex', justifyContent: 'center', gap: 12, flexWrap: 'wrap' }}>
                    <SegBadge
                        leftIcon={<GithubOutlined />}
                        leftText="Source"
                        rightText="GitHub"
                        rightBg="#722ED1"
                        ariaLabel="Source code on GitHub"
                        href="https://github.com/rplalala/AI_Trip_Assistant"
                    />
                    <SegBadge
                        leftIcon={<CloudServerOutlined />}
                        leftText="CDN"
                        rightText="Aliyun"
                        rightBg="#D46B08"
                        ariaLabel="CDN on Aliyun"
                        href="https://cn.aliyun.com/product/cdn"
                    />
                    <SegBadge
                        leftIcon={<DeploymentUnitOutlined />}
                        leftText="Hosted"
                        rightText="AWS EC2"
                        rightBg="#52C41A"
                        ariaLabel="Hosted on AWS EC2"
                        href="https://aws.amazon.com/ec2"
                    />
                    <SegBadge
                        leftIcon={<RobotOutlined />}
                        leftText="LLM"
                        rightText="Gpt-4o-mini"
                        rightBg="#2F54EB"
                        ariaLabel="LLM model Gpt-4o-mini"
                        href="https://platform.openai.com/docs/models/gpt-4o-mini"
                    />
                </div>
            </Footer>
        </Layout>
    );
};

export default MainLayout;