import { App as AntdApp, Alert, Avatar, Button, Card, Divider, Flex, Form, Input, InputNumber, Modal, Select, Space, Typography } from 'antd';
import { useEffect, useRef, useState, type ChangeEvent } from 'react';
import { UploadOutlined, LinkOutlined, MailOutlined, DeleteOutlined, LockOutlined, UserOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import {
  changePassword,
  deleteUserAccount,
  sendChangeEmailLink,
  type UpdateProfilePayload,
  updateUserProfile,
  uploadAvatar,
  uploadAvatarByUrl,
  verifyCurrentPassword,
} from '../../api/user';
import { useAuth } from '../../contexts/AuthContext';

type ProfileFormValues = {
  username: string;
  gender?: number;
  age?: number;
};

export default function UserProfilePage() {
  const { message, modal } = AntdApp.useApp();
  const navigate = useNavigate();
  const { user, refreshProfile, setStatus, setUser } = useAuth();

  const [profileForm] = Form.useForm<ProfileFormValues>();
  const [deleteAccountForm] = Form.useForm<{ password: string }>();
  const [verifyPasswordForm] = Form.useForm<{ currentPassword: string }>();
  const [newPasswordForm] = Form.useForm<{ newPassword: string; confirmPassword: string }>();

  const [avatarUploading, setAvatarUploading] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState('');
  const [uploadChoiceOpen, setUploadChoiceOpen] = useState(false);
  const [uploadUrlOpen, setUploadUrlOpen] = useState(false);
  const [passwordModalOpen, setPasswordModalOpen] = useState(false);
  const [passwordStep, setPasswordStep] = useState<'verify' | 'update'>('verify');
  const [verifyingPassword, setVerifyingPassword] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [currentPassword, setCurrentPassword] = useState('');
  const fileInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (user) {
      profileForm.setFieldsValue({
        username: user.username,
        gender: user.gender,
        age: user.age,
      });
    }
  }, [user, profileForm]);

  const genderOptions = [
    { label: 'Male', value: 1 },
    { label: 'Female', value: 2 },
  ];

  const handleAvatarUpload = async (file: File) => {
    setAvatarUploading(true);
    try {
      const newUrl = await uploadAvatar(file);
      await refreshProfile();
      message.success('Avatar updated');
      setAvatarUrl('');
      setUploadUrlOpen(false);
      return newUrl;
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to upload avatar';
      message.error(msg);
      throw err;
    } finally {
      setAvatarUploading(false);
    }
  };

  const handleAvatarUploadByUrl = async () => {
    if (!avatarUrl.trim()) {
      message.warning('Please enter an image URL');
      return;
    }
    setAvatarUploading(true);
    try {
      await uploadAvatarByUrl(avatarUrl.trim());
      await refreshProfile();
      message.success('Avatar updated');
      setAvatarUrl('');
      setUploadUrlOpen(false);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to upload avatar';
      message.error(msg);
    } finally {
      setAvatarUploading(false);
    }
  };

  const handleFileInputChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;
    try {
      await handleAvatarUpload(file);
    } finally {
      event.target.value = '';
    }
  };

  const handleProfileSubmit = async (values: ProfileFormValues) => {
    const payload: UpdateProfilePayload = {};
    if (values.username && values.username !== user?.username) {
      payload.username = values.username.trim();
    }
    if (typeof values.gender === 'number' && values.gender !== user?.gender) {
      payload.gender = values.gender;
    }
    if (typeof values.age === 'number' && values.age !== user?.age) {
      payload.age = values.age;
    }

    if (!payload.username && payload.gender === undefined && payload.age === undefined) {
      message.info('No changes detected');
      return;
    }

    try {
      await updateUserProfile(payload);
      await refreshProfile();
      message.success('Profile updated');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to update profile';
      message.error(msg);
    }
  };

  const openChangePasswordModal = () => {
    setPasswordModalOpen(true);
    setPasswordStep('verify');
    setCurrentPassword('');
    verifyPasswordForm.resetFields();
    newPasswordForm.resetFields();
  };

  const handleVerifyPassword = async () => {
    if (!user?.email) {
      message.error('Email not available for verification');
      return;
    }
    try {
      const { currentPassword: candidate } = await verifyPasswordForm.validateFields();
      setVerifyingPassword(true);
      await verifyCurrentPassword(user.email, candidate);
      setCurrentPassword(candidate);
      setPasswordStep('update');
      message.success('Current password verified');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to verify password';
      message.error(msg);
    } finally {
      setVerifyingPassword(false);
    }
  };

  const handleChangePassword = async () => {
    try {
      const { newPassword } = await newPasswordForm.validateFields();
      setChangingPassword(true);
      await changePassword({ oldPassword: currentPassword, newPassword });
      localStorage.removeItem('token');
      setUser(null);
      setStatus('unauthenticated');
      setPasswordModalOpen(false);
      message.success('Password changed successfully, please log in again');
      navigate('/login');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to change password';
      message.error(msg);
    } finally {
      setChangingPassword(false);
    }
  };

  const handleSendChangeEmail = () => {
    modal.confirm({
      title: 'Change email',
      content: 'Do you want to change your email? We will send a change email to your current address.',
      okText: 'Send email',
      cancelText: 'Cancel',
      centered: true,
      onOk: async () => {
        try {
          await sendChangeEmailLink();
          message.success('Email sent');
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : 'Failed to send email';
          message.error(msg);
        }
      },
    });
  };

  const handleDeleteAccount = () => {
    deleteAccountForm.resetFields();
    Modal.confirm({
      title: 'Delete account',
      content: (
        <Form form={deleteAccountForm} layout="vertical">
          <Alert
            type="warning"
            showIcon
            style={{ marginBottom: 12 }}
            message="This action is irreversible. All of your data will be permanently deleted."
          />
          <Form.Item
            name="password"
            label="Confirm your password"
            rules={[{ required: true, message: 'Please enter your password' }]}
          >
            <Input.Password placeholder="Password" autoFocus />
          </Form.Item>
        </Form>
      ),
      okText: 'Delete',
      cancelText: 'Cancel',
      okButtonProps: { danger: true },
      centered: true,
      maskClosable: false,
      onOk: async () => {
        try {
          const { password } = await deleteAccountForm.validateFields();
          await deleteUserAccount({ verifyPassword: password });
          localStorage.removeItem('token');
          setUser(null);
          setStatus('unauthenticated');
          message.success('Account deleted');
          navigate('/login');
        } catch (err: unknown) {
          const msg = err instanceof Error ? err.message : 'Failed to delete account';
          message.error(msg);
          throw err;
        }
      },
    });
  };

  const passwordModalTitle = passwordStep === 'verify' ? 'Verify current password' : 'Set new password';

  return (
    <div style={{ width: '100%', display: 'flex', justifyContent: 'center', padding: '24px 16px' }}>
      <Flex vertical gap={24} style={{ width: '100%', maxWidth: 720 }}>
        <Card>
          <Space align="start" size={24} wrap>
            <Avatar
              size={96}
              src={user?.avatar}
              icon={<UserOutlined />}
              style={{ border: '1px solid rgba(0,0,0,0.06)' }}
            />
            <Flex vertical gap={16}>
              <Typography.Title level={4} style={{ margin: 0 }}>
                Profile photo
              </Typography.Title>
              <Button
                icon={<UploadOutlined />}
                loading={avatarUploading}
                onClick={() => setUploadChoiceOpen(true)}
              >
                Upload image
              </Button>
              <Typography.Text type="secondary">
                Choose to upload an image file or provide an image URL.
              </Typography.Text>
            </Flex>
          </Space>
          <input
            type="file"
            accept="image/*"
            ref={fileInputRef}
            style={{ display: 'none' }}
            onChange={handleFileInputChange}
          />
        </Card>

        <Card title="Profile information">
          <Form
            form={profileForm}
            layout="vertical"
            onFinish={handleProfileSubmit}
            initialValues={{
              username: user?.username,
              gender: user?.gender,
              age: user?.age,
            }}
          >
            <Form.Item
              name="username"
              label="Username"
              rules={[{ required: true, message: 'Please enter your username' }]}
            >
              <Input placeholder="Enter username" />
            </Form.Item>

            <Form.Item name="gender" label="Gender">
              <Select
                placeholder="Select gender"
                options={genderOptions}
                allowClear
              />
            </Form.Item>

            <Form.Item
              name="age"
              label="Age"
              rules={[
                { type: 'number', min: 0, message: 'Age must be a positive number' },
              ]}
            >
              <InputNumber min={0} style={{ width: '100%' }} placeholder="Enter age" />
            </Form.Item>

            <Divider />

            <Button type="primary" htmlType="submit">
              Save changes
            </Button>
          </Form>
        </Card>

        <Modal
          open={uploadChoiceOpen}
          title="Choose upload method"
          footer={null}
          centered
          onCancel={() => setUploadChoiceOpen(false)}
        >
          <Flex vertical gap={12}>
            <Button
              type="primary"
              icon={<UploadOutlined />}
              block
              disabled={avatarUploading}
              onClick={() => {
                setUploadChoiceOpen(false);
                fileInputRef.current?.click();
              }}
            >
              Upload from file
            </Button>
            <Button
              icon={<LinkOutlined />}
              block
              disabled={avatarUploading}
              onClick={() => {
                setUploadChoiceOpen(false);
                setUploadUrlOpen(true);
              }}
            >
              Upload via URL
            </Button>
          </Flex>
        </Modal>

        <Modal
          open={uploadUrlOpen}
          title="Upload via URL"
          okText="Upload"
          centered
          okButtonProps={{ loading: avatarUploading }}
          onCancel={() => setUploadUrlOpen(false)}
          onOk={handleAvatarUploadByUrl}
        >
          <Flex vertical gap={8}>
            <Input
              value={avatarUrl}
              onChange={(event) => setAvatarUrl(event.target.value)}
              placeholder="https://example.com/avatar.png"
            />
            <Typography.Text type="secondary">
              We will fetch the image from the provided link.
            </Typography.Text>
          </Flex>
        </Modal>

        <Card title="Account">
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            <Space align="center" size="middle" style={{ justifyContent: 'space-between', width: '100%' }}>
              <div>
                <Typography.Text type="secondary">Email</Typography.Text>
                <Typography.Paragraph style={{ margin: 0 }}>
                  {user?.email ?? 'Not available'}
                </Typography.Paragraph>
              </div>
              <Button
                icon={<MailOutlined />}
                onClick={handleSendChangeEmail}
              >
                Change email
              </Button>
            </Space>

            <Divider />

            <Space align="center" size="middle" style={{ justifyContent: 'space-between', width: '100%' }}>
              <div>
                <Typography.Text type="secondary">Password</Typography.Text>
                <Typography.Paragraph style={{ margin: 0 }}>
                  Update your password to keep your account secure.
                </Typography.Paragraph>
              </div>
              <Button icon={<LockOutlined />} onClick={openChangePasswordModal}>
                Change password
              </Button>
            </Space>

            <Divider />

            <Space align="center" size="middle" style={{ justifyContent: 'space-between', width: '100%' }}>
              <div>
                <Typography.Text type="danger">Delete account</Typography.Text>
                <Typography.Paragraph type="secondary" style={{ margin: 0 }}>
                  Permanently remove your profile and all related data.
                </Typography.Paragraph>
              </div>
              <Button icon={<DeleteOutlined />} danger onClick={handleDeleteAccount}>
                Delete account
              </Button>
            </Space>
          </Space>
        </Card>

        <Modal
          open={passwordModalOpen}
          title={passwordModalTitle}
          onCancel={() => setPasswordModalOpen(false)}
          footer={null}
          destroyOnClose
        >
          {passwordStep === 'verify' && (
            <Form form={verifyPasswordForm} layout="vertical" onFinish={handleVerifyPassword}>
              <Form.Item
                name="currentPassword"
                label="Current password"
                rules={[{ required: true, message: 'Please enter your current password' }]}
              >
                <Input.Password autoFocus />
              </Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={verifyingPassword}
                block
                icon={<LockOutlined />}
              >
                Verify
              </Button>
            </Form>
          )}

          {passwordStep === 'update' && (
            <Form form={newPasswordForm} layout="vertical" onFinish={handleChangePassword}>
              <Form.Item
                name="newPassword"
                label="New password"
                rules={[{ required: true, min: 6, message: 'Password must be at least 6 characters' }]}
              >
                <Input.Password autoFocus />
              </Form.Item>
              <Form.Item
                name="confirmPassword"
                label="Confirm new password"
                dependencies={['newPassword']}
                rules={[
                  { required: true, message: 'Please confirm your new password' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (!value || getFieldValue('newPassword') === value) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('Passwords do not match'));
                    },
                  }),
                ]}
              >
                <Input.Password />
              </Form.Item>
              <Button
                type="primary"
                htmlType="submit"
                loading={changingPassword}
                block
              >
                Update password
              </Button>
            </Form>
          )}
        </Modal>
      </Flex>
    </div>
  );
}
