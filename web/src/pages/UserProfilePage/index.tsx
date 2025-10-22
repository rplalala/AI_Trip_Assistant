import React, { useMemo } from "react";
import { useParams, Link } from "react-router-dom";
import {
  Breadcrumb,
  Card,
  Menu,
  Tabs,
  Form,
  Input,
  Select,
  Button,
  Row,
  Col,
  Divider,
  Switch,
  message,
} from "antd";
import type { BreadcrumbProps, MenuProps, TabsProps } from "antd";
import type { FormInstance } from "antd/es/form";
import {
  SettingOutlined,
  FileOutlined,
  TeamOutlined,
  DollarOutlined,
  UnorderedListOutlined,
  FlagOutlined,
} from "@ant-design/icons";

// ---------- 类型定义 ----------
type RouteParams = { tripId?: string };

type ProfileFormValues = {
  fullName: string;
  email: string;
  phone?: string;
  sex?: "male" | "female" | "other";
  age?: number | string; // 你若用 InputNumber 可改成 number
};

type PasswordFormValues = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type LeftMenuKey = "plan" | "itinerary" | "collaborate" | "budget" | "documents";

// ---------- 组件 ----------
export default function UserProfile(): JSX.Element {
  const { tripId } = useParams<RouteParams>();

  // 仅用于演示：把 tripId 变成可读名字（真实项目从接口拿）
  const tripName = useMemo<string>(() => {
    if (!tripId) return "Current Trip";
    return tripId
      .split("-")
      .map((s) => s[0].toUpperCase() + s.slice(1))
      .join(" ");
  }, [tripId]);

  const [formProfile] = Form.useForm<ProfileFormValues>();
  const [formPassword] = Form.useForm<PasswordFormValues>();

  const onSaveProfile = async (): Promise<void> => {
    try {
      const values = await formProfile.validateFields();
      // 提交给后端的地方
      // await api.updateProfile(values);
      // 这里只做演示：
      // eslint-disable-next-line no-console
      console.log("Profile submit:", values);
      message.success("Profile updated");
    } catch {
      // 校验失败忽略
    }
  };

  const onChangePassword = async (): Promise<void> => {
    try {
      const { currentPassword, newPassword, confirmPassword } =
        await formPassword.validateFields();

      if (newPassword !== confirmPassword) {
        message.error("New passwords do not match");
        return;
      }

      // await api.changePassword({ currentPassword, newPassword });
      // 演示：
      // eslint-disable-next-line no-console
      console.log("Password submit:", { currentPassword, newPassword });
      message.success("Password changed");
      formPassword.resetFields();
    } catch {
      // 校验失败忽略
    }
  };

  const leftMenuItems: MenuProps["items"] = [
    { key: "plan", icon: <FlagOutlined />, label: "Plan" },
    { key: "itinerary", icon: <UnorderedListOutlined />, label: "Itinerary" },
    { key: "collaborate", icon: <TeamOutlined />, label: "Collaborate" },
    { key: "budget", icon: <DollarOutlined />, label: "Budget" },
    { key: "documents", icon: <FileOutlined />, label: "Documents" },
  ];

  const breadcrumbItems: BreadcrumbProps["items"] = [
    { title: <Link to="/dashboard">Home</Link> },
    { title: "Trips" },
    { title: tripName },
  ];

  const tabsItems: TabsProps["items"] = [
    {
      key: "profile",
      label: "Profile",
      children: (
        <>
          <h3 style={{ marginTop: 0 }}>Profile Information</h3>
          <Form<ProfileFormValues>
            layout="vertical"
            form={formProfile}
            initialValues={{
              fullName: "",
              email: "",
              phone: "",
              sex: undefined,
              age: undefined,
            }}
          >
            <Row gutter={16}>
              <Col xs={24}>
                <Form.Item
                  label="Full Name"
                  name="fullName"
                  rules={[{ required: true, message: "Please enter full name" }]}
                >
                  <Input placeholder="Enter full name" />
                </Form.Item>
              </Col>

              <Col xs={24}>
                <Form.Item
                  label="Email"
                  name="email"
                  rules={[
                    { required: true, message: "Please enter email" },
                    { type: "email", message: "Invalid email" },
                  ]}
                >
                  <Input placeholder="Enter email address" />
                </Form.Item>
              </Col>

              <Col xs={24}>
                <Form.Item label="Phone Number" name="phone">
                  <Input placeholder="Enter phone number" />
                </Form.Item>
              </Col>

              <Col xs={24} md={12}>
                <Form.Item label="Sex" name="sex">
                  {/* 建议使用 options 写法，更利于类型推断 */}
                  <Select
                    placeholder="Select"
                    options={[
                      { value: "male", label: "Male" },
                      { value: "female", label: "Female" },
                      { value: "other", label: "Other" },
                    ]}
                  />
                </Form.Item>
              </Col>

              <Col xs={24} md={12}>
                <Form.Item label="Age" name="age">
                  <Input placeholder="Enter age" />
                </Form.Item>
              </Col>
            </Row>

            <Button type="primary" onClick={onSaveProfile}>
              Update Profile
            </Button>
          </Form>

          <Divider />

          <h3>Change Password</h3>
          <Form<PasswordFormValues> layout="vertical" form={formPassword}>
            <Form.Item
              label="Current Password"
              name="currentPassword"
              rules={[{ required: true, message: "Please enter current password" }]}
            >
              <Input.Password placeholder="Enter current password" />
            </Form.Item>

            <Form.Item
              label="New Password"
              name="newPassword"
              rules={[{ required: true, message: "Please enter new password" }]}
            >
              <Input.Password placeholder="Enter new password" />
            </Form.Item>

            <Form.Item
              label="Confirm New Password"
              name="confirmPassword"
              rules={[{ required: true, message: "Please confirm new password" }]}
            >
              <Input.Password placeholder="Confirm new password" />
            </Form.Item>

            <Button type="primary" onClick={onChangePassword}>
              Change Password
            </Button>
          </Form>
        </>
      ),
    },
    {
      key: "notifications",
      label: "Notifications",
      children: (
        <>
          <h3 style={{ marginTop: 0 }}>Notification Settings</h3>
          <Row gutter={[16, 16]}>
            <Col span={24}>
              <Switch defaultChecked />{" "}
              <span style={{ marginLeft: 8 }}>Email alerts for itinerary changes</span>
            </Col>
            <Col span={24}>
              <Switch />{" "}
              <span style={{ marginLeft: 8 }}>Budget threshold warnings</span>
            </Col>
            <Col span={24}>
              <Switch defaultChecked />{" "}
              <span style={{ marginLeft: 8 }}>Daily weather summary</span>
            </Col>
          </Row>
          <Divider />
          <Button type="primary" onClick={() => message.success("Notifications saved")}>
            Save
          </Button>
        </>
      ),
    },
    {
      key: "preferences",
      label: "Preferences",
      children: (
        <>
          <h3 style={{ marginTop: 0 }}>Travel Preferences</h3>
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form layout="vertical" initialValues={{ currency: "USD", language: "English" }}>
                <Form.Item label="Preferred Currency" name="currency">
                  <Select
                    options={[
                      { value: "USD", label: "USD" },
                      { value: "AUD", label: "AUD" },
                      { value: "EUR", label: "EUR" },
                      { value: "CNY", label: "CNY" },
                    ]}
                  />
                </Form.Item>
                <Form.Item label="Language" name="language">
                  <Select
                    options={[
                      { value: "English", label: "English" },
                      { value: "中文", label: "中文" },
                      { value: "日本語", label: "日本語" },
                    ]}
                  />
                </Form.Item>
                <Button type="primary" onClick={() => message.success("Preferences saved")}>
                  Save
                </Button>
              </Form>
            </Col>
          </Row>
        </>
      ),
    },
  ];

  return (
    <Row gutter={[24, 24]}>
      {/* 左侧：页面内部菜单（不影响全局 Sider） */}
      <Col xs={24} md={6} lg={5}>
        <Card bodyStyle={{ padding: 0 }} style={{ position: "sticky", top: 16 }}>
          <Menu
            mode="inline"
            defaultSelectedKeys={["plan"]}
            items={leftMenuItems}
            style={{ borderRight: 0 }}
          />
          <Divider style={{ margin: 0 }} />
          <div
            style={{
              padding: 12,
              display: "flex",
              alignItems: "center",
              gap: 8,
              color: "rgba(0,0,0,0.65)",
            }}
          >
            <SettingOutlined />
            <span>Settings</span>
          </div>
        </Card>
      </Col>

      {/* 右侧：内容区 */}
      <Col xs={24} md={18} lg={19}>
        {/* 面包屑 */}
        <Breadcrumb items={breadcrumbItems} style={{ marginBottom: 12 }} />

        <Card>
          <h2 style={{ marginTop: 0 }}>Settings</h2>
          <div style={{ color: "rgba(0,0,0,0.45)", marginBottom: 16 }}>
            Manage your account settings and preferences
          </div>

          <Tabs defaultActiveKey="profile" items={tabsItems} />
        </Card>
      </Col>
    </Row>
  );
}
