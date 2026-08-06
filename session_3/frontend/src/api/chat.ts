import type { ChatErrorResponse, ChatRequest, ChatResponse } from '../types/chat'

const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export class ChatApiError extends Error {
  readonly type: ChatErrorResponse['type']

  constructor(message: string, type: ChatErrorResponse['type'] = 'hard') {
    super(message)
    this.name = 'ChatApiError'
    this.type = type
  }
}

export async function sendChatMessage(request: ChatRequest): Promise<ChatResponse> {
  const response = await fetch(`${API_BASE}/api/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const errorBody = (await response.json().catch(() => ({
      error: 'Request failed',
      type: 'hard',
    }))) as Partial<ChatErrorResponse>
    throw new ChatApiError(errorBody.error ?? `HTTP ${response.status}`, errorBody.type ?? 'hard')
  }

  return response.json()
}
