export interface CodeAttachment {
  filename: string
  contentType: string
  encoding: 'base64'
  data: string
}

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  warnings?: string[]
  attachmentFilename?: string
}

export type DemoUserId =
  | 'FULANO_SMITH'
  | 'SUTANO_DOE'
  | 'MENGANA_DAVIDSON'
  | 'BART_PEREZ'

export interface ChatRequest {
  message: string
  conversationId?: string
  codeToReview?: CodeAttachment
  demoUser?: DemoUserId
}

export interface ChatResponse {
  answer: string
  conversationId: string
  warnings?: string[]
}

export interface ChatErrorResponse {
  error: string
  type: 'hard' | 'soft'
}
