import { useCallback, useEffect, useMemo, useState } from 'react';
import { Button, Empty, Space, Table, Tag, Typography, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import {
    confirmAllBookings,
    confirmBooking,
    getBookingItems,
    type BookingItem,
} from '../../api/booking';

type BookingSectionProps = {
    tripId?: string;
    refreshSignal?: number;
};

const { Text } = Typography;

function statusColor(status: string) {
    switch (status?.toLowerCase()) {
        case 'confirm':
        case 'confirmed':
            return 'green';
        case 'failed':
            return 'red';
        case 'pending':
        default:
            return 'cyan';
    }
}

function capitalize(value: string | undefined | null) {
    if (!value) return '';
    return value.charAt(0).toUpperCase() + value.slice(1);
}

function formatMetadata(metadata?: Record<string, unknown> | null) {
    if (!metadata) return null;
    const entries = Object.entries(metadata).filter(([, value]) => value !== null && value !== undefined && value !== '');
    if (entries.length === 0) return null;
    return entries
        .map(([key, value]) => `${formatLabel(key)}: ${String(value)}`)
        .join(' · ');
}

function formatLabel(label: string) {
    return label
        .replace(/([A-Z])/g, ' $1')
        .replace(/[_-]+/g, ' ')
        .replace(/\s+/g, ' ')
        .trim()
        .replace(/^./, (ch) => ch.toUpperCase());
}

function makeRowKey(item: BookingItem) {
    return `${item.productType}-${item.entityId}`;
}

export default function BookingSection({ tripId, refreshSignal }: BookingSectionProps) {
    const [items, setItems] = useState<BookingItem[]>([]);
    const [loading, setLoading] = useState(false);
    const [confirmingId, setConfirmingId] = useState<number | null>(null);
    const [confirmingAll, setConfirmingAll] = useState(false);

    const numericTripId = useMemo(() => {
        if (!tripId) return null;
        const parsed = Number(tripId);
        return Number.isFinite(parsed) ? parsed : null;
    }, [tripId]);

    const refresh = useCallback(async () => {
        if (!numericTripId) {
            setItems([]);
            return;
        }
        setLoading(true);
        try {
            const data = await getBookingItems(numericTripId);
            setItems(Array.isArray(data) ? data : []);
        } catch (err) {
            const msg = err instanceof Error ? err.message : 'Failed to load bookings';
            message.error(msg);
        } finally {
            setLoading(false);
        }
    }, [numericTripId]);

    useEffect(() => {
        void refresh();
    }, [refresh]);

    // Trigger refresh when parent signals replan completion
    useEffect(() => {
        if (refreshSignal === undefined) return;
        void refresh();
    }, [refreshSignal, refresh]);

    const pendingCount = useMemo(() => {
        return items.filter((item) => {
            const status = item.status?.toLowerCase();
            const isConfirmed = status === 'confirm' || status === 'confirmed';
            return (item.reservationRequired ?? true) && !isConfirmed;
        }).length;
    }, [items]);

    const handleConfirm = useCallback(
        async (item: BookingItem) => {
            if (!item?.quoteRequest || !numericTripId) {
                return;
            }
            setConfirmingId(item.entityId);
            try {
                await confirmBooking(item.quoteRequest);
                message.success('Booking confirmation requested');
                await refresh();
            } catch (err) {
                const msg = err instanceof Error ? err.message : 'Failed to confirm booking';
                message.error(msg);
            } finally {
                setConfirmingId(null);
            }
        },
        [numericTripId, refresh]
    );

    const handleConfirmAll = useCallback(async () => {
        if (!numericTripId || pendingCount === 0) {
            return;
        }
        setConfirmingAll(true);
        try {
            const pendingItems = items.filter((item) => {
                const status = item.status?.toLowerCase();
                const isConfirmed = status === 'confirm' || status === 'confirmed';
                return (item.reservationRequired ?? true) && !isConfirmed;
            });
            await confirmAllBookings(numericTripId, pendingItems);
            message.success('Confirmation requested for all pending items');
            await refresh();
        } catch (err) {
            const msg = err instanceof Error ? err.message : 'Failed to confirm all bookings';
            message.error(msg);
        } finally {
            setConfirmingAll(false);
        }
    }, [items, numericTripId, pendingCount, refresh]);

    const columns: ColumnsType<BookingItem> = useMemo(
        () => [
            {
                title: 'Item',
                dataIndex: 'title',
                key: 'title',
                render: (_: unknown, item) => {
                    const metadataText = formatMetadata(item.metadata);
                    return (
                        <Space direction="vertical" size={4}>
                            <Text strong>{item.title || capitalize(item.productType)}</Text>
                            <Space size={8} wrap>
                                <Tag color="cyan">{capitalize(item.productType)}</Tag>
                                {item.subtitle ? <Text type="secondary">{item.subtitle}</Text> : null}
                            </Space>
                            {metadataText ? (
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                    {metadataText}
                                </Text>
                            ) : null}
                        </Space>
                    );
                },
            },
            {
                title: 'Date',
                dataIndex: 'date',
                key: 'date',
                render: (value: string | null | undefined, item) => (
                    <Space direction="vertical" size={0}>
                        <Text>{value ?? 'TBD'}</Text>
                        {item.time ? <Text type="secondary">{item.time}</Text> : null}
                    </Space>
                ),
            },
            {
                title: 'Status',
                dataIndex: 'status',
                key: 'status',
                align: 'right',
                render: (value: string, item) => {
                    const normalized = value || 'pending';
                    const normalizedLower = normalized.toLowerCase();
                    const color = statusColor(normalized);
                    const isConfirmed = normalizedLower === 'confirm' || normalizedLower === 'confirmed';
                    const disabled =
                        isConfirmed ||
                        confirmingAll ||
                        confirmingId === item.entityId ||
                        item.reservationRequired === false ||
                        !item.quoteRequest;
                    return (
                        <Space direction="vertical" align="end" size={4}>
                            <Tag color={color}>{capitalize(normalized)}</Tag>
                            {item.quoteSummary?.voucherCode ? (
                                <Text type="secondary" style={{ fontSize: 12 }}>
                                    Voucher: {item.quoteSummary.voucherCode}
                                </Text>
                            ) : null}
                            <Button
                                type="primary"
                                size="small"
                                onClick={() => handleConfirm(item)}
                                disabled={disabled}
                                loading={confirmingId === item.entityId}
                            >
                                Confirm
                            </Button>
                        </Space>
                    );
                },
            },
        ],
        [confirmingAll, confirmingId, handleConfirm]
    );

    if (!numericTripId) {
        return <Empty description="Select a trip to view booking tasks" />;
    }

    return (
        <Space direction="vertical" style={{ width: '100%' }} size="large">
            <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                    type="primary"
                    onClick={handleConfirmAll}
                    disabled={confirmingAll || pendingCount === 0}
                    loading={confirmingAll}
                >
                    Confirm All
                </Button>
            </div>
            <Table<BookingItem>
                rowKey={makeRowKey}
                dataSource={items}
                columns={columns}
                loading={loading || confirmingAll}
                pagination={false}
                locale={{
                    emptyText: <Empty description="No booking actions pending" />,
                }}
            />
        </Space>
    );
}
