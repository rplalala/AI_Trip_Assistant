import React from "react";
import {
  Breadcrumb,
  Typography,
  Space,
  Button,
  Select,
  Row,
  Col,
  Card,
} from "antd";
import type { DefaultOptionType } from "antd/es/select";
import { PlusOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router-dom";

const { Title, Text } = Typography;

// ---- 类型定义 ----
type Trip = {
  id: string;
  title: string;
  date: string;       // 也可以拆成 { start: Date; end: Date }，这里只保持与原始数据一致
  travelers: number;
  budget: number;
  cover: string;
};

type StatusFilter = "All" | "Upcoming" | "Past";
type PublishFilter = "Draft" | "Published";
type ActiveFilter = "Active" | "Archived";

type Option<T extends string> = {
  value: T;
  label: string;
};

// ---- 静态数据 ----
const trips: Trip[] = [
  {
    id: "tokyo-spring",
    title: "Tokyo Spring",
    date: "Mar 20 – Mar 23, 2025",
    travelers: 4,
    budget: 6000,
    cover:
      "https://images.unsplash.com/photo-1540959733332-eab4deabeeaf?q=80&w=1600&auto=format&fit=crop",
  },
  {
    id: "paris-getaway",
    title: "Paris Getaway",
    date: "Apr 15 – Apr 20, 2025",
    travelers: 2,
    budget: 4500,
    cover:
      "https://images.unsplash.com/photo-1511735111819-9a3f7709049c?q=80&w=1600&auto=format&fit=crop",
  },
  {
    id: "ny-adventure",
    title: "New York Adventure",
    date: "May 10 – May 15, 2025",
    travelers: 3,
    budget: 5000,
    cover:
      "https://images.unsplash.com/photo-1490578474895-699cd4e2cf59?q=80&w=1600&auto=format&fit=crop",
  },
];

// ---- 下拉选项（带类型）----
const statusOptions: Option<StatusFilter>[] = [
  { value: "All", label: "All" },
  { value: "Upcoming", label: "Upcoming" },
  { value: "Past", label: "Past" },
];

const publishOptions: Option<PublishFilter>[] = [
  { value: "Draft", label: "Draft" },
  { value: "Published", label: "Published" },
];

const activeOptions: Option<ActiveFilter>[] = [
  { value: "Active", label: "Active" },
  { value: "Archived", label: "Archived" },
];

export default function TripsPage(): JSX.Element {
  const navigate = useNavigate();

  return (
    <Space direction="vertical" size="large" style={{ width: "100%" }}>
      {/* 面包屑 */}
      <Breadcrumb items={[{ title: "Home" }, { title: "Trips" }]} />

      {/* 标题 + 按钮 */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "space-between",
          gap: 12,
        }}
      >
        <Title level={1} style={{ margin: 0, fontWeight: 700 }}>
          My Trips
        </Title>

        <Button
          type="default"
          icon={<PlusOutlined />}
          onClick={() => navigate("/trips/create")}
        >
          Create Trip
        </Button>
      </div>

      {/* 筛选条 */}
      <Space size="middle" wrap>
        <Select<StatusFilter, DefaultOptionType>
          defaultValue="All"
          options={statusOptions}
          style={{ width: 120 }}
        />
        <Select<PublishFilter, DefaultOptionType>
          defaultValue="Draft"
          options={publishOptions}
          style={{ width: 120 }}
        />
        <Select<ActiveFilter, DefaultOptionType>
          defaultValue="Active"
          options={activeOptions}
          style={{ width: 120 }}
        />
      </Space>

      {/* 卡片网格 */}
      <Row gutter={[24, 24]}>
        {trips.map((t: Trip) => (
          <Col key={t.id} xs={24} sm={12} lg={8}>
            <Card
              hoverable
              onClick={() => navigate(`/trips/${t.id}`)}
              bodyStyle={{ padding: 14 }} // 与原注释保持一致
              style={{
                borderRadius: 12,
                overflow: "hidden",
                boxShadow: "0 2px 6px rgba(0,0,0,0.06)",
              }}
              cover={
                <img
                  alt={t.title}
                  src={t.cover}
                  style={{
                    width: "100%",
                    height: 200,
                    objectFit: "cover",
                  }}
                />
              }
            >
              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                <Text strong style={{ fontSize: 16 }}>
                  {t.title}
                </Text>
                <Text type="secondary">
                  {t.date} · {t.travelers} travelers · Budget:{" "}
                  <Text underline>${t.budget.toLocaleString()}</Text>
                </Text>
              </div>
            </Card>
          </Col>
        ))}
      </Row>
    </Space>
  );
}
