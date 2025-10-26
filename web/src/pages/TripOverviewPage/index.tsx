// src/pages/Trips/Overview/index.tsx
import React, { useEffect, useState } from 'react'
import { Link, useParams } from "react-router-dom";
import { getTripInsights, type TripInsightsResponse } from '../../api/trip';
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
} from "antd";
import type { BreadcrumbProps, TabsProps, TableProps } from "antd";
import {
  HomeOutlined,
  CoffeeOutlined,
  ShopOutlined,
  RocketOutlined,
} from "@ant-design/icons";

const { Title, Text } = Typography;

// ---------- 类型 ----------
type Trip = {
  id: string;
  title: string;
  dateRange: string;
  travelers: number;
};

type TimelineEvent = {
  day: string;
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  color: string;
  warn?: boolean;
};

type BookingRow = {
  key: React.Key;
  name: string;
  date: string;
  status: "Confirmed" | "Pending";
};

// ---- Mock data（后续可换成接口返回）----
const mockTrip = (tripId: string): Trip => ({
  id: tripId,
  title: "Tokyo Spring",
  dateRange: "Mar 20, 2025 – Mar 23, 2025",
  travelers: 4,
});

const timelineEvents: TimelineEvent[] = [
  {
    day: "Mar 20",
    icon: <RocketOutlined />,
    title: "Flight to Tokyo",
    subtitle: "Hotel Check-in",
    color: "#1677ff",
  },
  {
    day: "Mar 21",
    icon: <CoffeeOutlined />,
    title: "Shinjuku Gyoen",
    subtitle: "Sushi Saito",
    color: "#52c41a",
  },
  {
    day: "Mar 22",
    icon: <ShopOutlined />,
    title: "Ghibli Museum",
    subtitle: "Budget Warning",
    color: "#faad14",
    warn: true,
  },
  {
    day: "Mar 23",
    icon: <HomeOutlined />,
    title: "Departure",
    subtitle: "",
    color: "#1677ff",
  },
];

const bookings: BookingRow[] = [
  { key: 1, name: "Flight to Tokyo", date: "Mar 20, 2025", status: "Confirmed" },
  { key: 2, name: "Hotel in Shinjuku", date: "Mar 20–23, 2025", status: "Confirmed" },
  { key: 3, name: "Dinner at Sushi Saito", date: "Mar 21, 2025", status: "Pending" },
];

// Table 列（强类型）
const bookingColumns: TableProps<BookingRow>["columns"] = [
  { title: "Booking", dataIndex: "name" },
  { title: "Date", dataIndex: "date" },
  {
    title: "Status",
    dataIndex: "status",
    render: (v: BookingRow["status"]) =>
      v === "Confirmed" ? <Tag color="green">Confirmed</Tag> : <Tag>Pending</Tag>,
    align: "right",
  },
];

export default function TripOverview() {
  const { tripId } = useParams<{ tripId?: string }>();
  const effectiveTripId = tripId ?? "1";
  const trip = mockTrip(effectiveTripId);
  const [tripInsights, setTripInsights] = useState<TripInsightsResponse[]>([]);

  useEffect(() => {
    if (!tripId) return;

    getTripInsights(tripId)
        .then((res) => {
          setTripInsights(res);
        })
  }, [tripId]);

  const tabsItems: TabsProps["items"] = [
    { key: "timeline", label: "Timeline", children: null },
    { key: "plan", label: "Plan", children: null },
    { key: "book", label: "Book", children: null },
    { key: "budget", label: "Budget", children: null },
    { key: "members", label: "Members", children: null },
  ];

  const breadcrumbItems: BreadcrumbProps["items"] = [
    { title: <Link to="/dashboard">Home</Link> },
    { title: "Trips" },
    { title: trip.title },
  ];

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      {/* 顶部面包屑 */}
      <Breadcrumb items={breadcrumbItems} />

      {/* 标题区 */}
      <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <Title level={2} style={{ margin: 0 }}>
            {trip.title}
          </Title>
          <Text type="secondary">
            {trip.dateRange} · {trip.travelers} travelers
          </Text>
        </div>
      </div>

      {/* Tabs */}
      <Tabs defaultActiveKey="timeline" items={tabsItems} />

      {/* 主体两栏 */}
      <Row gutter={[24, 24]}>
        {/* 左侧：时间轴 + 预订列表 */}
        <Col xs={24} lg={16}>
          <Card title="Trip Timeline">
            {/* 自定义横向时间轴 */}
            <div style={{ padding: "16px 8px" }}>
              <div
                style={{
                  position: "relative",
                  height: 120,
                  borderTop: "2px solid rgba(0,0,0,0.08)",
                  display: "flex",
                  justifyContent: "space-between",
                }}
              >
                {timelineEvents.map((e, idx) => (
                  <div
                    key={idx}
                    style={{
                      textAlign: "center",
                      width: "25%",
                      position: "relative",
                    }}
                  >
                    {/* 节点 */}
                    <div
                      style={{
                        position: "absolute",
                        top: 40,
                        left: "50%",
                        transform: "translateX(-50%)",
                        background: "#fff",
                        border: `2px solid ${e.color}`,
                        color: e.color,
                        width: 36,
                        height: 36,
                        borderRadius: "50%",
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        fontSize: 16,
                        boxShadow: "0 2px 6px rgba(0,0,0,0.05)",
                      }}
                      title={e.title}
                    >
                      {e.icon}
                    </div>

                    {/* 日期标注 */}
                    <div
                      style={{
                        position: "absolute",
                        top: 20,
                        left: "50%",
                        transform: "translateX(-50%)",
                        fontWeight: 600,
                      }}
                    >
                      {e.day}
                    </div>

                    {/* 文本 */}
                    <div
                      style={{
                        position: "absolute",
                        top: 84,
                        left: "50%",
                        transform: "translateX(-50%)",
                      }}
                    >
                      <div style={{ fontSize: 12 }}>{e.title}</div>
                      <div
                        style={{
                          fontSize: 12,
                          color: e.warn ? "#fa541c" : "rgba(0,0,0,0.45)",
                        }}
                      >
                        {e.subtitle}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </Card>

          <Card title="Next Bookings" style={{ marginTop: 16 }}>
            <Table<BookingRow>
              rowKey="key"
              dataSource={bookings}
              pagination={false}
              columns={bookingColumns}
            />
          </Card>
        </Col>

        {/* 右侧：AI Generated Trip Insights */}
        <Col xs={24} lg={8}>
          <Card title="Trip Insights">
            <List<TripInsightsResponse>
              itemLayout="horizontal"
              dataSource={tripInsights}
              renderItem={(item) => (
                <List.Item>
                  <List.Item.Meta
                    avatar={<div style={{ fontSize: 18 }}>{item.icon}</div>}
                    title={<div style={{ fontWeight: 600 }}>{item.title}</div>}
                    description={<Text>{item.content}</Text>}
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
