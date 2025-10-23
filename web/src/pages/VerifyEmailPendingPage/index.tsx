import { Alert, Button, Card, Flex, Typography } from 'antd';
import { useNavigate, useSearchParams } from 'react-router-dom';

export default function VerifyEmailSentPage() {
    const [params] = useSearchParams();
    const email = params.get('email') || '';
    const navigate = useNavigate();

    return (
        <Flex vertical align="center" justify="center" style={{ minHeight: '80vh', padding: 24 }}>
            <Card style={{ width: 520 }} variant="outlined">
                <Typography.Title level={3} style={{ textAlign: 'left', marginBottom: 16 }}>
                    Verify Your Email
                </Typography.Title>
                <Alert
                    type="success"
                    showIcon
                    message={
                        <span>
                            We've sent a verification link to <b>{email}</b>. Please check your email and click the link to verify your account.
                        </span>
                    }
                    style={{ marginBottom: 16 }}
                />
                <Typography.Paragraph type="secondary" style={{ marginBottom: 16 }}>
                    Check your console logs for the verification link.
                </Typography.Paragraph>
                <Button type="primary" block onClick={() => navigate('/login')}>
                    Back to Login
                </Button>
            </Card>
        </Flex>
    );
}

