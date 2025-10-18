import { Layout, Menu, theme, Avatar, Dropdown, Space } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';
import { UserOutlined, LogoutOutlined } from '@ant-design/icons';
import { useAuth } from '../contexts/AuthContext'

const { Header, Content, Footer } = Layout;

const items = [
    { key: '/', label: 'Home' },
    { key: '/trips', label: 'My Trips' },
];

const MainLayout = ({ children }: any) => {
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
                ELEC5620 ©2025 Created by Group 61
            </Footer>
        </Layout>
    );
};

export default MainLayout;