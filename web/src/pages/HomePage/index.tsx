import { Button, Typography } from 'antd';
import { useAuth } from '../../contexts/AuthContext';
import { useLocation, useNavigate } from 'react-router-dom';
import React from 'react';

const heroBg =
  "linear-gradient(rgba(0, 0, 0, 0.1) 0%, rgba(0, 0, 0, 0.4) 100%), url('https://awscdn.dingzh.cc/elec5620-stage2/2025/10/c5bdc3ca7ef042e9926d06dfa52d5d92.png')";

const features = [
  {
    img: "https://lh3.googleusercontent.com/aida-public/AB6AXuDeqJdt7iTW8DthSHGh0UDw1gO-0sJKrEsvgJHZiI6YUeAHo9uwO5TeLfPWr25BkcZp7CEYzgquCXWE1kk3bPU04Cb0lMQxc87lS5q-hUKPKoiuaPc3OCfVHxy_qNrkp9mn7GPlaqpJZbBiBSMqW7drVqw94sH9psdxh3YUSNTn9kZ_0yoYOa-AKUYpMP_Fktd2BDmB7MN5Vdz4kmXLSbavd_VG_eEMRrZD5TYUszLlgsIzpP28XPj1XLPtAJFnS2DmLny4A7X8ouo",
    title: 'Smart Planning',
    desc: 'Create detailed itineraries with integrated maps, points of interest, and personalized recommendations.'
  },
  {
    img: "https://lh3.googleusercontent.com/aida-public/AB6AXuAgnvGgHS6vYsBYuQcYH3fqEFNJghpVC_ysYVdMBNcFJniii87SCn0Jf6FnyYERt6JGJiSwKYsAsSVwWcX1aIsUX43B4v7l_X8jP4wJ3GpJq1sHP-e425-6I_K6h6ssLGDsKkRpPAWVXK1n_UXzMsTGwVLn88c620c-KPq8xB3AaZle-BEZr0pAEJz--zP9doL9M8vm_qekixwAYjWsqydj9aQwyUHv5wqOb4blP-96Ox9pl-EvL2sZkNAyHLl-rloJMUmbcFNgZ_I",
    title: 'Live Weather',
    desc: "Stay ahead with real-time weather updates for your destinations, ensuring you're always prepared.",
  },
  {
    img: "https://lh3.googleusercontent.com/aida-public/AB6AXuCIlT-nxwSgTvSSx6ZqnDcdDq8pgXxGgFAvZ-MfEJbikW5CES0_S-VcknUbMRxBDQKMMaSjOpYADct5YliTrYIdQVJCYviuUkM45TOOdzMuQecdsiGj-PW0zr1woTzXtwYNhGR6VZxpPm0PxoFkqxub6mpXimvH92UsnQphFtqB0bgW8OfDyNjft7B9Ptqwn9XqtMiGuot5pW7ScQieBWosydAK46rEQPK7Iqxfl4LUwL3LLj2o-SdVsPv1ECBIBV2OfAzFBftOMZo",
    title: 'One-click Booking',
    desc: 'Secure flights, accommodations, and activities with a single click, using our trusted partner network.',
  },
  {
    img: "https://lh3.googleusercontent.com/aida-public/AB6AXuBgpC-zM0FtZTPUEJBbiz0gt0t0EiM9cSZqneQ_7JYL3On1WlEe85Bmi9fQhauQJyynKIc7v2I0zFL9HnG3jUmY7J7DU-sM6eElQnTr3T_h1TmM4rj4Vmy-VIH8yI1ab57fa0hzpxHdbkKn438fdPL6l9JbHnIs079EVGLUTQMlI0zuwiKO7duhf0TzKDRwigxBGrnWUgwmHt3_9J7i8JyoXbq5UR9wCUWL8rcFxWbPCgzaoL9FgSCmrQ1d3AEBLW4dIRRLKdvEWw4",
    title: 'Auto Re-plan',
    desc: 'Automatically adjust your plans based on unforeseen events, ensuring a smooth and stress-free journey.',
  },
];

export default function HomePage(): React.ReactElement {
  const { status } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  function handleStart() {
    if (status === 'unauthenticated') {
      navigate('/login', { state: { from: location } });
    } else if (status === 'authenticated') {
      navigate('/trips');
    }
  }

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Container */}
      <div style={{ flex: 1, display: 'flex', justifyContent: 'center', padding: '20px 24px' }}>
        <div style={{ width: '100%', maxWidth: 1280 }}>
          {/* Hero */}
          <div style={{ padding: 16 }}>
            <div
              style={{
                minHeight: 480,
                display: 'flex',
                flexDirection: 'column',
                gap: 16,
                alignItems: 'center',
                justifyContent: 'center',
                backgroundImage: heroBg,
                backgroundSize: 'cover',
                backgroundPosition: 'center',
                borderRadius: 12,
                padding: 16,
              }}
            >
              <div style={{ textAlign: 'center', display: 'flex', flexDirection: 'column', gap: 8 }}>
                <Typography.Title level={1} style={{ color: '#fff', margin: 0, fontWeight: 900 }}>
                  Plan once. Adapt always.
                </Typography.Title>
                <Typography.Text style={{ color: '#fff', fontSize: 16 }}>
                  Your all-in-one travel companion for seamless trip planning and real-time adjustments.
                </Typography.Text>
              </div>
              <Button
                size="large"
                style={{
                  height: 48,
                  borderRadius: 12,
                  background: '#13ecc8',
                  color: '#111817',
                  fontWeight: 700,
                }}
                onClick={handleStart}
              >
                Try Free
              </Button>
            </div>
          </div>

          {/* Features section */}
          <div style={{ padding: '16px', paddingBottom: 40 }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
              <Typography.Title level={2} style={{ margin: 0, maxWidth: 720 }}>
                Your travel, simplified
              </Typography.Title>
              <Typography.Paragraph style={{ maxWidth: 720, margin: 0 }}>
                Explore the features that make planning and adapting your trips effortless.
              </Typography.Paragraph>
            </div>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(158px, 1fr))',
                gap: 12,
                marginTop: 16,
              }}
            >
              {features.map((f, idx) => (
                <div key={idx} style={{ display: 'flex', flexDirection: 'column', gap: 12, paddingBottom: 12 }}>
                  <div
                    style={{
                      width: '100%',
                      aspectRatio: '16 / 9',
                      backgroundImage: `url('${f.img}')`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                      borderRadius: 12,
                    }}
                  />
                  <div>
                    <Typography.Text strong style={{ display: 'block', fontSize: 16 }}>{f.title}</Typography.Text>
                    <Typography.Text type="secondary" style={{ fontSize: 13 }}>{f.desc}</Typography.Text>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Footer */}
          <footer style={{ padding: '24px 20px', textAlign: 'center' }}>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 24, justifyContent: 'center', marginBottom: 16 }}>
              <a href="#" style={{ color: '#618983' }}>About</a>
              <a href="#" style={{ color: '#618983' }}>Contact</a>
              <a href="#" style={{ color: '#618983' }}>Terms of Service</a>
              <a href="#" style={{ color: '#618983' }}>Privacy Policy</a>
            </div>
          </footer>
        </div>
      </div>
    </div>
  );
}