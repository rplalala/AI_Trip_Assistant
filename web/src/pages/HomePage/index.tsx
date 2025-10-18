import { Button } from 'antd';
import { useAuth } from '../../contexts/AuthContext';
import { useLocation, useNavigate } from 'react-router-dom';

export default function HomePage() {
    const { status } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    function handleStart() {
        if (status === 'unauthenticated') {
            navigate('/login', { state: { from: location } })
        } else if (status === 'authenticated') {
            navigate('/trips/new')
        }
    }

    return (
        <div>
            <Button onClick={handleStart}>Start</Button>
        </div>
    )
}