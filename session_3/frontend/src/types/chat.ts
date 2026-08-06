export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: Date
  warnings?: string[]
}

export interface ChatRequest {
  message: string
  conversationId?: string
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
