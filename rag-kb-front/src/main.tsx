import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { ConfigProvider, App as AntdApp } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import App from './App';
import './index.css';

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <ConfigProvider
        locale={zhCN}
        theme={{
          token: {
            colorPrimary: '#4f46e5',
            colorBgContainer: '#ffffff',
            colorBgElevated: '#ffffff',
            colorBorder: '#e5e7eb',
            colorBorderSecondary: '#f3f4f6',
            borderRadius: 8,
            colorText: '#1f2937',
            colorTextSecondary: '#6b7280',
            fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
          },
          components: {
            Menu: {
              itemBg: 'transparent',
              subMenuItemBg: 'transparent',
              itemSelectedBg: 'rgba(79, 70, 229, 0.08)',
              itemHoverBg: 'rgba(79, 70, 229, 0.04)',
              itemSelectedColor: '#4f46e5',
            },
            Card: {
              colorBgContainer: '#ffffff',
            },
            Table: {
              colorBgContainer: '#ffffff',
              headerBg: '#f9fafb',
            },
            Modal: {
              contentBg: '#ffffff',
              headerBg: '#ffffff',
            },
          },
        }}
      >
        <AntdApp>
          <App />
        </AntdApp>
      </ConfigProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
