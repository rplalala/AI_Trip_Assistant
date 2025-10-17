import { Layout, Menu, theme } from 'antd';
import { useNavigate, useLocation } from 'react-router-dom';

const { Header, Content, Footer } = Layout;

const items = [
    { key: '/', label: 'Home' },
    { key: '/trips', label: 'My Trips' },
];

const MainLayout = ({ children }: any) => {
    const navigate = useNavigate();
    const location = useLocation();

    const {
        token: { colorBgContainer, borderRadiusLG },
    } = theme.useToken();

    return (
        <Layout>
            <Header style={{ display: 'flex', alignItems: 'center' }}>
                <div className="demo-logo" />
                <Menu
                    theme="dark"
                    mode="horizontal"
                    selectedKeys={[location.pathname]}
                    items={items}
                    style={{ flex: 1, minWidth: 0 }}
                    onClick={(e) => navigate(e.key)}
                />
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