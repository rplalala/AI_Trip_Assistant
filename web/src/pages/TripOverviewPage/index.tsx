// src/pages/Trips/Overview/index.tsx
import React, {useCallback, useEffect, useMemo, useState} from 'react';
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
    Tooltip,
    Spin,
    Alert,
} from 'antd';
import type {BreadcrumbProps, TabsProps} from 'antd';
import {HomeOutlined, EnvironmentOutlined, CarOutlined, ReloadOutlined} from '@ant-design/icons';
import BookingSection from './BookingSection';
import {generateRoute, type MapRouteResponse} from '../../api/map';

const {Title, Text} = Typography;

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

type TimelineRouteContext = {
    origin: string;
    destination: string;
    destinationLabel?: string | null;
    label: string;
    travelMode: string;
    type: 'hotel' | 'attraction' | 'transportation';
};

function DayTimeline({day, trip}: { day: TimeLineDTO; trip: TripDetail | null }) {
    const [routeModalOpen, setRouteModalOpen] = useState(false);
    const [routeModalLoading, setRouteModalLoading] = useState(false);
    const [routeModalData, setRouteModalData] = useState<MapRouteResponse | null>(null);
    const [routeModalTarget, setRouteModalTarget] = useState<TimelineRouteContext | null>(null);
    const [routeError, setRouteError] = useState<string | null>(null);

    const attractions = sortByTime<AttractionTimeLineDTO>(day.attraction);
    const hotels = sortByTime<HotelTimeLineDTO>(day.hotel);
    const transports = sortByTime<TransportationTimeLineDTO>(day.transportation);

    type BaseItem = {
        order: number;
        time?: string | null;
        title: string;
        subtitle?: string | null;
        type: 'hotel' | 'attraction' | 'transportation';
        originHint?: string | null;
        destinationHint?: string | null;
    };

    let orderCounter = 0;
    const baseItems: BaseItem[] = [];

    hotels.forEach((h) => {
        const title = h.title || h.hotelName || 'Hotel';
        const destination = h.hotelName || h.title || title;
        baseItems.push({
            order: orderCounter++,
            time: h.time,
            title,
            subtitle: h.hotelName && h.hotelName !== title ? h.hotelName : undefined,
            type: 'hotel',
            destinationHint: destination,
        });
    });

    attractions.forEach((a) => {
        const title = a.title || a.location || 'Attraction';
        const location = a.location || title;
        baseItems.push({
            order: orderCounter++,
            time: a.time,
            title,
            subtitle: a.location && a.location !== title ? a.location : undefined,
            type: 'attraction',
            destinationHint: location,
        });
    });

    transports.forEach((t) => {
        const title = t.title || 'Transportation';
        const from = t.from || null;
        const to = t.to || null;
        const subtitle = from && to ? `${from} → ${to}` : from || to || undefined;
        baseItems.push({
            order: orderCounter++,
            time: t.time,
            title,
            subtitle,
            type: 'transportation',
            originHint: from,
            destinationHint: to || title,
        });
    });

    const defaultOrigin = trip?.toCity || trip?.toCountry || trip?.fromCity || undefined;
    let previousLocation = defaultOrigin;

    const items = baseItems
        .sort((a, b) => {
            const diff = parseTimeToMinutes(a.time) - parseTimeToMinutes(b.time);
            if (diff !== 0) return diff;
            return a.order - b.order;
        })
        .map((item, idx) => {
            const destinationCandidate = (item.destinationHint ?? item.subtitle ?? item.title)?.trim() || null;
            const originCandidate = (item.originHint ?? previousLocation)?.trim() || null;

            let route: TimelineRouteContext | null = null;
            const origin = originCandidate;
            const destination = destinationCandidate;

            if (origin && destination && origin !== destination) {
                route = {
                    origin,
                    destination,
                    destinationLabel: destination,
                    label: item.time ? `${item.time} ${item.title}` : item.title,
                    travelMode: item.type === 'transportation' ? 'transit' : 'walking',
                    type: item.type,
                };
            }

            if (destination) {
                previousLocation = destination;
            } else if (origin) {
                previousLocation = origin;
            }

            return {
                key: `${item.type}-${idx}`,
                ...item,
                route,
            };
        });

    const handleOpenRoute = useCallback(
        async (route: TimelineRouteContext) => {
            if (!route.origin || !route.destination) {
                message.warning('Route information is incomplete for this activity');
                return;
            }
            setRouteModalTarget(route);
            setRouteModalOpen(true);
            setRouteModalLoading(true);
            setRouteModalData(null);
            setRouteError(null);
            try {
                const data = await generateRoute({
                    origin: route.origin,
                    destination: route.destination,
                    travelMode: route.travelMode,
                });
                setRouteModalData(data);
            } catch (err) {
                const errMsg = err instanceof Error ? err.message : 'Failed to generate route';
                setRouteError(errMsg);
                message.error(errMsg);
            } finally {
                setRouteModalLoading(false);
            }
        },
        []
    );

    const handleCloseRouteModal = useCallback(() => {
        setRouteModalOpen(false);
        setRouteModalLoading(false);
        setRouteModalTarget(null);
        setRouteModalData(null);
        setRouteError(null);
    }, []);

    const dotFor = (type: BaseItem['type'], size = 14) => {
        if (type === 'hotel') return <HomeOutlined style={{color: '#fa8c16', fontSize: size}}/>;
        if (type === 'transportation') return <CarOutlined style={{color: '#1677ff', fontSize: size}}/>;
        return <EnvironmentOutlined style={{color: '#52c41a', fontSize: size}}/>;
    };

    const IMAGE_W = 200;
    const IMAGE_H = 150;
    const ITEM_ROW_HEIGHT = 50;
    const MIN_ROW_GAP = 3;
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
                    ...items.map((it) => ({
                        key: it.key,
                        dot: dotFor(it.type, 16),
                        children: (
                            <div
                                style={{
                                    whiteSpace: 'normal',
                                    padding: '6px 0',
                                    lineHeight: '22px',
                                }}
                            >
                                <Space direction="vertical" size={4} style={{width: '100%'}}>
                                    <div style={{display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8}}>
                                        <Space size={6} wrap>
                                            {it.time ? <Text strong>{it.time}</Text> : null}
                                            <Text style={{fontWeight: 500}}>{it.title}</Text>
                                        </Space>
                                        {it.route ? (
                                            <Tooltip title={it.route.destinationLabel ? `Route to ${it.route.destinationLabel}` : 'Open in Google Maps'}>
                                                <Button
                                                    size="small"
                                                    type="text"
                                                    icon={<EnvironmentOutlined />}
                                                    onClick={() => handleOpenRoute(it.route!)}
                                                />
                                            </Tooltip>
                                        ) : null}
                                    </div>
                                    {it.subtitle ? (
                                        <Text type="secondary" style={{fontSize: 12}}>
                                            {it.subtitle}
                                        </Text>
                                    ) : null}
                                </Space>
                            </div>
                        ),
                    })),
                ]}
            />

            <Modal
                open={routeModalOpen}
                onCancel={handleCloseRouteModal}
                footer={null}
                width={720}
                title={
                    routeModalTarget
                        ? `Route: ${routeModalTarget.origin} → ${routeModalTarget.destination}`
                        : 'Route'
                }
            >
                {routeModalLoading ? (
                    <div style={{display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '40px 0'}}>
                        <Spin size="large"/>
                    </div>
                ) : routeError ? (
                    <Alert type="error" message={routeError} showIcon/>
                ) : routeModalData ? (
                    <Space direction="vertical" size="middle" style={{width: '100%'}}>
                        {routeModalData.embedUrl ? (
                            <iframe
                                title={routeModalTarget?.destinationLabel ? `Route to ${routeModalTarget.destinationLabel}` : 'Route preview'}
                                src={routeModalData.embedUrl ?? undefined}
                                style={{border: 0, width: '100%', height: 360}}
                                loading="lazy"
                                allowFullScreen
                            />
                        ) : null}
                        <Space direction="vertical" size={4}>
                            {routeModalData.routeSummary ? <Text strong>{routeModalData.routeSummary}</Text> : null}
                            <Space size={16} wrap>
                                {routeModalData.distanceText ? <Text>Distance: {routeModalData.distanceText}</Text> : null}
                                {routeModalData.durationText ? <Text>Duration: {routeModalData.durationText}</Text> : null}
                            </Space>
                        </Space>
                        {routeModalData.warnings && routeModalData.warnings.length ? (
                            <Alert
                                type="warning"
                                showIcon
                                message="Warnings"
                                description={routeModalData.warnings.join('; ')}
                            />
                        ) : null}
                        {routeModalData.shareUrl ? (
                            <Button
                                type="primary"
                                href={routeModalData.shareUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                            >
                                Open in Google Maps
                            </Button>
                        ) : null}
                    </Space>
                ) : (
                    <Text type="secondary">No route data available.</Text>
                )}
            </Modal>
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
                                        <DayTimeline key={idx} day={day} trip={trip}/>
                                    ))}
                                </Space>
                            )}
                        </Card>
                    ) : (
                        <Card title="Booking Tasks">
                            <BookingSection tripId={tripId}/>
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
