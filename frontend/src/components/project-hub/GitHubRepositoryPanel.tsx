import { GithubOutlined, SyncOutlined } from '@ant-design/icons'
import { Button, Card, Descriptions, Form, Input, Space, Tag, message } from 'antd'
import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import {
  type GitHubRepositoryRequest,
  type GitHubRepositoryResponse,
  saveGitHubRepository,
  syncGitHubRepository,
} from '../../api/github'

type GitHubRepositoryPanelProps = {
  projectId: number | null
}

function GitHubRepositoryPanel({ projectId }: GitHubRepositoryPanelProps) {
  const [form] = Form.useForm<GitHubRepositoryRequest>()
  const [messageApi, contextHolder] = message.useMessage()
  const [repository, setRepository] = useState<GitHubRepositoryResponse | null>(null)

  const saveMutation = useMutation({
    mutationFn: (request: GitHubRepositoryRequest) => {
      if (!projectId) {
        throw new Error('Please select a project first')
      }
      return saveGitHubRepository(projectId, request)
    },
    onSuccess: (data) => {
      setRepository(data)
      messageApi.success('GitHub 仓库已保存')
    },
  })

  const syncMutation = useMutation({
    mutationFn: () => {
      if (!projectId) {
        throw new Error('Please select a project first')
      }
      return syncGitHubRepository(projectId)
    },
    onSuccess: (data) => {
      setRepository(data)
      messageApi.success('GitHub 元数据已同步')
    },
  })

  return (
    <Card className="workspace-card" title="GitHub 仓库">
      {contextHolder}
      <Form<GitHubRepositoryRequest>
        form={form}
        layout="vertical"
        requiredMark={false}
        onFinish={(values) => saveMutation.mutate(values)}
      >
        <div className="form-grid">
          <Form.Item
            label="Owner"
            name="owner"
            rules={[{ required: true, message: '请输入 GitHub owner' }]}
          >
            <Input placeholder="violetztr" />
          </Form.Item>
          <Form.Item
            label="Repo"
            name="repo"
            rules={[{ required: true, message: '请输入仓库名' }]}
          >
            <Input placeholder="study-flow" />
          </Form.Item>
        </div>
        <Space wrap>
          <Button
            type="primary"
            htmlType="submit"
            icon={<GithubOutlined />}
            disabled={!projectId}
            loading={saveMutation.isPending}
          >
            保存仓库
          </Button>
          <Button
            icon={<SyncOutlined />}
            disabled={!projectId}
            loading={syncMutation.isPending}
            onClick={() => syncMutation.mutate()}
          >
            同步 GitHub
          </Button>
        </Space>
      </Form>

      {repository ? (
        <Descriptions className="github-summary" size="small" column={2}>
          <Descriptions.Item label="仓库">
            {repository.htmlUrl ? (
              <a href={repository.htmlUrl} target="_blank" rel="noreferrer">
                {repository.owner}/{repository.repo}
              </a>
            ) : (
              `${repository.owner}/${repository.repo}`
            )}
          </Descriptions.Item>
          <Descriptions.Item label="语言">
            {repository.primaryLanguage ?? '未同步'}
          </Descriptions.Item>
          <Descriptions.Item label="Stars">{repository.stars ?? 0}</Descriptions.Item>
          <Descriptions.Item label="Forks">{repository.forks ?? 0}</Descriptions.Item>
          <Descriptions.Item label="README">
            <Tag color={repository.readmePresent ? 'green' : 'default'}>
              {repository.readmePresent ? '已检测到' : '未检测'}
            </Tag>
          </Descriptions.Item>
          <Descriptions.Item label="最近同步">
            {repository.lastSyncedAt ?? '未同步'}
          </Descriptions.Item>
        </Descriptions>
      ) : null}
    </Card>
  )
}

export default GitHubRepositoryPanel
