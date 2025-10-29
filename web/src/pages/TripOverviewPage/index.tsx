// src/pages/Trips/Overview/index.tsx
import React, {useEffect, useMemo, useState} from 'react';
import {Link, useParams} from 'react-router-dom';
import {
    getTripDetails,
    getTripInsights,
    getTripTimeline,
    type TripDetail,
    type TripInsightsResponse,
    type TimeLineDTO,
    type AttractionTimeLineDTO,
    type HotelTimeLineDTO,
    type TransportationTimeLineDTO,
    regenerateTrip,
    type ModifyPlanPayload,
} from '../../api/trip';
import {
    Breadcrumb,
    Typography,
    Row,
    Col,
    Card,
    Tabs,
    List,
    Tag,
    Table,
    Space,
    Empty,
    Skeleton,
    Timeline,
    Image,
    Button,
    Modal,
    Form,
    Input,
    message,
} from 'antd';
import type {BreadcrumbProps, TabsProps, TableProps} from 'antd';
import {HomeOutlined, EnvironmentOutlined, CarOutlined, ReloadOutlined} from '@ant-design/icons';

const {Title, Text} = Typography;

// Types for Bookings table (placeholder for now)
type BookingRow = {
    key: React.Key;
    name: string;
    date: string;
    status: 'Confirmed' | 'Pending';
};

const bookingColumns: TableProps<BookingRow>['columns'] = [
    {title: 'Booking', dataIndex: 'name'},
    {title: 'Date', dataIndex: 'date'},
    {
        title: 'Status',
        dataIndex: 'status',
        render: (v: BookingRow['status']) =>
            v === 'Confirmed' ? <Tag color="green">Confirmed</Tag> : <Tag>Pending</Tag>,
        align: 'right',
    },
];

const mockBookings: BookingRow[] = [
    {key: 1, name: 'Flight', date: 'Mar 20, 2025', status: 'Confirmed'},
    {key: 2, name: 'Hotel', date: 'Mar 21, 2025', status: 'Confirmed'},
];

function parseTimeToMinutes(time?: string | null): number {
    if (!time) return Number.MAX_SAFE_INTEGER;
    const m = time.match(/^(\d{1,2}):(\d{2})/);
    if (!m) return Number.MAX_SAFE_INTEGER;
    const hh = parseInt(m[1]!, 10);
    const mm = parseInt(m[2]!, 10);
    return hh * 60 + mm;
}

function sortByTime<T extends { time?: string | null }>(arr?: T[] | null): T[] {
    return [...(arr ?? [])].sort((a, b) => parseTimeToMinutes(a.time) - parseTimeToMinutes(b.time));
}

