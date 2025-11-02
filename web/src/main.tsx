// import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App as AntdApp, ConfigProvider } from 'antd';
import './index.css'
import '@ant-design/v5-patch-for-react-19';
import 'antd/dist/reset.css';
import App from './App.tsx'

const themePrimary = '#00C2B2';

createRoot(document.getElementById('root')!).render(
  // <StrictMode>
    <ConfigProvider
        theme={{
            token: {
                colorPrimary: themePrimary,
                colorLink: themePrimary,
            },
        }}
    >
        <AntdApp>
            <App />
        </AntdApp>
    </ConfigProvider>
  // </StrictMode>,
)
