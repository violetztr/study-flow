import {
  DeleteOutlined,
  FileTextOutlined,
  PlusOutlined,
  SaveOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  Checkbox,
  Empty,
  Form,
  Input,
  List,
  Popconfirm,
  Select,
  Space,
  Tag,
  message,
} from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import {
  type NoteBlockRequest,
  type NoteBlockType,
  createNote,
  deleteNote,
  getNote,
  listNotes,
  saveNoteBlocks,
  updateNote,
} from '../api/notes'

type NoteFormValues = {
  title: string
}

const blockTypeOptions: Array<{ label: string; value: NoteBlockType }> = [
  { label: '段落', value: 'paragraph' },
  { label: '标题', value: 'heading' },
  { label: '待办', value: 'todo' },
  { label: '引用', value: 'quote' },
  { label: '代码', value: 'code' },
]

function emptyBlock(sortOrder: number): NoteBlockRequest {
  return {
    type: 'paragraph',
    content: '',
    checked: false,
    sortOrder,
  }
}

function NotesPage() {
  const queryClient = useQueryClient()
  const [noteForm] = Form.useForm<NoteFormValues>()
  const [selectedNoteId, setSelectedNoteId] = useState<number | null>(null)
  const [blocks, setBlocks] = useState<NoteBlockRequest[]>([emptyBlock(0)])

  const notesQuery = useQuery({
    queryKey: ['notes'],
    queryFn: listNotes,
  })

  const selectedNoteQuery = useQuery({
    queryKey: ['notes', selectedNoteId],
    queryFn: () => getNote(selectedNoteId!),
    enabled: selectedNoteId != null,
  })

  useEffect(() => {
    const firstNote = notesQuery.data?.[0]
    if (!selectedNoteId && firstNote) {
      setSelectedNoteId(firstNote.id)
    }
  }, [notesQuery.data, selectedNoteId])

  useEffect(() => {
    const note = selectedNoteQuery.data
    if (!note) {
      return
    }

    noteForm.setFieldsValue({ title: note.title })
    setBlocks(
      note.blocks.length
        ? note.blocks.map((block) => ({
            type: block.type,
            content: block.content ?? '',
            checked: block.checked,
            sortOrder: block.sortOrder,
          }))
        : [emptyBlock(0)],
    )
  }, [noteForm, selectedNoteQuery.data])

  const createNoteMutation = useMutation({
    mutationFn: createNote,
    onSuccess: (note) => {
      queryClient.invalidateQueries({ queryKey: ['notes'] })
      setSelectedNoteId(note.id)
      message.success('笔记已创建')
    },
  })

  const updateNoteMutation = useMutation({
    mutationFn: ({ id, title }: { id: number; title: string }) =>
      updateNote(id, {
        title,
        parentId: selectedNoteQuery.data?.parentId ?? null,
        icon: selectedNoteQuery.data?.icon,
        favorite: selectedNoteQuery.data?.favorite,
        sortOrder: selectedNoteQuery.data?.sortOrder ?? 0,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes'] })
      message.success('标题已保存')
    },
  })

  const saveBlocksMutation = useMutation({
    mutationFn: ({ id, request }: { id: number; request: NoteBlockRequest[] }) =>
      saveNoteBlocks(id, request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes', selectedNoteId] })
      message.success('正文已保存')
    },
  })

  const deleteNoteMutation = useMutation({
    mutationFn: deleteNote,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notes'] })
      setSelectedNoteId(null)
      message.success('笔记已归档')
    },
  })

  function createQuickNote() {
    createNoteMutation.mutate({
      title: `新的学习笔记 ${new Date().toLocaleTimeString('zh-CN', { hour12: false })}`,
      parentId: null,
      icon: 'book',
      favorite: false,
      sortOrder: 0,
    })
  }

  function updateBlock(index: number, patch: Partial<NoteBlockRequest>) {
    setBlocks((current) =>
      current.map((block, blockIndex) =>
        blockIndex === index ? { ...block, ...patch } : block,
      ),
    )
  }

  function addBlock() {
    setBlocks((current) => [...current, emptyBlock(current.length)])
  }

  function removeBlock(index: number) {
    setBlocks((current) =>
      current.length === 1
        ? [emptyBlock(0)]
        : current
            .filter((_, blockIndex) => blockIndex !== index)
            .map((block, blockIndex) => ({ ...block, sortOrder: blockIndex })),
    )
  }

  async function saveCurrentNote(values: NoteFormValues) {
    if (!selectedNoteId) {
      return
    }

    await updateNoteMutation.mutateAsync({ id: selectedNoteId, title: values.title })
    await saveBlocksMutation.mutateAsync({
      id: selectedNoteId,
      request: blocks.map((block, index) => ({
        ...block,
        checked: Boolean(block.checked),
        sortOrder: index,
      })),
    })
  }

  return (
    <section className="page-section">
      <div className="page-heading">
        <div>
          <p className="dashboard-kicker">Notes module</p>
          <h1>笔记工作台</h1>
          <p>把学习过程沉淀成页面和块：标题、段落、待办、引用、代码块都能保存到后端。</p>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          loading={createNoteMutation.isPending}
          onClick={createQuickNote}
        >
          新建笔记
        </Button>
      </div>

      <div className="workspace-grid">
        <Card className="workspace-card note-list-card">
          <div className="workspace-card-title">
            <FileTextOutlined />
            <span>笔记列表</span>
          </div>
          <List
            loading={notesQuery.isLoading}
            dataSource={notesQuery.data ?? []}
            locale={{ emptyText: <Empty description="还没有笔记，先新建一篇。" /> }}
            renderItem={(note) => (
              <List.Item
                className={note.id === selectedNoteId ? 'active-list-item' : ''}
                onClick={() => setSelectedNoteId(note.id)}
                actions={[
                  <Popconfirm
                    key="delete"
                    title="确认归档这篇笔记？"
                    onConfirm={(event) => {
                      event?.stopPropagation()
                      deleteNoteMutation.mutate(note.id)
                    }}
                  >
                    <Button
                      danger
                      size="small"
                      icon={<DeleteOutlined />}
                      onClick={(event) => event.stopPropagation()}
                    />
                  </Popconfirm>,
                ]}
              >
                <List.Item.Meta
                  title={note.title}
                  description={note.favorite ? <Tag color="gold">收藏</Tag> : '学习笔记'}
                />
              </List.Item>
            )}
          />
        </Card>

        <Card className="workspace-card editor-card">
          {selectedNoteId ? (
            <Form<NoteFormValues>
              form={noteForm}
              layout="vertical"
              requiredMark={false}
              onFinish={saveCurrentNote}
            >
              <Form.Item
                label="笔记标题"
                name="title"
                rules={[{ required: true, message: '请输入笔记标题' }]}
              >
                <Input size="large" placeholder="例如 React Hooks 学习笔记" />
              </Form.Item>

              <div className="block-editor">
                {blocks.map((block, index) => (
                  <div className="note-block-row" key={`${index}-${block.sortOrder}`}>
                    <Select
                      value={block.type}
                      options={blockTypeOptions}
                      className="block-type-select"
                      onChange={(type) => updateBlock(index, { type })}
                    />
                    {block.type === 'todo' ? (
                      <Checkbox
                        checked={Boolean(block.checked)}
                        onChange={(event) =>
                          updateBlock(index, { checked: event.target.checked })
                        }
                      />
                    ) : null}
                    <Input.TextArea
                      autoSize={{ minRows: block.type === 'code' ? 3 : 1, maxRows: 8 }}
                      value={block.content}
                      placeholder={
                        block.type === 'code'
                          ? '粘贴代码片段'
                          : block.type === 'heading'
                            ? '写一个小标题'
                            : '记录你的理解'
                      }
                      onChange={(event) =>
                        updateBlock(index, { content: event.target.value })
                      }
                    />
                    <Button danger onClick={() => removeBlock(index)}>
                      删除
                    </Button>
                  </div>
                ))}
              </div>

              <Space wrap className="editor-actions">
                <Button icon={<PlusOutlined />} onClick={addBlock}>
                  添加块
                </Button>
                <Button
                  type="primary"
                  htmlType="submit"
                  icon={<SaveOutlined />}
                  loading={updateNoteMutation.isPending || saveBlocksMutation.isPending}
                >
                  保存笔记
                </Button>
              </Space>
            </Form>
          ) : (
            <Empty description="选择或新建一篇笔记后开始编辑。" />
          )}
        </Card>
      </div>
    </section>
  )
}

export default NotesPage