function DayTimeline({day}: { day: TimeLineDTO }) {
    const attractions = sortByTime<AttractionTimeLineDTO>(day.attraction);
    const hotels = sortByTime<HotelTimeLineDTO>(day.hotel);
    const transports = sortByTime<TransportationTimeLineDTO>(day.transportation);

    type Item = { time?: string | null; label: string; type: 'hotel' | 'attraction' | 'transportation' };
    const items: Item[] = [
        ...hotels.map((h) => ({
            time: h.time,
            type: 'hotel' as const,
            label: `${h.time ? `${h.time} ` : ''}${h.title || h.hotelName || 'Hotel'}`,
        })),
        ...attractions.map((a) => ({
            time: a.time,
            type: 'attraction' as const,
            label: `${a.time ? `${a.time} ` : ''}${a.title || a.location || 'Attraction'}`,
        })),
        ...transports.map((t) => ({
            time: t.time,
            type: 'transportation' as const,
            label: `${t.time ? `${t.time} ` : ''}${t.title || 'Transportation'}`,
        })),
    ].sort((a, b) => parseTimeToMinutes(a.time) - parseTimeToMinutes(b.time));

    const dotFor = (type: Item['type'], size = 14) => {
        if (type === 'hotel') return <HomeOutlined style={{color: '#fa8c16', fontSize: size}}/>;
        if (type === 'transportation') return <CarOutlined style={{color: '#1677ff', fontSize: size}}/>;
        return <EnvironmentOutlined style={{color: '#52c41a', fontSize: size}}/>;
    };

    const IMAGE_W = 200;
    const IMAGE_H = 150;
    // Approximate height of one itinerary row (lineHeight 22 + vertical padding ~6)
    const ITEM_ROW_HEIGHT = 50;
    const MIN_ROW_GAP = 3; // keep at least 3 rows worth of spacing
    const MIN_CONTAINER_HEIGHT = IMAGE_H + ITEM_ROW_HEIGHT * MIN_ROW_GAP;

    const weatherIconMap: Record<string, React.ReactNode> = {
        clouds: '☁️',
        rain: '🌧️',
        thunderstorm: '⛈️',
        drizzle: '🌦️',
        snow: '❄️',
        clear: '☀️',
    };

    const weatherNode = (() => {
        const cond = day.weatherCondition?.trim();
        const condKey = cond?.toLowerCase() ?? '';
        const icon = weatherIconMap[condKey];
        const tempText = (day.minTemperature != null || day.maxTemperature != null)
            ? `${day.minTemperature ?? ''}${day.minTemperature != null ? '°' : ''}${day.maxTemperature != null ? ` – ${day.maxTemperature}°` : ''}`
            : '';
        if (!cond && !tempText) return null;
        return (
            <div style={{marginTop: 4, color: 'rgba(0,0,0,0.45)', fontWeight: 400, display: 'flex', alignItems: 'center', gap: 6}}>
                {icon ? (
                    <span style={{fontSize: 16, lineHeight: 1}}>{icon}</span>
                ) : cond ? (
                    <span>{cond}</span>
                ) : null}
                {tempText ? <span>{tempText}</span> : null}
            </div>
        );
    })();

    return (
        <div style={{position: 'relative', paddingRight: IMAGE_W + 24, minHeight: MIN_CONTAINER_HEIGHT}}>
            {/* Scoped style to remove any horizontal connector lines inside this timeline */}
            <style>{`
        .day-timeline .ant-timeline-item-content::before { display: none !important; }
        .day-timeline .ant-timeline-item-head-custom { top: 6px; width: 18px; height: 18px; line-height: 18px; }
      `}</style>

            {day.imageUrl ? (
                <div style={{position: 'absolute', right: 0, top: 20}}>
                    <Image
                        src={day.imageUrl}
                        alt={day.summary || day.date}
                        width={IMAGE_W}
                        height={IMAGE_H}
                        style={{
                            objectFit: 'cover',
                            borderRadius: 8,
                            border: '1px solid rgba(0,0,0,0.06)'
                        }}
                        preview={{
                            mask: null,
                        }}
                    />
                </div>
            ) : null}

            <Timeline
                className="day-timeline"
                items={[
                    {
                        color: 'red',
                        children: (
                            <div>
                                <div style={{fontWeight: 600}}>
                                    {day.date}
                                    {day.summary ? <span> · {day.summary}</span> : null}
                                </div>
                                {weatherNode}
                            </div>
                        ),
                    },
                    // Render each activity as its own timeline item with a dot icon
                    ...items.map((it, idx) => ({
                        dot: dotFor(it.type, 16),
                        children: (
                            <div
                                key={idx}
                                style={{
                                    whiteSpace: 'nowrap',
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    padding: '6px 0',
                                    lineHeight: '22px',
                                }}
                            >
                                {it.label}
                            </div>
                        ),
                    })),
                ]}
            />
        </div>
    );
}

