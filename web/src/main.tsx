import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App as AntdApp } from 'antd';
import './index.css'
import '@ant-design/v5-patch-for-react-19';
import 'antd/dist/reset.css';
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
      <AntdApp>
          <App />
      </AntdApp>
  </StrictMode>,
)
