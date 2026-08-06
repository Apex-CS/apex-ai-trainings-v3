import { useCallback, useEffect, useRef, useState } from 'react'
import { ChatApiError, sendChatMessage } from '../api/chat'
import type { ChatMessage } from '../types/chat'

const CONVERSATION_KEY = 'example-company-chat-conversation-id'

function createMessage(
  role: ChatMessage['role'],
  content: string,
  warnings?: string[],
): ChatMessage {
  return {
    id: crypto.randomUUID(),
    role,
    content,
    timestamp: new Date(),
    warnings,
  }
}

export function useChat() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    createMessage(
      'assistant',
      'Hello! I am the Example Company AI assistant. I can help with finance, IT, marketing, and sales questions using internal documents, web search, or the company database. What would you like to know?',
    ),
  ])
  const [conversationId, setConversationId] = useState<string | null>(() =>
    sessionStorage.getItem(CONVERSATION_KEY),
  )
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [warnings, setWarnings] = useState<string[]>([])
  const messagesEndRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, isLoading])

  const sendMessage = useCallback(
    async (text: string) => {
      const trimmed = text.trim()
      if (!trimmed || isLoading) return

      setError(null)
      setWarnings([])
      setMessages((prev) => [...prev, createMessage('user', trimmed)])
      setIsLoading(true)

      try {
        const response = await sendChatMessage({
          message: trimmed,
          conversationId: conversationId ?? undefined,
        })

        const responseWarnings = response.warnings ?? []
        setConversationId(response.conversationId)
        sessionStorage.setItem(CONVERSATION_KEY, response.conversationId)
        setWarnings(responseWarnings)
        setMessages((prev) => [
          ...prev,
          createMessage('assistant', response.answer, responseWarnings),
        ])
      } catch (err) {
        const message = err instanceof Error ? err.message : 'Something went wrong'
        const isHardError = !(err instanceof ChatApiError) || err.type === 'hard'
        if (isHardError) {
          setError(message)
        } else {
          setWarnings([message])
        }
        setMessages((prev) => [
          ...prev,
          createMessage(
            'assistant',
            isHardError
              ? `Sorry, I could not process that request. ${message}`
              : `I ran into a partial issue, but here is what I could determine. ${message}`,
            isHardError ? undefined : [message],
          ),
        ])
      } finally {
        setIsLoading(false)
      }
    },
    [conversationId, isLoading],
  )

  const startNewChat = useCallback(() => {
    sessionStorage.removeItem(CONVERSATION_KEY)
    setConversationId(null)
    setError(null)
    setWarnings([])
    setMessages([
      createMessage(
        'assistant',
        'Started a new conversation. Ask me about finance, IT, marketing, sales, or internal company documents.',
      ),
    ])
  }, [])

  return {
    messages,
    isLoading,
    error,
    warnings,
    sendMessage,
    startNewChat,
    messagesEndRef,
  }
}
