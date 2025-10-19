import {
    Button,
    Form,
    Input,
    App as AntdApp,
    Card,
    Typography,
    InputNumber,
    Divider,
    Space, DatePicker
} from 'antd'
import { LocationAutoComplete, type Location } from '../../components/LocationAutoComplete'
import { generateTrip, type generateTripPayload } from '../../api/trip';
import dayjs from 'dayjs';
import { useState } from 'react'

const { RangePicker } = DatePicker;

type CreateTripFieldType = {
    destination: Location | null;
    dates: [dayjs.Dayjs, dayjs.Dayjs];
    travelers: number;
    budget?: number;
    departure?: Location | null;
    preferences?: string;
};

export default function CreateTripPage() {
    const { message } = AntdApp.useApp();
    const [form] = Form.useForm<CreateTripFieldType>();
    const [ submitLoading, setSubmitLoading ] = useState(false)

    const onFinish = async (values: CreateTripFieldType) => {
        setSubmitLoading(true)
        if (!values.destination) {
            message.error('Please choose a destination');
            return;
        }
        const payload: generateTripPayload = {
            // destination: values.destination,
            startDate: values.dates?.[0]?.format('YYYY-MM-DD'),
            endDate: values.dates?.[1]?.format('YYYY-MM-DD'),
            people: values.travelers,
            budget: values.budget || 0,
            currency: 'AUD',
            fromCity: values.departure?.country?.name || '',
            fromCountry: values.departure?.name || '',
            toCity: values.destination?.country?.name || '',
            toCountry: values.destination?.name || '',
            preferences: values.preferences || '',
        };
        console.log('Submit payload:', payload);

        generateTrip(payload)
            .then((data: string) => {
                message.info(data);
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

                <Form.Item<CreateTripFieldType>
                    label="Departure city"
                    name="departure"
                    rules={[{ required: true, message: 'Please choose your departure city' }]}
                    valuePropName="value"
                    getValueFromEvent={(val: Location | null) => val}
                >
                    <LocationAutoComplete placeholder="Search your departure city..." />
                </Form.Item>

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