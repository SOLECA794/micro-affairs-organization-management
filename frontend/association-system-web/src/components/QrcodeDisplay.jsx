import { QRCodeSVG } from 'qrcode.react';
import { Typography } from 'antd';

/**
 * 二维码展示（02 文档 §5.5）：将后端返回的 qrcodeUrl 编码为二维码。
 */
export default function QrcodeDisplay({ url, size = 200 }) {
  if (!url) return null;
  return (
    <div style={{ textAlign: 'center' }}>
      <div style={{ display: 'inline-block', padding: 12, background: '#fff', border: '1px solid #eee' }}>
        <QRCodeSVG value={url} size={size} level="M" />
      </div>
      <Typography.Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, wordBreak: 'break-all' }} copyable>
        {url}
      </Typography.Paragraph>
    </div>
  );
}
