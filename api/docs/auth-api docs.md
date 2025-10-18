# Api 文档

**API Version:** v1.0  


## 1. 登录/注册

实现用户注册，登录，认证功能


---
### 登录

> BASIC

**Path:** /api/login

**Method:** POST

**Desc:** 验证账号与密码，登录成功后返回 JWT令牌

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| email | string | 用户登录邮箱。必填 |
| password | string | 用户登录密码 6-64位。必填 |

**Request Demo:**

```json
{
  "email": "1234@1234",
  "password": "12345678"
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | string | 返回的 JWT 访问令牌 |

**Response Demo:**

```json
{
  "code": 1,
  "msg": "success",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwidXNlcm5hbWUiOiIxMjMiLCJlbWFpbCI6IjEyMzRAMTIzNCIsImlhdCI6MTc2MDY0NDAwNCwiZXhwIjoxNzYwNjQ3NjA0fQ.qxQaCRhJgj-R94k6ndGrSMs13ktl4WVECdPMyJtMSHg"
}
```




---
### 注册

> BASIC

**Path:** /api/register

**Method:** POST

**Desc:** 创建新用户

> REQUEST

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Content-Type | application/json | YES |  |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| username | string | 注册用户名，3-30位，只能包含字母、数字、下划线和短横线。必填 |
| email | string | 注册邮箱。必填 |
| password | string | 注册密码，6-64位，必填 |

**Request Demo:**

```json
{
  "username": "1234",
  "email": "1234@1234",
  "password": "123456"
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | object | 返回的数据 |

**Response Demo:**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

## 2. 用户详情页

实现用户详情页的查看与更新功能


---
### 获取用户信息详情

> BASIC

**Path:** /api/users/profile

**Method:** GET

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

例如：`Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIzIiwidXNlcm5hbWUiOiIxMjM0IiwiZW1haWwiOiIxMjM0QDEyMzQiLCJpYXQiOjE3NjA2NDA5MzAsImV4cCI6MTc2MDY0NDUzMH0.RLbFwtT54Pp0OpgXV4s-J2gkDTnGhPzb7MBDl3PAfV4`

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |

**Query:（无）**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| — | — | — | 本接口不需要 query 参数；userId 由后端通过 JWT 获取 |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | object | 返回的数据 |
| &ensp;&ensp;&#124;─email | string | 只读：请求体中的 email 会被忽略，响应中仍然返回 |
| &ensp;&ensp;&#124;─avatar | string | 用户头像。 |
| &ensp;&ensp;&#124;─username | string | 用户名 3-30位，只能包含字母、数字、下划线和短横线。非必须 |
| &ensp;&ensp;&#124;─age | integer | 年龄 0-150。非必须 |
| &ensp;&ensp;&#124;─gender | integer | 性别，非必须。 1: Male, 2: Female |

**Response Demo:**

```json
{
    "code": 1,
    "msg": "success",
    "data": {
        "username": "123",
        "age": 21,
        "gender": 1,
        "email": "1234@1234",
        "avatar": "https://awscdn.dingzh.cc/elec5620-stage2/avatars/default_avatar.png"
    }
}
```




---
### 更新用户详情页信息（头像更新在 /upload/avatar接口）

> BASIC

**Path:** /api/users/profile

**Method:** PUT

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |
| Content-Type | application/json | YES |  |

**Query:（无）**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| — | — | — | 本接口不需要 query 参数；userId 由后端通过 JWT 获取 |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| username | string | 用户名 3-30位，只能包含字母、数字、下划线和短横线。非必须 |
| age | integer | 年龄 0-150。非必须 |
| gender | integer | 性别，非必须。 1: Male, 2: Female (前端映射) |

**Request Demo:**

```json
{
    "username": 123,
    "age": 21,
    "gender": 1
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | object | 返回的数据 |

**Response Demo:**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```




---
### 更新用户密码

> BASIC

**Path:** /api/users/profile/pd

**Method:** PUT

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

更改密码后，jwt token失效，需要重新登录认证

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |
| Content-Type | application/json | YES |  |

**Query:（无）**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| — | — | — | 本接口不需要 query 参数；userId 由后端通过 JWT 获取 |

**Request Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| oldPassword | string | 旧密码 6-64位 |
| newPassword | string | 新密码 6-64位 |

**Request Demo:**

```json
{
    "newPassword": 12345678,
    "oldPassword": 1234567
}
```



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | object | 返回的数据 |

**Response Demo:**

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```





## 3. 上传图片到AWS S3

实现本地图片上传、外链图片上传。支持格式 jpg,jpeg,png,gif,svg,webp


---
### 图片上传。 本地上传。

> BASIC

**Path:** /api/upload

**Method:** POST

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |
| Content-Type | multipart/form-data | YES |  |

**Form:**

| name | value | required | type | desc |
| ------------ | ------------ | ------------ | ------------ | ------------ |
| file |  | YES | file | 本地图片文件，支持 jpg/png/gif/webp/svg，最大10MB |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | string | 返回的 AWS S3 文件 URL |

**Response Demo:**

```json
{
    "code": 1,
    "msg": "success",
    "data": "https://awscdn.dingzh.cc/elec5620-stage2/2025/10/71e1452685044b95aa3f1c24dc789228.png"
}
```




---
### 用户头像上传并更新。 本地上传

> BASIC

**Path:** /api/upload/avatar

**Method:** POST

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |
| Content-Type | multipart/form-data | YES |  |

**Query:（无）**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| — | — | — | 本接口不需要 query 参数；userId 由后端通过 JWT 获取 |

**Form:**

| name | value | required | type | desc |
| ------------ | ------------ | ------------ | ------------ | ------------ |
| file |  | YES | file | 头像文件 |



> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | string | 返回的新头像 AWS S3 URL |

**Response Demo:**

```json
{
    "code": 1,
    "msg": "success",
    "data": "https://awscdn.dingzh.cc/elec5620-stage2/avatars/default_avatar.png"
}
```




---
### 单文件上传 外链上传

> BASIC

**Path:** /api/upload/link

**Method:** POST

**Desc:** 需要携带 Authorization: `Bearer <jwt>`

> REQUEST

**Headers:**

| name | value/example | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| Authorization | `Bearer <jwt>` | YES | 登录后返回的访问令牌 |

**Query:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| url |  | YES | 外部图片链接 |


**请求样例：**
POST: http://localhost:8082/api/upload/link?url=https://xxxx.png

> RESPONSE

**Headers:**

| name | value | required | desc |
| ------------ | ------------ | ------------ | ------------ |
| content-type | application/json;charset=UTF-8 | NO |  |

**Body:**

| name | type | desc |
| ------------ | ------------ | ------------ |
| code | integer | 响应码，1 代表成功，0 代表失败 |
| msg | string | 提示信息。成功：success；失败：异常信息 |
| data | string | 返回的 AWS S3 文件 URL |

**Response Demo:**

```json
{
    "code": 1,
    "msg": "success",
    "data": "https://awscdn.dingzh.cc/elec5620-stage2/2025/10/71e1452685044b95aa3f1c24dc789228.png"
}
```