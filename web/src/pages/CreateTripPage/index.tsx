import {
    Button,
    Form,
    Input,
    App as AntdApp,
    Card,
    Typography,
    InputNumber,
    Divider,
    Space, DatePicker,
    Select
} from 'antd'
import { LocationAutoComplete, type Location } from '../../components/LocationAutoComplete'
import { generateTrip, type generateTripPayload } from '../../api/trip';
import dayjs from 'dayjs';
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'

const { RangePicker } = DatePicker;

type CreateTripFieldType = {
    destination: Location | null;
    dates: [dayjs.Dayjs, dayjs.Dayjs];
    travelers: number;
    budget?: number;
    departure?: Location | null;
    preferences?: string;
    currency: string;
};

const CURRENCY_OPTIONS: { label: string; value: string }[] = [
    { label: 'US Dollar (USD)', value: 'USD' },
    { label: 'Euro (EUR)', value: 'EUR' },
    { label: 'British Pound (GBP)', value: 'GBP' },
    { label: 'Australian Dollar (AUD)', value: 'AUD' },
    { label: 'Canadian Dollar (CAD)', value: 'CAD' },
    { label: 'Chinese Yuan (CNY)', value: 'CNY' },
    { label: 'Japanese Yen (JPY)', value: 'JPY' },
    { label: 'Korean Won (KRW)', value: 'KRW' },
    { label: 'Hong Kong Dollar (HKD)', value: 'HKD' },
    { label: 'Singapore Dollar (SGD)', value: 'SGD' },
    { label: 'Indian Rupee (INR)', value: 'INR' },
    { label: 'New Zealand Dollar (NZD)', value: 'NZD' },
    { label: 'Swiss Franc (CHF)', value: 'CHF' },
    { label: 'Swedish Krona (SEK)', value: 'SEK' },
    { label: 'Norwegian Krone (NOK)', value: 'NOK' },
    { label: 'Danish Krone (DKK)', value: 'DKK' },
    { label: 'Brazilian Real (BRL)', value: 'BRL' },
    { label: 'Mexican Peso (MXN)', value: 'MXN' },
    { label: 'South African Rand (ZAR)', value: 'ZAR' },
    { label: 'UAE Dirham (AED)', value: 'AED' },
    { label: 'Saudi Riyal (SAR)', value: 'SAR' },
    { label: 'Turkish Lira (TRY)', value: 'TRY' },
    { label: 'Thai Baht (THB)', value: 'THB' },
    { label: 'Indonesian Rupiah (IDR)', value: 'IDR' },
    { label: 'Malaysian Ringgit (MYR)', value: 'MYR' },
    { label: 'Philippine Peso (PHP)', value: 'PHP' },
    { label: 'Vietnamese Dong (VND)', value: 'VND' },
    { label: 'New Taiwan Dollar (TWD)', value: 'TWD' },
];

export default function CreateTripPage() {
    const { message } = AntdApp.useApp();
    const [form] = Form.useForm<CreateTripFieldType>();
    const [ submitLoading, setSubmitLoading ] = useState(false)
    const navigate = useNavigate();

    const onFinish = async (values: CreateTripFieldType) => {
        setSubmitLoading(true)
        if (!values.destination) {
            message.error('Please choose a destination');
            setSubmitLoading(false);
            return;
        }
        const payload: generateTripPayload = {
            // destination: values.destination,
            startDate: values.dates?.[0]?.format('YYYY-MM-DD'),
            endDate: values.dates?.[1]?.format('YYYY-MM-DD'),
            people: values.travelers,
            budget: values.budget || 0,
            currency: values.currency,
            fromCity: values.departure?.name || '',
            fromCountry: values.departure?.country?.name || '',
            toCity: values.destination?.name || '',
            toCountry: values.destination?.country?.name || '',
            preferences: values.preferences || '',
        };
        console.log('Submit payload:', payload);

        generateTrip(payload)
            .then(() => {
                message.success('Trip generation started');
                navigate('/trips', { replace: true });
            })
            .catch((err) => {
                message.error(err?.message || 'Failed to start trip generation');
            })
            .finally(() => {
                setSubmitLoading(false)
            });
    };

    return (
        <Card style={{ maxWidth: 720, margin: '0 auto' }}>
            <Typography.Title level={3} style={{ marginBottom: 24 }}>
                Create A New Trip
            </Typography.Title>

            <Form<CreateTripFieldType> form={form} layout="vertical" onFinish={onFinish}>
                <Form.Item<CreateTripFieldType>
                    label="Destination"
                    name="destination"
                    rules={[{ required: true, message: 'Please select a destination' }]}
                    valuePropName="value"
                    getValueFromEvent={(val: Location | null) => val}
                >
                    <LocationAutoComplete placeholder="Search city or country..." />
                </Form.Item>

                <Form.Item<CreateTripFieldType>
                    label="Departure city"
                    name="departure"
                    rules={[{ required: true, message: 'Please choose your departure city' }]}
                    valuePropName="value"
                    getValueFromEvent={(val: Location | null) => val}
                >
                    <LocationAutoComplete placeholder="Search your departure city..." />
                </Form.Item>

                <Form.Item<CreateTripFieldType>
                    label="Dates"
                    name="dates"
                    rules={[{ required: true, message: 'Please select your dates' }]}
                >
                    <RangePicker style={{ width: '100%' }} />
                </Form.Item>

                <Form.Item<CreateTripFieldType>
                    label="Number of Travelers"
                    name="travelers"
                    rules={[{ required: true, message: 'Please enter number of travelers' }]}
                >
                    <InputNumber min={1} precision={0} style={{ width: '100%' }} />
                </Form.Item>

                <Form.Item<CreateTripFieldType>
                    label="Currency"
                    name="currency"
                    rules={[{ required: true, message: 'Please select a currency' }]}
                >
                    <Select
                        showSearch
                        placeholder="Select a currency"
                        options={CURRENCY_OPTIONS}
                        optionFilterProp="label"
                    />
                </Form.Item>

                <Form.Item<CreateTripFieldType>
                    label="Budget"
                    name="budget"
                    rules={[{ required: true, message: 'Please enter your budget' }]}
                >
                    <InputNumber
                        min={0}
                        step={100}
                        style={{ width: '100%' }}
                        formatter={(v) => (v == null ? '' : `$ ${v}`)}
                        // parser={(v) => (v ? Number(v.replace(/\$\s?|(,*)/g, '')) : 0)}
                    />
                </Form.Item>

                <Divider />

                <Form.Item<CreateTripFieldType> label="Preferences" name="preferences">
                    <Input.TextArea
                        rows={4}
                        placeholder="e.g., Food: Vegan; Pace: Moderate; Accessibility: Wheelchair friendly paths"
                    />
                </Form.Item>

                <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
                    <Button htmlType="reset">Reset</Button>
                    <Button type="primary" htmlType="submit" loading={submitLoading}>
                        Generate Plan
                    </Button>
                </Space>
            </Form>
        </Card>
    )
}