export default function TripOverview() {
    const {tripId} = useParams<{ tripId?: string }>();
    const [trip, setTrip] = useState<TripDetail | null>(null);
    const [timeline, setTimeline] = useState<TimeLineDTO[]>([]);
    const [loadingTimeline, setLoadingTimeline] = useState<boolean>(false);
    const [tripInsights, setTripInsights] = useState<TripInsightsResponse[]>([]);
    const [activeTab, setActiveTab] = useState<'timeline' | 'book'>('timeline');

    const messageApi = message;

    const reloadData = async (tid: string) => {
        setLoadingTimeline(true);
        try {
            const [insights, tl] = await Promise.all([
                getTripInsights(tid),
                getTripTimeline(tid),
            ]);
            setTripInsights(insights ?? []);
            setTimeline(tl ?? []);
        } finally {
            setLoadingTimeline(false);
        }
    };

    useEffect(() => {
        let mounted = true;
        if (!tripId) return;

        getTripDetails()
            .then((list) => {
                if (!mounted) return;
                const found = (list ?? []).find((t) => String(t.tripId) === String(tripId));
                setTrip(found ?? null);
            });

        reloadData(tripId);

        return () => {
            mounted = false;
        };
    }, [tripId]);

    const [replanOpen, setReplanOpen] = useState(false);
    const [submittingReplan, setSubmittingReplan] = useState(false);
    const [form] = Form.useForm<{ secondPreference: string }>();

    const onOpenReplan = () => {
        form.resetFields();
        setReplanOpen(true);
    };

    const onSubmitReplan = async () => {
        try {
            const values = await form.validateFields();
            if (!tripId) return;
            setSubmittingReplan(true);
            const payload: ModifyPlanPayload = { secondPreference: values.secondPreference.trim() };
            await regenerateTrip(tripId, payload);
            setReplanOpen(false);
            messageApi.success('Plan regenerated');
            await reloadData(tripId);
        } catch (err: unknown) {
            const isValidationError = (e: unknown): e is { errorFields: unknown } =>
                !!e && typeof e === 'object' && 'errorFields' in (e as Record<string, unknown>);
            if (!isValidationError(err)) {
                const errMsg = err instanceof Error ? err.message : 'Failed to regenerate plan';
                messageApi.error(errMsg);
            }
        } finally {
            setSubmittingReplan(false);
        }
    };

    const tabsItems: TabsProps['items'] = [
        {key: 'timeline', label: 'Timeline', children: null},
        {key: 'book', label: 'Book', children: null},
    ];

    const breadcrumbItems: BreadcrumbProps['items'] = [
        {title: <Link to="/">Home</Link>},
        {title: <Link to="/trips">Trips</Link>},
        {title: trip ? (trip.toCity || trip.toCountry || 'Trip') : 'Trip'},
    ];

    const titleText = useMemo(() => {
        const name = trip ? (trip.toCity || trip.toCountry || 'Trip') : 'Trip';
        const dateText = trip ? `${trip.startDate} – ${trip.endDate}` : '';
        const travelers = trip?.people ? `${trip.people} travelers` : '';
        const budgetText = typeof trip?.budget === 'number' ? `Budget: $${trip.budget.toLocaleString?.() ?? trip.budget}` : '';
        return {name, dateText, travelers, budgetText};
    }, [trip]);

    return (
        <Space direction="vertical" size="large" style={{width: '100%'}}>
            <Breadcrumb items={breadcrumbItems}/>

            <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between'}}>
                <div>
                    <Title level={2} style={{margin: 0}}>
                        {titleText.name}
                    </Title>
                    <Text type="secondary">
                        {[titleText.dateText, titleText.travelers, titleText.budgetText].filter(Boolean).join(' · ')}
                    </Text>
                </div>
                <div>
                    <Button type="primary" icon={<ReloadOutlined />} onClick={onOpenReplan} disabled={!tripId || submittingReplan}>
                        Replan
                    </Button>
                </div>
            </div>

            <Tabs
                defaultActiveKey="timeline"
                items={tabsItems}
                activeKey={activeTab}
                onChange={(k) => setActiveTab(k as 'timeline' | 'book')}
            />

            <Row gutter={[24, 24]}>
                <Col xs={24} lg={16}>
                    {activeTab === 'timeline' ? (
                        <Card title="Trip Timeline">
                            {loadingTimeline ? (
                                <Skeleton active paragraph={{rows: 6}}/>
                            ) : timeline.length === 0 ? (
                                <Empty description="No timeline"/>
                            ) : (
                                <Space direction="vertical" style={{width: '100%'}} size={16}>
                                    {timeline.map((day, idx) => (
                                        <DayTimeline key={idx} day={day}/>
                                    ))}
                                </Space>
                            )}
                        </Card>
                    ) : (
                        <Card title="Next Bookings">
                            <Table<BookingRow>
                                rowKey="key"
                                dataSource={mockBookings}
                                pagination={false}
                                columns={bookingColumns}
                            />
                        </Card>
                    )}
                </Col>

                <Col xs={24} lg={8}>
                    <Card title="Trip Insights">
                        <List<TripInsightsResponse>
                            itemLayout="horizontal"
                            dataSource={tripInsights}
                            renderItem={(item) => (
                                <List.Item>
                                    <List.Item.Meta
                                        avatar={<div style={{fontSize: 18}}>{item.icon}</div>}
                                        title={<div style={{fontWeight: 600}}>{item.title}</div>}
                                        description={<Text>{item.content}</Text>}
                                    />
                                </List.Item>
                            )}
                        />
                    </Card>
                </Col>
            </Row>

            <Modal
                title="Regenerate plan"
                open={replanOpen}
                onCancel={() => setReplanOpen(false)}
                onOk={onSubmitReplan}
                okText="Confirm"
                confirmLoading={submittingReplan}
            >
                <Form form={form} layout="vertical" preserve={false}>
                    <Form.Item
                        label="New preference"
                        name="secondPreference"
                        rules={[{ required: true, message: 'Please enter your new preference' }]}
                    >
                        <Input.TextArea placeholder="e.g., Prefer museums; avoid theme parks; daily walking under 6km" rows={4} maxLength={1000} showCount/>
                    </Form.Item>
                </Form>
            </Modal>
        </Space>
    );
}
